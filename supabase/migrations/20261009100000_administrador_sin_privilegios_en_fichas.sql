-- En la aplicacion el administrador tiene las mismas funciones que los demas roles: ya no ve ni edita fichas ajenas por ser
-- administrador (ni por tener acceso a toda la Sala). Cada quien ve y edita sus fichas (las que creo) y las que otra persona
-- le comparte. Tampoco puede darse acceso a fichas de otros. Los EAIS y barrios los puede gestionar cualquier miembro que edita.
-- Aplicar DESPUES de 20261008200000_traspaso_de_fichas_por_codigo.sql.

-- 1) Quien puede ver o editar una ficha: su autor, o quien la recibio (ficha suelta o grupo del autor).
create or replace function private.tiene_acceso_ficha(
  p_organizacion_id uuid,
  p_eais_id uuid,
  p_territorio_id uuid,
  p_ficha_id uuid,
  p_permiso_minimo text default 'LECTOR'
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select
    exists (
      select 1 from public.fichas_familiares f
      where f.id = p_ficha_id and f.creado_por = (select auth.uid())
    )
    or exists (
      select 1
      from public.accesos_ficha a
      where a.ficha_id = p_ficha_id
        and a.usuario_id = (select auth.uid())
        and a.activo
        and private.rango_permiso(a.permiso) >= private.rango_permiso(p_permiso_minimo)
    )
    or exists (
      select 1
      from public.fichas_familiares f
      join public.accesos_autor a
        on a.autor_id = f.creado_por
       and a.organizacion_id = f.organizacion_id
      where f.id = p_ficha_id
        and a.usuario_id = (select auth.uid())
        and a.activo
        and private.rango_permiso(a.permiso) >= private.rango_permiso(p_permiso_minimo)
        and (
          a.alcance = 'SALA'
          or (a.alcance = 'EAIS' and a.eais_id = f.eais_id)
          or (a.alcance = 'TERRITORIO' and a.territorio_id = f.territorio_id)
        )
    );
$function$;

-- Al crear una ficha, su autor tiene que poder leerla en la misma instruccion (INSERT ... RETURNING): por eso la
-- autoria tambien se comprueba con la propia fila.
drop policy if exists fichas_leer_alcance on public.fichas_familiares;
create policy fichas_leer_alcance on public.fichas_familiares
  for select to authenticated
  using (
    creado_por = (select auth.uid())
    or private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'LECTOR')
  );

drop policy if exists fichas_actualizar_alcance on public.fichas_familiares;
create policy fichas_actualizar_alcance on public.fichas_familiares
  for update to authenticated
  using (
    creado_por = (select auth.uid())
    or private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'EDITOR')
  )
  with check (
    creado_por = (select auth.uid())
    or private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'EDITOR')
  );

-- 2) El administrador ya no maneja los accesos de fichas de los demas: solo se ven los propios (los que di o los que recibi)
-- y se escriben con las funciones de compartir, traspasar y quitar.
drop policy if exists accesos_ficha_eliminar_admin on public.accesos_ficha;
drop policy if exists accesos_ficha_insertar_admin on public.accesos_ficha;
drop policy if exists accesos_ficha_actualizar_admin on public.accesos_ficha;
drop policy if exists accesos_ficha_leer on public.accesos_ficha;
create policy accesos_ficha_leer on public.accesos_ficha
  for select to authenticated
  using (usuario_id = (select auth.uid()) or creado_por = (select auth.uid()));

-- 3) EAIS y barrios: los gestiona cualquier miembro que edita (administrador o medico), no solo el administrador.
create or replace function private.es_miembro_que_edita(p_organizacion_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1 from public.miembros_organizacion m
    where m.organizacion_id = p_organizacion_id
      and m.usuario_id = (select auth.uid())
      and m.activo
      and m.rol in ('ADMIN', 'MEDICO')
  );
$function$;

drop policy if exists eais_insertar_admin on public.eais;
drop policy if exists eais_actualizar_admin on public.eais;
drop policy if exists eais_eliminar_admin on public.eais;
create policy eais_insertar_miembro on public.eais
  for insert to authenticated with check (private.es_miembro_que_edita(organizacion_id));
create policy eais_actualizar_miembro on public.eais
  for update to authenticated
  using (private.es_miembro_que_edita(organizacion_id))
  with check (private.es_miembro_que_edita(organizacion_id));

drop policy if exists territorios_insertar_admin on public.territorios;
drop policy if exists territorios_actualizar_admin on public.territorios;
drop policy if exists territorios_eliminar_admin on public.territorios;
create policy territorios_insertar_miembro on public.territorios
  for insert to authenticated with check (private.es_miembro_que_edita(organizacion_id));
create policy territorios_actualizar_miembro on public.territorios
  for update to authenticated
  using (private.es_miembro_que_edita(organizacion_id))
  with check (private.es_miembro_que_edita(organizacion_id));
