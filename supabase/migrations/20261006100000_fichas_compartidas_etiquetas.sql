-- Etiquetas de fichas compartidas y «quitarme el acceso».
-- Aplicar DESPUES de 20261005220000_quitar_acceso_heredado.sql.

-- 1) Para cada ficha que veo: si me la compartieron (autor, permiso) o si yo se la compartí a alguien (cuantas personas).
create or replace function public.info_fichas_compartidas()
returns table(
  ficha_id uuid,
  tipo text,
  autor_id uuid,
  autor_nombre text,
  permiso text,
  personas integer
)
language sql
stable
security definer
set search_path = ''
as $function$
  select
    f.id,
    'RECIBIDA'::text,
    f.creado_por,
    coalesce(nullif(trim(coalesce(pr.nombres, '')), ''), nullif(trim(coalesce(pr.correo, '')), ''), 'Otra persona'),
    case when private.puede_editar_ficha_id(f.id) then 'EDITOR' else 'LECTOR' end,
    0
  from public.fichas_familiares f
  left join public.perfiles pr on pr.id = f.creado_por
  where f.deleted_at is null
    and f.creado_por is not null
    and f.creado_por <> (select auth.uid())
    and private.puede_ver_ficha_id(f.id)
  union all
  select
    f.id,
    'OTORGADA'::text,
    f.creado_por,
    '',
    '',
    (
      select count(distinct u.usuario_id)::integer
      from (
        select a.usuario_id
        from public.accesos_ficha a
        where a.ficha_id = f.id and a.activo and a.creado_por = (select auth.uid())
          and a.usuario_id <> (select auth.uid())
        union all
        select a.usuario_id
        from public.accesos_autor a
        where a.autor_id = (select auth.uid()) and a.activo
          and a.usuario_id <> (select auth.uid())
          and a.organizacion_id = f.organizacion_id
          and (
            a.alcance = 'SALA'
            or (a.alcance = 'EAIS' and a.eais_id = f.eais_id)
            or (a.alcance = 'TERRITORIO' and a.territorio_id = f.territorio_id)
          )
      ) u
    )
  from public.fichas_familiares f
  where f.deleted_at is null
    and f.creado_por = (select auth.uid());
$function$;

revoke all on function public.info_fichas_compartidas() from public, anon;
grant execute on function public.info_fichas_compartidas() to authenticated;

-- 2) Quienes me compartieron fichas (lo que yo recibí).
create or replace function public.listar_accesos_recibidos()
returns table(
  organizacion_id uuid,
  autor_id uuid,
  nombres text,
  cargo text,
  correo text,
  permiso text,
  n_sala integer,
  n_eais integer,
  n_barrios integer,
  n_fichas integer
)
language sql
stable
security definer
set search_path = ''
as $function$
  with partes as (
    select a.organizacion_id, a.autor_id, a.permiso, a.alcance
    from public.accesos_autor a
    where a.usuario_id = (select auth.uid()) and a.activo
    union all
    select f.organizacion_id, f.creado_por, a.permiso, 'FICHA'::text
    from public.accesos_ficha a
    join public.fichas_familiares f on f.id = a.ficha_id
    where a.usuario_id = (select auth.uid()) and a.activo and f.creado_por is not null
    union all
    -- accesos dados con la version anterior de la app
    select s.organizacion_id, s.creado_por, s.permiso, s.alcance
    from public.accesos_sala s
    where s.usuario_id = (select auth.uid())
      and s.activo
      and s.creado_por is not null
      and s.creado_por <> (select auth.uid())
      and s.permiso <> 'ADMINISTRADOR'
  )
  select
    p.organizacion_id,
    p.autor_id,
    pr.nombres,
    pr.cargo,
    pr.correo,
    case when bool_or(p.permiso = 'EDITOR') then 'EDITOR' else 'LECTOR' end,
    (count(*) filter (where p.alcance = 'SALA'))::integer,
    (count(*) filter (where p.alcance = 'EAIS'))::integer,
    (count(*) filter (where p.alcance = 'TERRITORIO'))::integer,
    (count(*) filter (where p.alcance = 'FICHA'))::integer
  from partes p
  left join public.perfiles pr on pr.id = p.autor_id
  where p.autor_id <> (select auth.uid())
  group by p.organizacion_id, p.autor_id, pr.nombres, pr.cargo, pr.correo
  order by pr.nombres nulls last;
$function$;

revoke all on function public.listar_accesos_recibidos() from public, anon;
grant execute on function public.listar_accesos_recibidos() to authenticated;

-- 3) Quitarme yo mismo el acceso a lo que una persona me compartió (no necesita que ella lo autorice).
create or replace function public.quitar_mi_acceso(
  p_organizacion_id uuid,
  p_autor_id uuid
)
returns integer
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_total integer := 0;
  v_filas integer;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_autor_id = v_yo then
    raise exception 'Eso lo creaste tu.' using errcode = '22023';
  end if;

  update public.accesos_autor
  set activo = false, updated_at = now()
  where organizacion_id = p_organizacion_id
    and usuario_id = v_yo
    and autor_id = p_autor_id
    and activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  update public.accesos_ficha a
  set activo = false, updated_at = now()
  where a.organizacion_id = p_organizacion_id
    and a.usuario_id = v_yo
    and a.creado_por = p_autor_id
    and a.activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  -- accesos dados con la version anterior de la app
  update public.accesos_sala
  set activo = false, updated_at = now()
  where organizacion_id = p_organizacion_id
    and usuario_id = v_yo
    and creado_por = p_autor_id
    and permiso <> 'ADMINISTRADOR'
    and activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  -- Si ya no me queda ningun acceso en esa Sala y entre por una invitacion de esa persona, dejo de ser miembro.
  if not exists (select 1 from public.accesos_sala where organizacion_id = p_organizacion_id and usuario_id = v_yo and activo)
     and not exists (select 1 from public.accesos_autor where organizacion_id = p_organizacion_id and usuario_id = v_yo and activo)
     and not exists (select 1 from public.accesos_ficha where organizacion_id = p_organizacion_id and usuario_id = v_yo and activo) then
    update public.miembros_organizacion
    set activo = false, updated_at = now()
    where organizacion_id = p_organizacion_id
      and usuario_id = v_yo
      and rol <> 'ADMIN'
      and creado_por = p_autor_id;
  end if;

  return v_total;
end;
$function$;

revoke all on function public.quitar_mi_acceso(uuid, uuid) from public, anon;
grant execute on function public.quitar_mi_acceso(uuid, uuid) to authenticated;
