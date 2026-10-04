-- Detalle de quien ve cada ficha mia, quitar el acceso ficha por ficha y asignar una visita a un companero.
-- Aplicar DESPUES de 20261006100000_fichas_compartidas_etiquetas.sql. Solo agrega funciones.

-- Quienes pueden ver una ficha de :p_autor y por cual via (ficha suelta, barrio, EAIS o centro completo).
create or replace function private.accesos_de_ficha(p_ficha_id uuid, p_autor uuid)
returns table(usuario_id uuid, permiso text, via text)
language sql
stable
security definer
set search_path = ''
as $function$
  select a.usuario_id, a.permiso, 'FICHA'::text
  from public.accesos_ficha a
  join public.fichas_familiares f on f.id = a.ficha_id
  where a.ficha_id = p_ficha_id and a.activo and a.creado_por = p_autor
    and f.creado_por = p_autor and a.usuario_id <> p_autor
  union all
  select a.usuario_id, a.permiso,
    case a.alcance when 'TERRITORIO' then 'BARRIO' when 'EAIS' then 'EAIS' else 'CENTRO' end
  from public.accesos_autor a
  join public.fichas_familiares f on f.id = p_ficha_id
  where a.autor_id = p_autor and a.activo and f.creado_por = p_autor
    and a.usuario_id <> p_autor
    and a.organizacion_id = f.organizacion_id
    and (
      a.alcance = 'SALA'
      or (a.alcance = 'EAIS' and a.eais_id = f.eais_id)
      or (a.alcance = 'TERRITORIO' and a.territorio_id = f.territorio_id)
    );
$function$;

revoke all on function private.accesos_de_ficha(uuid, uuid) from public, anon, authenticated;

-- 1) Personas con acceso a una ficha mia.
create or replace function public.personas_con_acceso_ficha(p_ficha_id uuid)
returns table(
  usuario_id uuid,
  nombres text,
  cargo text,
  correo text,
  permiso text,
  via text
)
language sql
stable
security definer
set search_path = ''
as $function$
  select
    x.usuario_id,
    pr.nombres,
    pr.cargo,
    pr.correo,
    case when bool_or(x.permiso = 'EDITOR') then 'EDITOR' else 'LECTOR' end,
    -- la via mas especifica: si hay acceso directo a la ficha se puede quitar solo ella
    case when bool_or(x.via = 'FICHA') then 'FICHA'
         when bool_or(x.via = 'BARRIO') then 'BARRIO'
         when bool_or(x.via = 'EAIS') then 'EAIS'
         else 'CENTRO' end
  from private.accesos_de_ficha(p_ficha_id, (select auth.uid())) x
  left join public.perfiles pr on pr.id = x.usuario_id
  group by x.usuario_id, pr.nombres, pr.cargo, pr.correo
  order by pr.nombres nulls last;
$function$;

revoke all on function public.personas_con_acceso_ficha(uuid) from public, anon;
grant execute on function public.personas_con_acceso_ficha(uuid) to authenticated;

-- 2) Quitar a una persona el acceso a UNA ficha mia (solo el acceso dado ficha por ficha).
create or replace function public.quitar_acceso_ficha(p_ficha_id uuid, p_usuario_id uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_org uuid;
  v_filas integer;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  select organizacion_id into v_org
  from public.fichas_familiares
  where id = p_ficha_id and creado_por = v_yo;
  if v_org is null then
    raise exception 'Solo quien creo la ficha puede quitar su acceso.' using errcode = '42501';
  end if;

  update public.accesos_ficha
  set activo = false, updated_at = now()
  where ficha_id = p_ficha_id and usuario_id = p_usuario_id and creado_por = v_yo and activo;
  get diagnostics v_filas = row_count;

  if not exists (select 1 from public.accesos_sala where organizacion_id = v_org and usuario_id = p_usuario_id and activo)
     and not exists (select 1 from public.accesos_autor where organizacion_id = v_org and usuario_id = p_usuario_id and activo)
     and not exists (select 1 from public.accesos_ficha where organizacion_id = v_org and usuario_id = p_usuario_id and activo) then
    update public.miembros_organizacion
    set activo = false, updated_at = now()
    where organizacion_id = v_org and usuario_id = p_usuario_id and rol <> 'ADMIN' and creado_por = v_yo;
  end if;

  return v_filas;
end;
$function$;

revoke all on function public.quitar_acceso_ficha(uuid, uuid) from public, anon;
grant execute on function public.quitar_acceso_ficha(uuid, uuid) to authenticated;

-- 3) Fichas sueltas que le di a una persona (para quitarlas una por una desde «Compartido con»).
create or replace function public.fichas_compartidas_con(p_usuario_id uuid)
returns table(
  ficha_id uuid,
  organizacion_id uuid,
  numero text,
  jefe text,
  permiso text
)
language sql
stable
security definer
set search_path = ''
as $function$
  select f.id, f.organizacion_id, f.numero_ficha_familiar, f.nombre_apellido_jefe_familia, a.permiso
  from public.accesos_ficha a
  join public.fichas_familiares f on f.id = a.ficha_id
  where a.usuario_id = p_usuario_id
    and a.creado_por = (select auth.uid())
    and a.activo
    and f.creado_por = (select auth.uid())
    and f.deleted_at is null
  order by f.nombre_apellido_jefe_familia;
$function$;

revoke all on function public.fichas_compartidas_con(uuid) from public, anon;
grant execute on function public.fichas_compartidas_con(uuid) to authenticated;

-- 4) Asignar una visita a una persona con la que ya compartí la ficha: le aparece en su agenda al sincronizar.
create or replace function public.asignar_visita(
  p_ficha_id uuid,
  p_usuario_id uuid,
  p_fecha_hora bigint,
  p_tipo text default 'Visita domiciliaria',
  p_nota text default ''
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_ficha public.fichas_familiares%rowtype;
  v_mi_nombre text;
  v_id uuid := gen_random_uuid();
  v_ahora bigint := (extract(epoch from now()) * 1000)::bigint;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_usuario_id = v_yo then
    raise exception 'Esa visita es tuya: agendala desde tu Agenda.' using errcode = '22023';
  end if;
  select * into v_ficha from public.fichas_familiares where id = p_ficha_id and creado_por = v_yo and deleted_at is null;
  if v_ficha.id is null then
    raise exception 'Solo quien creo la ficha puede asignar visitas.' using errcode = '42501';
  end if;
  if not exists (select 1 from private.accesos_de_ficha(p_ficha_id, v_yo) x where x.usuario_id = p_usuario_id) then
    raise exception 'Primero comparte la ficha con esa persona.' using errcode = '42501';
  end if;
  if p_fecha_hora < v_ahora - 86400000 then
    raise exception 'La fecha de la visita ya pasó.' using errcode = '22023';
  end if;

  select coalesce(nullif(trim(coalesce(nombres, '')), ''), 'una persona de tu equipo')
  into v_mi_nombre from public.perfiles where id = v_yo;

  insert into public.agenda_privada(id, owner_id, organizacion_id, version, payload, actualizado_en)
  values (
    v_id, p_usuario_id, v_ficha.organizacion_id, 1,
    jsonb_build_object(
      'ficha_id', v_ficha.id::text,
      'miembro_id', null,
      'persona', v_ficha.nombre_apellido_jefe_familia,
      'cedula', v_ficha.cedula_jefe_hogar,
      'barrio', v_ficha.barrio,
      'fecha_hora', p_fecha_hora,
      'tipo', coalesce(nullif(trim(p_tipo), ''), 'Visita domiciliaria'),
      'nota', trim('Asignada por ' || coalesce(v_mi_nombre, 'una persona de tu equipo') || '. ' || coalesce(p_nota, '')),
      'estado', 'PENDIENTE',
      'recordar', true,
      'creado_en', v_ahora,
      'origen', 'MANUAL',
      'grupo_riesgo', '',
      'fecha_base', null,
      'fecha_editada', true
    ),
    v_ahora
  );
  return v_id;
end;
$function$;

revoke all on function public.asignar_visita(uuid, uuid, bigint, text, text) from public, anon;
grant execute on function public.asignar_visita(uuid, uuid, bigint, text, text) to authenticated;
