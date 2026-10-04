-- info_fichas_compartidas: ahora dice a que Sala pertenece cada ficha (para bajar solo las faltantes de cada Sala)
-- y ya no devuelve una fila por cada ficha propia, solo las compartidas o editadas por otra persona
-- (PostgREST corta las respuestas en 1000 filas y una cuenta con muchas fichas quedaba incompleta).
-- Aplicar DESPUES de 20261007200000_solo_el_autor_elimina_su_ficha.sql.
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
  editada_en bigint,
  organizacion_id uuid
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
    0::bigint,
    f.organizacion_id
  from public.fichas_familiares f
  left join public.perfiles pr on pr.id = f.creado_por
  where f.deleted_at is null
    and f.creado_por is not null
    and f.creado_por <> (select auth.uid())
    and private.puede_ver_ficha_id(f.id)
  union all
  select * from (
    select
      f.id,
      'OTORGADA'::text,
      f.creado_por,
      ''::text,
      ''::text,
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
      ) as personas,
      case when f.actualizado_por is not null and f.actualizado_por <> (select auth.uid())
        then coalesce(nullif(trim(coalesce(ed.nombres, '')), ''), nullif(trim(coalesce(ed.correo, '')), ''), 'Otra persona')
        else '' end as editor_nombre,
      case when f.actualizado_por is not null and f.actualizado_por <> (select auth.uid())
        then (extract(epoch from f.updated_at) * 1000)::bigint
        else 0::bigint end as editada_en,
      f.organizacion_id
    from public.fichas_familiares f
    left join public.perfiles ed on ed.id = f.actualizado_por
    where f.deleted_at is null
      and f.creado_por = (select auth.uid())
  ) propias
  where propias.personas > 0 or propias.editada_en > 0;
$function$;

revoke all on function public.info_fichas_compartidas() from public, anon;
grant execute on function public.info_fichas_compartidas() to authenticated;
