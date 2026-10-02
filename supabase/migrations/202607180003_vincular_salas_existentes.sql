-- Ruralitos: completa el vínculo de Salas antiguas con su centro de salud.
-- Solo actúa cuando las fichas activas de la Sala tienen un único código UO
-- y ese código coincide con un establecimiento activo del catálogo.

with candidatos as (
  select
    f.organizacion_id,
    min(upper(btrim(f.codigo_uo))) as codigo_uo
  from public.fichas_familiares f
  where f.deleted_at is null
    and nullif(btrim(f.codigo_uo), '') is not null
  group by f.organizacion_id
  having count(distinct upper(btrim(f.codigo_uo))) = 1
)
update public.organizaciones o
set establecimiento_id = es.id,
    updated_at = now()
from candidatos c
join public.establecimientos_salud es
  on upper(btrim(es.codigo_uo)) = c.codigo_uo
 and es.activo
where o.id = c.organizacion_id
  and o.establecimiento_id is null;

update public.miembros_organizacion m
set establecimiento_id = o.establecimiento_id,
    updated_at = now()
from public.organizaciones o
where m.organizacion_id = o.id
  and m.establecimiento_id is null
  and o.establecimiento_id is not null;

update public.fichas_familiares f
set establecimiento_id = o.establecimiento_id,
    updated_at = now()
from public.organizaciones o
where f.organizacion_id = o.id
  and f.establecimiento_id is null
  and o.establecimiento_id is not null;
