-- Ruralitos: endurecimiento posterior a la migracion inicial.
-- Se ocultan funciones internas del Data API y se cubren todas las claves foraneas.

create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated, service_role;

-- Las dependencias de triggers y politicas conservan el OID al mover la funcion.
alter function public.actualizar_marca_tiempo() set schema private;
alter function public.actualizar_version_sync() set schema private;
alter function public.uuid_seguro(text) set schema private;
alter function public.crear_perfil_nuevo_usuario() set schema private;
alter function public.sincronizar_correo_perfil() set schema private;
alter function public.es_miembro(uuid) set schema private;
alter function public.es_admin(uuid) set schema private;
alter function public.puede_editar(uuid) set schema private;
alter function public.comparte_organizacion(uuid) set schema private;

revoke execute on all functions in schema private from public, anon, authenticated;
grant execute on function private.uuid_seguro(text) to authenticated;
grant execute on function private.es_miembro(uuid) to authenticated;
grant execute on function private.es_admin(uuid) to authenticated;
grant execute on function private.puede_editar(uuid) to authenticated;
grant execute on function private.comparte_organizacion(uuid) to authenticated;

-- Las tres RPC publicas estan disponibles solo despues de iniciar sesion.
revoke execute on function public.crear_organizacion_inicial(text, text, bigint)
  from public, anon;
revoke execute on function public.crear_invitacion(uuid, text, text, bigint, integer)
  from public, anon;
revoke execute on function public.aceptar_invitacion(text)
  from public, anon;
grant execute on function public.crear_organizacion_inicial(text, text, bigint)
  to authenticated;
grant execute on function public.crear_invitacion(uuid, text, text, bigint, integer)
  to authenticated;
grant execute on function public.aceptar_invitacion(text)
  to authenticated;

-- Funcion creada por la opcion automatica de RLS del panel de Supabase.
revoke execute on function public.rls_auto_enable()
  from public, anon, authenticated;

-- La tabla guarda hashes de invitacion y solo se usa desde RPC seguras.
create policy invitaciones_sin_acceso_directo
on public.invitaciones_organizacion
for all to authenticated
using (false)
with check (false);

-- Los indices compuestos cubren exactamente las claves foraneas de pertenencia.
drop index if exists public.adjuntos_ficha_idx;
create index adjuntos_ficha_idx
  on public.adjuntos_ficha (ficha_id, organizacion_id);

drop index if exists public.calificaciones_ficha_idx;
create index calificaciones_ficha_idx
  on public.calificaciones_riesgo (ficha_id, organizacion_id);

drop index if exists public.contaminacion_ficha_idx;
create index contaminacion_ficha_idx
  on public.contaminacion_ambiental (ficha_id, organizacion_id);

drop index if exists public.embarazadas_ficha_idx;
create index embarazadas_ficha_idx
  on public.embarazadas (ficha_id, organizacion_id);

drop index if exists public.gestion_riesgo_ficha_idx;
create index gestion_riesgo_ficha_idx
  on public.gestion_riesgo (ficha_id, organizacion_id);

drop index if exists public.historial_ficha_idx;
create index historial_ficha_idx
  on public.historial_fichas (ficha_id, organizacion_id, ocurrido_en desc);

drop index if exists public.lugares_ficha_idx;
create index lugares_ficha_idx
  on public.lugares_tratamiento (ficha_id, organizacion_id);

drop index if exists public.miembros_familia_ficha_idx;
create index miembros_familia_ficha_idx
  on public.miembros_familia (ficha_id, organizacion_id);

drop index if exists public.mortalidad_ficha_idx;
create index mortalidad_ficha_idx
  on public.mortalidad_familiar (ficha_id, organizacion_id);

drop index if exists public.valores_riesgo_calificacion_idx;
create index valores_riesgo_calificacion_idx
  on public.valores_riesgo (calificacion_id, organizacion_id);

-- Indices de auditoria y relaciones secundarias.
create index adjuntos_creado_por_idx
  on public.adjuntos_ficha (creado_por) where creado_por is not null;
create index calificaciones_creado_por_idx
  on public.calificaciones_riesgo (creado_por) where creado_por is not null;
create index contaminacion_creado_por_idx
  on public.contaminacion_ambiental (creado_por) where creado_por is not null;
create index embarazadas_creado_por_idx
  on public.embarazadas (creado_por) where creado_por is not null;
create index gestion_creado_por_idx
  on public.gestion_riesgo (creado_por) where creado_por is not null;
create index miembros_familia_creado_por_idx
  on public.miembros_familia (creado_por) where creado_por is not null;
create index mortalidad_creado_por_idx
  on public.mortalidad_familiar (creado_por) where creado_por is not null;

create index fichas_creado_por_idx
  on public.fichas_familiares (creado_por) where creado_por is not null;
create index fichas_actualizado_por_idx
  on public.fichas_familiares (actualizado_por) where actualizado_por is not null;
create index fichas_completado_por_idx
  on public.fichas_familiares (completado_por) where completado_por is not null;
create index fichas_establecimiento_idx
  on public.fichas_familiares (establecimiento_id) where establecimiento_id is not null;

create index historial_usuario_idx
  on public.historial_fichas (usuario_id) where usuario_id is not null;
create index invitaciones_creado_por_idx
  on public.invitaciones_organizacion (creado_por);
create index invitaciones_establecimiento_idx
  on public.invitaciones_organizacion (establecimiento_id)
  where establecimiento_id is not null;
create index invitaciones_usado_por_idx
  on public.invitaciones_organizacion (usado_por)
  where usado_por is not null;
create index miembros_organizacion_creado_por_idx
  on public.miembros_organizacion (creado_por) where creado_por is not null;
create index organizaciones_creado_por_idx
  on public.organizaciones (creado_por);
