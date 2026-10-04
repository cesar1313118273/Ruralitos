-- Avisos de fichas editadas por otra persona y traspaso de una ficha a otra persona.
-- Aplicar DESPUES de 20261006200000_visitas_y_acceso_por_ficha.sql.

-- 1) Quien hizo el ultimo cambio: hasta ahora solo se llenaba al crear la ficha.
create or replace function private.marcar_actualizado_por()
returns trigger
language plpgsql
set search_path = ''
as $function$
begin
  if new.creado_por is distinct from old.creado_por then
    -- un traspaso no cuenta como edicion de otra persona
    new.actualizado_por := new.creado_por;
  elsif (select auth.uid()) is not null then
    new.actualizado_por := (select auth.uid());
  end if;
  return new;
end;
$function$;

drop trigger if exists fichas_marcar_actualizado_por on public.fichas_familiares;
create trigger fichas_marcar_actualizado_por
  before update on public.fichas_familiares
  for each row execute function private.marcar_actualizado_por();

-- 2) info_fichas_compartidas ahora tambien dice, en mis fichas, si otra persona las edito y cuando.
drop function if exists public.info_fichas_compartidas();
create function public.info_fichas_compartidas()
returns table(
  ficha_id uuid,
  tipo text,
  autor_id uuid,
  autor_nombre text,
  permiso text,
  personas integer,
  editor_nombre text,
  editada_en bigint
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
    0,
    ''::text,
    0::bigint
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
    ),
    case when f.actualizado_por is not null and f.actualizado_por <> (select auth.uid())
      then coalesce(nullif(trim(coalesce(ed.nombres, '')), ''), nullif(trim(coalesce(ed.correo, '')), ''), 'Otra persona')
      else '' end,
    case when f.actualizado_por is not null and f.actualizado_por <> (select auth.uid())
      then (extract(epoch from f.updated_at) * 1000)::bigint
      else 0::bigint end
  from public.fichas_familiares f
  left join public.perfiles ed on ed.id = f.actualizado_por
  where f.deleted_at is null
    and f.creado_por = (select auth.uid());
$function$;

revoke all on function public.info_fichas_compartidas() from public, anon;
grant execute on function public.info_fichas_compartidas() to authenticated;

-- 3) Traspasar una ficha: la nueva persona pasa a ser su autora; quien la creo conserva acceso de edicion.
-- Las demas personas que la veian por mi acceso dejan de verla: la nueva autora decide a quien se la comparte.
create or replace function public.traspasar_ficha(p_ficha_id uuid, p_usuario_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_org uuid;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_usuario_id = v_yo then
    raise exception 'La ficha ya es tuya.' using errcode = '22023';
  end if;
  select organizacion_id into v_org
  from public.fichas_familiares
  where id = p_ficha_id and creado_por = v_yo and deleted_at is null;
  if v_org is null then
    raise exception 'Solo quien creo la ficha puede traspasarla.' using errcode = '42501';
  end if;
  if not exists (select 1 from private.accesos_de_ficha(p_ficha_id, v_yo) x where x.usuario_id = p_usuario_id) then
    raise exception 'Primero comparte la ficha con esa persona.' using errcode = '42501';
  end if;

  -- los accesos que di ficha por ficha ya no valen; la nueva autora decide
  update public.accesos_ficha
  set activo = false, updated_at = now()
  where ficha_id = p_ficha_id and activo and creado_por = v_yo;

  update public.fichas_familiares
  set creado_por = p_usuario_id
  where id = p_ficha_id;

  -- quien la creo sigue pudiendo editarla
  insert into public.accesos_ficha(organizacion_id, ficha_id, usuario_id, permiso, creado_por, activo)
  values (v_org, p_ficha_id, v_yo, 'EDITOR', p_usuario_id, true)
  on conflict (usuario_id, ficha_id)
  do update set activo = true, permiso = 'EDITOR', creado_por = p_usuario_id, updated_at = now();
end;
$function$;

revoke all on function public.traspasar_ficha(uuid, uuid) from public, anon;
grant execute on function public.traspasar_ficha(uuid, uuid) to authenticated;
