-- Ruralitos: Salas, EAIS, territorios y acceso compartido por alcance.
-- Mantiene las organizaciones existentes y las presenta como Salas.

alter table public.organizaciones
  add column if not exists establecimiento_id bigint references public.establecimientos_salud(id),
  add column if not exists descripcion text not null default '';

update public.organizaciones o
set establecimiento_id = (
  select mo.establecimiento_id
  from public.miembros_organizacion mo
  where mo.organizacion_id = o.id and mo.establecimiento_id is not null
  order by (mo.rol = 'ADMIN') desc, mo.creado_en
  limit 1
)
where o.establecimiento_id is null
  and exists (
    select 1
    from public.miembros_organizacion mo
    where mo.organizacion_id = o.id and mo.establecimiento_id is not null
  );

create index if not exists organizaciones_establecimiento_idx
  on public.organizaciones(establecimiento_id);

create table if not exists public.eais (
  id uuid primary key default gen_random_uuid(),
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  numero integer not null check (numero between 1 and 999),
  activo boolean not null default true,
  creado_por uuid not null default auth.uid() references auth.users(id),
  creado_en timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint eais_organizacion_numero_key unique (organizacion_id, numero),
  constraint eais_id_organizacion_key unique (id, organizacion_id)
);

create index if not exists eais_organizacion_activo_idx
  on public.eais(organizacion_id, activo);

create table if not exists public.territorios (
  id uuid primary key default gen_random_uuid(),
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  eais_id uuid not null,
  tipo text not null check (tipo in ('BARRIO', 'COMUNIDAD')),
  nombre text not null check (char_length(btrim(nombre)) between 2 and 80),
  activo boolean not null default true,
  creado_por uuid not null default auth.uid() references auth.users(id),
  creado_en timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint territorios_eais_org_fk foreign key (eais_id, organizacion_id)
    references public.eais(id, organizacion_id) on delete cascade,
  constraint territorios_eais_tipo_nombre_key unique (eais_id, tipo, nombre),
  constraint territorios_id_organizacion_key unique (id, organizacion_id)
);

create index if not exists territorios_org_eais_activo_idx
  on public.territorios(organizacion_id, eais_id, activo);

create table if not exists public.accesos_sala (
  id uuid primary key default gen_random_uuid(),
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  alcance text not null check (alcance in ('SALA', 'EAIS', 'TERRITORIO')),
  eais_id uuid,
  territorio_id uuid,
  permiso text not null check (permiso in ('LECTOR', 'EDITOR', 'ADMINISTRADOR')),
  activo boolean not null default true,
  creado_por uuid references auth.users(id),
  creado_en timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint accesos_sala_eais_org_fk foreign key (eais_id, organizacion_id)
    references public.eais(id, organizacion_id) on delete cascade,
  constraint accesos_sala_territorio_org_fk foreign key (territorio_id, organizacion_id)
    references public.territorios(id, organizacion_id) on delete cascade,
  constraint accesos_sala_scope_check check (
    (alcance = 'SALA' and eais_id is null and territorio_id is null)
    or (alcance = 'EAIS' and eais_id is not null and territorio_id is null)
    or (alcance = 'TERRITORIO' and eais_id is not null and territorio_id is not null)
  ),
  constraint accesos_sala_scope_key unique nulls not distinct
    (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
);

create index if not exists accesos_sala_usuario_activo_idx
  on public.accesos_sala(usuario_id, activo, organizacion_id);
create index if not exists accesos_sala_org_eais_territorio_idx
  on public.accesos_sala(organizacion_id, eais_id, territorio_id);

insert into public.accesos_sala (
  organizacion_id, usuario_id, alcance, permiso, activo, creado_por
)
select
  m.organizacion_id,
  m.usuario_id,
  'SALA',
  case
    when m.rol = 'ADMIN' then 'ADMINISTRADOR'
    when m.rol = 'MEDICO' then 'EDITOR'
    else 'LECTOR'
  end,
  m.activo,
  m.creado_por
from public.miembros_organizacion m
on conflict (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
do update set
  permiso = excluded.permiso,
  activo = excluded.activo,
  updated_at = now();

alter table public.fichas_familiares
  add column if not exists eais_id uuid,
  add column if not exists territorio_id uuid;

do $migration$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'fichas_eais_org_fk'
  ) then
    alter table public.fichas_familiares
      add constraint fichas_eais_org_fk foreign key (eais_id, organizacion_id)
      references public.eais(id, organizacion_id);
  end if;
  if not exists (
    select 1 from pg_constraint where conname = 'fichas_territorio_org_fk'
  ) then
    alter table public.fichas_familiares
      add constraint fichas_territorio_org_fk foreign key (territorio_id, organizacion_id)
      references public.territorios(id, organizacion_id);
  end if;
end
$migration$;

create index if not exists fichas_org_eais_idx
  on public.fichas_familiares(organizacion_id, eais_id)
  where deleted_at is null;
create index if not exists fichas_org_territorio_idx
  on public.fichas_familiares(organizacion_id, territorio_id)
  where deleted_at is null;

alter table public.invitaciones_organizacion
  add column if not exists alcance text not null default 'SALA',
  add column if not exists eais_id uuid,
  add column if not exists territorio_id uuid,
  add column if not exists permiso text not null default 'EDITOR';

do $migration$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'invitaciones_alcance_check'
  ) then
    alter table public.invitaciones_organizacion
      add constraint invitaciones_alcance_check
      check (alcance in ('SALA', 'EAIS', 'TERRITORIO'));
  end if;
  if not exists (
    select 1 from pg_constraint where conname = 'invitaciones_permiso_check'
  ) then
    alter table public.invitaciones_organizacion
      add constraint invitaciones_permiso_check
      check (permiso in ('LECTOR', 'EDITOR'));
  end if;
  if not exists (
    select 1 from pg_constraint where conname = 'invitaciones_eais_org_fk'
  ) then
    alter table public.invitaciones_organizacion
      add constraint invitaciones_eais_org_fk foreign key (eais_id, organizacion_id)
      references public.eais(id, organizacion_id) on delete cascade;
  end if;
  if not exists (
    select 1 from pg_constraint where conname = 'invitaciones_territorio_org_fk'
  ) then
    alter table public.invitaciones_organizacion
      add constraint invitaciones_territorio_org_fk foreign key (territorio_id, organizacion_id)
      references public.territorios(id, organizacion_id) on delete cascade;
  end if;
end
$migration$;

create or replace function private.rango_permiso(p_permiso text)
returns integer
language sql
immutable
set search_path = ''
as $function$
  select case upper(coalesce(p_permiso, ''))
    when 'ADMINISTRADOR' then 3
    when 'EDITOR' then 2
    when 'LECTOR' then 1
    else 0
  end;
$function$;

create or replace function private.es_admin(p_organizacion_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1
    from public.miembros_organizacion m
    where m.organizacion_id = p_organizacion_id
      and m.usuario_id = (select auth.uid())
      and m.activo
      and m.rol = 'ADMIN'
  );
$function$;

create or replace function private.tiene_acceso(
  p_organizacion_id uuid,
  p_eais_id uuid default null,
  p_territorio_id uuid default null,
  p_permiso_minimo text default 'LECTOR'
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select
    private.es_admin(p_organizacion_id)
    or exists (
      select 1
      from public.accesos_sala a
      where a.organizacion_id = p_organizacion_id
        and a.usuario_id = (select auth.uid())
        and a.activo
        and private.rango_permiso(a.permiso) >= private.rango_permiso(p_permiso_minimo)
        and (
          a.alcance = 'SALA'
          or (a.alcance = 'EAIS' and a.eais_id = p_eais_id)
          or (
            a.alcance = 'TERRITORIO'
            and (
              a.territorio_id = p_territorio_id
              or (
                p_territorio_id is null
                and exists (
                  select 1 from public.territorios t
                  where t.id = a.territorio_id and t.eais_id = p_eais_id
                )
              )
            )
          )
        )
    );
$function$;

create or replace function private.puede_editar(p_organizacion_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.tiene_acceso(p_organizacion_id, null, null, 'EDITOR');
$function$;

create or replace function private.puede_ver_ficha_valores(
  p_organizacion_id uuid,
  p_eais_id uuid,
  p_territorio_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.tiene_acceso(
    p_organizacion_id, p_eais_id, p_territorio_id, 'LECTOR'
  );
$function$;

create or replace function private.puede_editar_ficha_valores(
  p_organizacion_id uuid,
  p_eais_id uuid,
  p_territorio_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.tiene_acceso(
    p_organizacion_id, p_eais_id, p_territorio_id, 'EDITOR'
  );
$function$;

create or replace function private.puede_ver_ficha_id(p_ficha_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1 from public.fichas_familiares f
    where f.id = p_ficha_id
      and private.tiene_acceso(
        f.organizacion_id, f.eais_id, f.territorio_id, 'LECTOR'
      )
  );
$function$;

create or replace function private.puede_editar_ficha_id(p_ficha_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1 from public.fichas_familiares f
    where f.id = p_ficha_id
      and private.tiene_acceso(
        f.organizacion_id, f.eais_id, f.territorio_id, 'EDITOR'
      )
  );
$function$;

create or replace function private.puede_ver_calificacion_id(p_calificacion_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1 from public.calificaciones_riesgo c
    where c.id = p_calificacion_id
      and private.puede_ver_ficha_id(c.ficha_id)
  );
$function$;

create or replace function private.puede_editar_calificacion_id(p_calificacion_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select exists (
    select 1 from public.calificaciones_riesgo c
    where c.id = p_calificacion_id
      and private.puede_editar_ficha_id(c.ficha_id)
  );
$function$;

create or replace function private.puede_ver_storage(p_nombre text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.puede_ver_ficha_id(
    private.uuid_seguro(split_part(p_nombre, '/', 2))
  );
$function$;

create or replace function private.puede_editar_storage(p_nombre text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
  select private.puede_editar_ficha_id(
    private.uuid_seguro(split_part(p_nombre, '/', 2))
  );
$function$;

alter table public.eais enable row level security;
alter table public.territorios enable row level security;
alter table public.accesos_sala enable row level security;

drop policy if exists eais_leer_alcance on public.eais;
drop policy if exists eais_insertar_admin on public.eais;
drop policy if exists eais_actualizar_admin on public.eais;
drop policy if exists eais_eliminar_admin on public.eais;
create policy eais_leer_alcance on public.eais
  for select to authenticated
  using (private.tiene_acceso(organizacion_id, id, null, 'LECTOR'));
create policy eais_insertar_admin on public.eais
  for insert to authenticated
  with check (private.es_admin(organizacion_id));
create policy eais_actualizar_admin on public.eais
  for update to authenticated
  using (private.es_admin(organizacion_id))
  with check (private.es_admin(organizacion_id));
create policy eais_eliminar_admin on public.eais
  for delete to authenticated
  using (private.es_admin(organizacion_id));

drop policy if exists territorios_leer_alcance on public.territorios;
drop policy if exists territorios_insertar_admin on public.territorios;
drop policy if exists territorios_actualizar_admin on public.territorios;
drop policy if exists territorios_eliminar_admin on public.territorios;
create policy territorios_leer_alcance on public.territorios
  for select to authenticated
  using (private.tiene_acceso(organizacion_id, eais_id, id, 'LECTOR'));
create policy territorios_insertar_admin on public.territorios
  for insert to authenticated
  with check (private.es_admin(organizacion_id));
create policy territorios_actualizar_admin on public.territorios
  for update to authenticated
  using (private.es_admin(organizacion_id))
  with check (private.es_admin(organizacion_id));
create policy territorios_eliminar_admin on public.territorios
  for delete to authenticated
  using (private.es_admin(organizacion_id));

drop policy if exists accesos_sala_leer on public.accesos_sala;
drop policy if exists accesos_sala_insertar_admin on public.accesos_sala;
drop policy if exists accesos_sala_actualizar_admin on public.accesos_sala;
drop policy if exists accesos_sala_eliminar_admin on public.accesos_sala;
create policy accesos_sala_leer on public.accesos_sala
  for select to authenticated
  using (usuario_id = (select auth.uid()) or private.es_admin(organizacion_id));
create policy accesos_sala_insertar_admin on public.accesos_sala
  for insert to authenticated
  with check (private.es_admin(organizacion_id));
create policy accesos_sala_actualizar_admin on public.accesos_sala
  for update to authenticated
  using (private.es_admin(organizacion_id))
  with check (private.es_admin(organizacion_id));
create policy accesos_sala_eliminar_admin on public.accesos_sala
  for delete to authenticated
  using (private.es_admin(organizacion_id));

do $policies$
declare
  r record;
begin
  for r in
    select schemaname, tablename, policyname
    from pg_policies
    where schemaname = 'public'
      and tablename in (
        'fichas_familiares', 'miembros_familia', 'embarazadas',
        'mortalidad_familiar', 'calificaciones_riesgo', 'valores_riesgo',
        'gestion_riesgo', 'contaminacion_ambiental', 'lugares_tratamiento',
        'adjuntos_ficha', 'historial_fichas'
      )
  loop
    execute format(
      'drop policy if exists %I on %I.%I',
      r.policyname, r.schemaname, r.tablename
    );
  end loop;
end
$policies$;

create policy fichas_leer_alcance on public.fichas_familiares
  for select to authenticated
  using (private.puede_ver_ficha_valores(organizacion_id, eais_id, territorio_id));
create policy fichas_insertar_alcance on public.fichas_familiares
  for insert to authenticated
  with check (private.puede_editar_ficha_valores(organizacion_id, eais_id, territorio_id));
create policy fichas_actualizar_alcance on public.fichas_familiares
  for update to authenticated
  using (private.puede_editar_ficha_valores(organizacion_id, eais_id, territorio_id))
  with check (private.puede_editar_ficha_valores(organizacion_id, eais_id, territorio_id));

do $policies$
declare
  v_tabla text;
begin
  foreach v_tabla in array array[
    'miembros_familia', 'embarazadas', 'mortalidad_familiar',
    'calificaciones_riesgo', 'gestion_riesgo', 'contaminacion_ambiental',
    'lugares_tratamiento', 'adjuntos_ficha'
  ]
  loop
    execute format(
      'create policy %I on public.%I for select to authenticated using (private.puede_ver_ficha_id(ficha_id))',
      v_tabla || '_leer_alcance', v_tabla
    );
    execute format(
      'create policy %I on public.%I for insert to authenticated with check (private.puede_editar_ficha_id(ficha_id))',
      v_tabla || '_insertar_alcance', v_tabla
    );
    execute format(
      'create policy %I on public.%I for update to authenticated using (private.puede_editar_ficha_id(ficha_id)) with check (private.puede_editar_ficha_id(ficha_id))',
      v_tabla || '_actualizar_alcance', v_tabla
    );
  end loop;
end
$policies$;

create policy valores_riesgo_leer_alcance on public.valores_riesgo
  for select to authenticated
  using (private.puede_ver_calificacion_id(calificacion_id));
create policy valores_riesgo_insertar_alcance on public.valores_riesgo
  for insert to authenticated
  with check (private.puede_editar_calificacion_id(calificacion_id));
create policy valores_riesgo_actualizar_alcance on public.valores_riesgo
  for update to authenticated
  using (private.puede_editar_calificacion_id(calificacion_id))
  with check (private.puede_editar_calificacion_id(calificacion_id));

create policy historial_leer_alcance on public.historial_fichas
  for select to authenticated
  using (private.puede_ver_ficha_id(ficha_id));
create policy historial_insertar_alcance on public.historial_fichas
  for insert to authenticated
  with check (
    private.puede_editar_ficha_id(ficha_id)
    and usuario_id = (select auth.uid())
  );

drop policy if exists adjuntos_storage_leer on storage.objects;
drop policy if exists adjuntos_storage_insertar on storage.objects;
drop policy if exists adjuntos_storage_actualizar on storage.objects;
drop policy if exists adjuntos_storage_eliminar on storage.objects;
create policy adjuntos_storage_leer on storage.objects
  for select to authenticated
  using (bucket_id = 'fichas-adjuntos' and private.puede_ver_storage(name));
create policy adjuntos_storage_insertar on storage.objects
  for insert to authenticated
  with check (bucket_id = 'fichas-adjuntos' and private.puede_editar_storage(name));
create policy adjuntos_storage_actualizar on storage.objects
  for update to authenticated
  using (bucket_id = 'fichas-adjuntos' and private.puede_editar_storage(name))
  with check (bucket_id = 'fichas-adjuntos' and private.puede_editar_storage(name));
create policy adjuntos_storage_eliminar on storage.objects
  for delete to authenticated
  using (bucket_id = 'fichas-adjuntos' and private.puede_editar_storage(name));

create or replace function public.crear_sala(p_codigo_uo text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_usuario uuid := auth.uid();
  v_establecimiento public.establecimientos_salud%rowtype;
  v_organizacion uuid;
  v_codigo text;
begin
  if v_usuario is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;

  select * into v_establecimiento
  from public.establecimientos_salud
  where upper(codigo_uo) = upper(btrim(p_codigo_uo)) and activo
  limit 1;

  if not found then
    raise exception 'El centro de salud no existe o esta inactivo.' using errcode = '23503';
  end if;

  select o.id into v_organizacion
  from public.organizaciones o
  join public.miembros_organizacion m
    on m.organizacion_id = o.id
  where o.creado_por = v_usuario
    and o.establecimiento_id = v_establecimiento.id
    and m.usuario_id = v_usuario
    and m.activo
  limit 1;

  if v_organizacion is null then
    v_codigo := 'SALA-' || upper(substr(md5(v_usuario::text || v_establecimiento.id::text || clock_timestamp()::text), 1, 16));
    insert into public.organizaciones (
      nombre, codigo, creado_por, establecimiento_id
    ) values (
      v_establecimiento.nombre_centro_salud,
      v_codigo,
      v_usuario,
      v_establecimiento.id
    ) returning id into v_organizacion;

    insert into public.miembros_organizacion (
      organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por
    ) values (
      v_organizacion, v_usuario, v_establecimiento.id, 'ADMIN', true, v_usuario
    );
  end if;

  insert into public.accesos_sala (
    organizacion_id, usuario_id, alcance, permiso, activo, creado_por
  ) values (
    v_organizacion, v_usuario, 'SALA', 'ADMINISTRADOR', true, v_usuario
  )
  on conflict (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
  do update set permiso = 'ADMINISTRADOR', activo = true, updated_at = now();

  return v_organizacion;
end;
$function$;

create or replace function public.crear_codigo_acceso(
  p_organizacion_id uuid,
  p_alcance text default 'SALA',
  p_eais_id uuid default null,
  p_territorio_id uuid default null,
  p_permiso text default 'EDITOR',
  p_correo text default null,
  p_horas_vigencia integer default 168
)
returns table(codigo text, expira_en timestamptz)
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_codigo text;
  v_expira timestamptz;
  v_alcance text := upper(btrim(p_alcance));
  v_permiso text := upper(btrim(p_permiso));
begin
  if not private.es_admin(p_organizacion_id) then
    raise exception 'Solo un administrador de la Sala puede compartir acceso.' using errcode = '42501';
  end if;
  if v_alcance not in ('SALA', 'EAIS', 'TERRITORIO') then
    raise exception 'Alcance no valido.' using errcode = '22023';
  end if;
  if v_permiso not in ('LECTOR', 'EDITOR') then
    raise exception 'Permiso no valido.' using errcode = '22023';
  end if;
  if p_horas_vigencia not between 1 and 720 then
    raise exception 'La vigencia debe estar entre 1 y 720 horas.' using errcode = '22023';
  end if;
  if v_alcance = 'SALA' and (p_eais_id is not null or p_territorio_id is not null) then
    raise exception 'Una invitacion de Sala no debe indicar EAIS ni territorio.' using errcode = '22023';
  end if;
  if v_alcance in ('EAIS', 'TERRITORIO') and not exists (
    select 1 from public.eais
    where id = p_eais_id and organizacion_id = p_organizacion_id and activo
  ) then
    raise exception 'El EAIS seleccionado no pertenece a la Sala.' using errcode = '23503';
  end if;
  if v_alcance = 'EAIS' and p_territorio_id is not null then
    raise exception 'Una invitacion de EAIS no debe indicar territorio.' using errcode = '22023';
  end if;
  if v_alcance = 'TERRITORIO' and not exists (
    select 1 from public.territorios
    where id = p_territorio_id
      and eais_id = p_eais_id
      and organizacion_id = p_organizacion_id
      and activo
  ) then
    raise exception 'El territorio seleccionado no pertenece al EAIS.' using errcode = '23503';
  end if;

  v_codigo := upper(encode(extensions.gen_random_bytes(8), 'hex'));
  v_expira := now() + make_interval(hours => p_horas_vigencia);

  insert into public.invitaciones_organizacion (
    organizacion_id, correo, rol, establecimiento_id, codigo_hash,
    expira_en, creado_por, alcance, eais_id, territorio_id, permiso
  ) values (
    p_organizacion_id,
    nullif(lower(btrim(p_correo)), ''),
    case when v_permiso = 'LECTOR' then 'ESTADISTICA' else 'MEDICO' end,
    (select establecimiento_id from public.organizaciones where id = p_organizacion_id),
    extensions.digest(v_codigo, 'sha256'),
    v_expira,
    auth.uid(),
    v_alcance,
    p_eais_id,
    p_territorio_id,
    v_permiso
  );

  return query select v_codigo, v_expira;
end;
$function$;

create or replace function public.aceptar_invitacion(p_codigo text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_usuario uuid := auth.uid();
  v_correo text;
  v_invitacion public.invitaciones_organizacion%rowtype;
begin
  if v_usuario is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;

  select lower(email) into v_correo from auth.users where id = v_usuario;

  select * into v_invitacion
  from public.invitaciones_organizacion
  where codigo_hash = extensions.digest(upper(btrim(p_codigo)), 'sha256')
    and usado_en is null
    and expira_en > now()
  for update;

  if not found then
    raise exception 'El codigo es invalido, ya fue usado o expiro.' using errcode = '22023';
  end if;
  if v_invitacion.correo is not null and lower(v_invitacion.correo) <> v_correo then
    raise exception 'La invitacion pertenece a otro correo.' using errcode = '42501';
  end if;

  insert into public.miembros_organizacion (
    organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por
  ) values (
    v_invitacion.organizacion_id,
    v_usuario,
    v_invitacion.establecimiento_id,
    case when v_invitacion.permiso = 'LECTOR' then 'ESTADISTICA' else 'MEDICO' end,
    true,
    v_invitacion.creado_por
  )
  on conflict (organizacion_id, usuario_id) do update
  set establecimiento_id = excluded.establecimiento_id,
      rol = case
        when public.miembros_organizacion.rol = 'ADMIN' then 'ADMIN'
        else excluded.rol
      end,
      activo = true,
      updated_at = now();

  insert into public.accesos_sala (
    organizacion_id, usuario_id, alcance, eais_id, territorio_id,
    permiso, activo, creado_por
  ) values (
    v_invitacion.organizacion_id,
    v_usuario,
    v_invitacion.alcance,
    v_invitacion.eais_id,
    v_invitacion.territorio_id,
    v_invitacion.permiso,
    true,
    v_invitacion.creado_por
  )
  on conflict (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
  do update set
    permiso = excluded.permiso,
    activo = true,
    updated_at = now();

  update public.invitaciones_organizacion
  set usado_en = now(), usado_por = v_usuario
  where id = v_invitacion.id;

  return v_invitacion.organizacion_id;
end;
$function$;

create or replace function public.crear_invitacion(
  p_organizacion_id uuid,
  p_correo text default null,
  p_rol text default 'MEDICO',
  p_establecimiento_id bigint default null,
  p_horas_vigencia integer default 168
)
returns table(codigo text, expira_en timestamptz)
language sql
security definer
set search_path = ''
as $function$
  select *
  from public.crear_codigo_acceso(
    p_organizacion_id,
    'SALA',
    null,
    null,
    case when upper(coalesce(p_rol, '')) = 'ESTADISTICA' then 'LECTOR' else 'EDITOR' end,
    p_correo,
    p_horas_vigencia
  );
$function$;

grant select, insert, update, delete on public.eais to authenticated;
grant select, insert, update, delete on public.territorios to authenticated;
grant select, insert, update, delete on public.accesos_sala to authenticated;

revoke all on function public.crear_sala(text) from public;
revoke all on function public.crear_codigo_acceso(uuid,text,uuid,uuid,text,text,integer) from public;
revoke all on function public.aceptar_invitacion(text) from public;
revoke all on function public.crear_invitacion(uuid,text,text,bigint,integer) from public;
grant execute on function public.crear_sala(text) to authenticated;
grant execute on function public.crear_codigo_acceso(uuid,text,uuid,uuid,text,text,integer) to authenticated;
grant execute on function public.aceptar_invitacion(text) to authenticated;
grant execute on function public.crear_invitacion(uuid,text,text,bigint,integer) to authenticated;
