-- «Compartido con» y «Quitar» tambien deben ver y retirar los accesos que se dieron con la version anterior de la app
-- (codigos de Sala, EAIS o barrio que viven en accesos_sala). Antes solo veian los accesos nuevos, y una persona a la que
-- se le habia compartido con un codigo antiguo seguia con su centro en la lista aunque se le "quitara" el acceso.
-- Aplicar DESPUES de 20261005200000_compartir_lo_propio.sql.

create or replace function public.listar_accesos_otorgados()
returns table(
  organizacion_id uuid,
  usuario_id uuid,
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
    select a.organizacion_id, a.usuario_id, a.permiso, a.alcance
    from public.accesos_autor a
    where a.autor_id = (select auth.uid()) and a.activo
    union all
    select f.organizacion_id, f.usuario_id, f.permiso, 'FICHA'::text
    from public.accesos_ficha f
    where f.creado_por = (select auth.uid()) and f.activo
    union all
    -- accesos dados con la version anterior de la app
    select s.organizacion_id, s.usuario_id, s.permiso, s.alcance
    from public.accesos_sala s
    where s.creado_por = (select auth.uid())
      and s.activo
      and s.usuario_id <> (select auth.uid())
      and s.permiso <> 'ADMINISTRADOR'
  )
  select
    p.organizacion_id,
    p.usuario_id,
    pr.nombres,
    pr.cargo,
    pr.correo,
    case when bool_or(p.permiso = 'EDITOR') then 'EDITOR' else 'LECTOR' end,
    (count(*) filter (where p.alcance = 'SALA'))::integer,
    (count(*) filter (where p.alcance = 'EAIS'))::integer,
    (count(*) filter (where p.alcance = 'TERRITORIO'))::integer,
    (count(*) filter (where p.alcance = 'FICHA'))::integer
  from partes p
  left join public.perfiles pr on pr.id = p.usuario_id
  group by p.organizacion_id, p.usuario_id, pr.nombres, pr.cargo, pr.correo
  order by pr.nombres nulls last;
$function$;

create or replace function public.quitar_acceso_compartido(
  p_organizacion_id uuid,
  p_usuario_id uuid
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
  if p_usuario_id = v_yo then
    raise exception 'No puedes quitarte el acceso a ti mismo.' using errcode = '22023';
  end if;

  update public.accesos_autor
  set activo = false, updated_at = now()
  where organizacion_id = p_organizacion_id
    and usuario_id = p_usuario_id
    and autor_id = v_yo
    and activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  update public.accesos_ficha
  set activo = false, updated_at = now()
  where organizacion_id = p_organizacion_id
    and usuario_id = p_usuario_id
    and creado_por = v_yo
    and activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  -- accesos dados con la version anterior de la app
  update public.accesos_sala
  set activo = false, updated_at = now()
  where organizacion_id = p_organizacion_id
    and usuario_id = p_usuario_id
    and creado_por = v_yo
    and permiso <> 'ADMINISTRADOR'
    and activo;
  get diagnostics v_filas = row_count;
  v_total := v_total + v_filas;

  -- Si ya no le queda ningun acceso en esa Sala y entro por una invitacion mia, tambien deja de ser miembro.
  if not exists (select 1 from public.accesos_sala where organizacion_id = p_organizacion_id and usuario_id = p_usuario_id and activo)
     and not exists (select 1 from public.accesos_autor where organizacion_id = p_organizacion_id and usuario_id = p_usuario_id and activo)
     and not exists (select 1 from public.accesos_ficha where organizacion_id = p_organizacion_id and usuario_id = p_usuario_id and activo) then
    update public.miembros_organizacion
    set activo = false, updated_at = now()
    where organizacion_id = p_organizacion_id
      and usuario_id = p_usuario_id
      and rol <> 'ADMIN'
      and creado_por = v_yo;
  end if;

  return v_total;
end;
$function$;
