-- Consulta liviana para que la app sepa si hay algo nuevo antes de descargar: devuelve la fecha del ultimo cambio
-- en la Sala (fichas y sus datos) y en los accesos de quien pregunta. Solo la responde a miembros de la Sala.
-- Aplicar DESPUES de 20261007400000_quitar_asignar_visita.sql.
create or replace function public.huella_de_cambios(p_organizacion_id uuid)
returns timestamptz
language plpgsql
stable
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v timestamptz;
begin
  if v_yo is null then
    return null;
  end if;
  if not exists (
    select 1 from public.miembros_organizacion m
    where m.organizacion_id = p_organizacion_id and m.usuario_id = v_yo and m.activo
  ) then
    return null;
  end if;

  select greatest(
    (select max(updated_at) from public.fichas_familiares where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.miembros_familia where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.embarazadas where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.mortalidad_familiar where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.calificaciones_riesgo where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.valores_riesgo where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.gestion_riesgo where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.contaminacion_ambiental where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.lugares_tratamiento where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.adjuntos_ficha where organizacion_id = p_organizacion_id),
    (select max(registrado_en) from public.historial_fichas where organizacion_id = p_organizacion_id),
    (select max(updated_at) from public.accesos_ficha where organizacion_id = p_organizacion_id and usuario_id = v_yo),
    (select max(updated_at) from public.accesos_autor where organizacion_id = p_organizacion_id and usuario_id = v_yo),
    (select max(updated_at) from public.accesos_sala where organizacion_id = p_organizacion_id and usuario_id = v_yo),
    (select max(updated_at) from public.miembros_organizacion where organizacion_id = p_organizacion_id and usuario_id = v_yo)
  ) into v;
  return v;
end;
$function$;

revoke all on function public.huella_de_cambios(uuid) from public, anon;
grant execute on function public.huella_de_cambios(uuid) to authenticated;
