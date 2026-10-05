-- Todos los roles son iguales: el administrador no tiene ningun permiso propio. Se quitan las politicas que solo dependian
-- de ser administrador; los miembros y accesos se escriben unicamente con las funciones de compartir, traspasar y quitar.
-- Aplicar DESPUES de 20261009100000_administrador_sin_privilegios_en_fichas.sql.

-- Accesos de Sala (version anterior de compartir): cada quien ve solo los suyos.
drop policy if exists accesos_sala_actualizar_admin on public.accesos_sala;
drop policy if exists accesos_sala_eliminar_admin on public.accesos_sala;
drop policy if exists accesos_sala_insertar_admin on public.accesos_sala;
drop policy if exists accesos_sala_leer on public.accesos_sala;
create policy accesos_sala_leer on public.accesos_sala
  for select to authenticated
  using (usuario_id = (select auth.uid()) or creado_por = (select auth.uid()));

-- Miembros de la Sala: nadie los administra por ser administrador.
drop policy if exists miembros_actualizar_admin on public.miembros_organizacion;
drop policy if exists miembros_eliminar_admin on public.miembros_organizacion;
drop policy if exists miembros_insertar_admin on public.miembros_organizacion;

-- Datos de la Sala: lo puede cambiar cualquier miembro que edita.
drop policy if exists organizaciones_actualizar_admin on public.organizaciones;
create policy organizaciones_actualizar_miembro on public.organizaciones
  for update to authenticated
  using (private.es_miembro_que_edita(id))
  with check (private.es_miembro_que_edita(id));

-- Crear fichas en una Sala: cualquier miembro que edita (administrador o medico).
create or replace function private.puede_editar_ficha_valores(
  p_organizacion_id uuid, p_eais_id uuid, p_territorio_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.es_miembro_que_edita(p_organizacion_id);
$function$;

-- Ver los EAIS y barrios de la Sala: cualquier miembro.
drop policy if exists eais_leer_alcance on public.eais;
create policy eais_leer_miembro on public.eais
  for select to authenticated using (private.es_miembro(organizacion_id));
drop policy if exists territorios_leer_alcance on public.territorios;
create policy territorios_leer_miembro on public.territorios
  for select to authenticated using (private.es_miembro(organizacion_id));
