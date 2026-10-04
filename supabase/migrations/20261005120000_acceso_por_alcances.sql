-- Compartir acceso con varios centros, EAIS, barrios o fichas sueltas en un solo codigo.
-- Es compatible hacia atras: los codigos de Sala, EAIS o barrio que ya existen siguen funcionando igual.
-- Aplicar ANTES de usar en la app las opciones "Centro de salud" con varios centros, varios EAIS/barrios o "Ficha".

-- 1) Fichas sueltas compartidas con una persona.
create table if not exists public.accesos_ficha (
  id uuid primary key default gen_random_uuid(),
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  ficha_id uuid not null references public.fichas_familiares(id) on delete cascade,
  permiso text not null check (permiso in ('LECTOR', 'EDITOR')),
  activo boolean not null default true,
  creado_por uuid references auth.users(id) on delete set null,
  creado_en timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint accesos_ficha_usuario_ficha_key unique (usuario_id, ficha_id)
);

create index if not exists accesos_ficha_usuario_activo_idx
  on public.accesos_ficha(usuario_id, activo, ficha_id);
create index if not exists accesos_ficha_ficha_idx
  on public.accesos_ficha(ficha_id);
create index if not exists accesos_ficha_organizacion_idx
  on public.accesos_ficha(organizacion_id, usuario_id);
create index if not exists accesos_ficha_creado_por_idx
  on public.accesos_ficha(creado_por);

alter table public.accesos_ficha enable row level security;

drop policy if exists accesos_ficha_leer on public.accesos_ficha;
drop policy if exists accesos_ficha_insertar_admin on public.accesos_ficha;
drop policy if exists accesos_ficha_actualizar_admin on public.accesos_ficha;
drop policy if exists accesos_ficha_eliminar_admin on public.accesos_ficha;
create policy accesos_ficha_leer on public.accesos_ficha
  for select to authenticated
  using (usuario_id = (select auth.uid()) or private.es_admin(organizacion_id));
create policy accesos_ficha_insertar_admin on public.accesos_ficha
  for insert to authenticated
  with check (private.es_admin(organizacion_id));
create policy accesos_ficha_actualizar_admin on public.accesos_ficha
  for update to authenticated
  using (private.es_admin(organizacion_id))
  with check (private.es_admin(organizacion_id));
create policy accesos_ficha_eliminar_admin on public.accesos_ficha
  for delete to authenticated
  using (private.es_admin(organizacion_id));

grant select, insert, update, delete on public.accesos_ficha to authenticated;

-- 2) Lo que lleva cada codigo: una fila por centro, EAIS, barrio o ficha.
create table if not exists public.invitaciones_alcances (
  id uuid primary key default gen_random_uuid(),
  invitacion_id uuid not null references public.invitaciones_organizacion(id) on delete cascade,
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  alcance text not null check (alcance in ('SALA', 'EAIS', 'TERRITORIO', 'FICHA')),
  eais_id uuid,
  territorio_id uuid,
  ficha_id uuid references public.fichas_familiares(id) on delete cascade
);

create index if not exists invitaciones_alcances_invitacion_idx
  on public.invitaciones_alcances(invitacion_id);
create index if not exists invitaciones_alcances_organizacion_idx
  on public.invitaciones_alcances(organizacion_id);
create index if not exists invitaciones_alcances_ficha_idx
  on public.invitaciones_alcances(ficha_id) where ficha_id is not null;

-- Solo la leen y escriben las funciones de abajo (security definer); nadie accede directo.
alter table public.invitaciones_alcances enable row level security;

-- 3) Permiso sobre una ficha: por su Sala, EAIS o barrio, o porque se la compartieron sola.
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
    private.tiene_acceso(p_organizacion_id, p_eais_id, p_territorio_id, p_permiso_minimo)
    or exists (
      select 1
      from public.accesos_ficha a
      where a.ficha_id = p_ficha_id
        and a.usuario_id = (select auth.uid())
        and a.activo
        and private.rango_permiso(a.permiso) >= private.rango_permiso(p_permiso_minimo)
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
      and private.tiene_acceso_ficha(
        f.organizacion_id, f.eais_id, f.territorio_id, f.id, 'LECTOR'
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
      and private.tiene_acceso_ficha(
        f.organizacion_id, f.eais_id, f.territorio_id, f.id, 'EDITOR'
      )
  );
$function$;

drop policy if exists fichas_leer_alcance on public.fichas_familiares;
drop policy if exists fichas_actualizar_alcance on public.fichas_familiares;
create policy fichas_leer_alcance on public.fichas_familiares
  for select to authenticated
  using (private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'LECTOR'));
create policy fichas_actualizar_alcance on public.fichas_familiares
  for update to authenticated
  using (private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'EDITOR'))
  with check (private.tiene_acceso_ficha(organizacion_id, eais_id, territorio_id, id, 'EDITOR'));

-- 4) Un codigo con varias partes.
create or replace function public.crear_codigo_acceso_varios(
  p_items jsonb,
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
  v_permiso text := upper(btrim(p_permiso));
  v_item jsonb;
  v_org uuid;
  v_alcance text;
  v_eais uuid;
  v_territorio uuid;
  v_ficha uuid;
  v_primera uuid;
  v_codigo text;
  v_expira timestamptz;
  v_invitacion uuid;
begin
  if p_items is null or jsonb_typeof(p_items) <> 'array'
     or jsonb_array_length(p_items) not between 1 and 500 then
    raise exception 'Elige entre 1 y 500 partes para compartir.' using errcode = '22023';
  end if;
  if v_permiso not in ('LECTOR', 'EDITOR') then
    raise exception 'Permiso no valido.' using errcode = '22023';
  end if;
  if p_horas_vigencia not between 1 and 720 then
    raise exception 'La vigencia debe estar entre 1 y 720 horas.' using errcode = '22023';
  end if;

  for v_item in select * from jsonb_array_elements(p_items) loop
    v_org := nullif(v_item->>'organizacion_id', '')::uuid;
    v_alcance := upper(btrim(coalesce(v_item->>'alcance', '')));
    v_eais := nullif(v_item->>'eais_id', '')::uuid;
    v_territorio := nullif(v_item->>'territorio_id', '')::uuid;
    v_ficha := nullif(v_item->>'ficha_id', '')::uuid;

    if v_org is null or not private.es_admin(v_org) then
      raise exception 'Solo un administrador de la Sala puede compartir acceso.' using errcode = '42501';
    end if;
    if v_primera is null then v_primera := v_org; end if;

    if v_alcance = 'SALA' then
      null;
    elsif v_alcance = 'EAIS' then
      if not exists (
        select 1 from public.eais
        where id = v_eais and organizacion_id = v_org and activo
      ) then
        raise exception 'El EAIS seleccionado no pertenece a la Sala.' using errcode = '23503';
      end if;
    elsif v_alcance = 'TERRITORIO' then
      if not exists (
        select 1 from public.territorios
        where id = v_territorio and eais_id = v_eais
          and organizacion_id = v_org and activo
      ) then
        raise exception 'El barrio seleccionado no pertenece al EAIS.' using errcode = '23503';
      end if;
    elsif v_alcance = 'FICHA' then
      if not exists (
        select 1 from public.fichas_familiares
        where id = v_ficha and organizacion_id = v_org and deleted_at is null
      ) then
        raise exception 'Una de las fichas elegidas no existe en la Sala o aun no se sincroniza.' using errcode = '23503';
      end if;
    else
      raise exception 'Alcance no valido.' using errcode = '22023';
    end if;
  end loop;

  v_codigo := upper(encode(extensions.gen_random_bytes(8), 'hex'));
  v_expira := now() + make_interval(hours => p_horas_vigencia);

  insert into public.invitaciones_organizacion (
    organizacion_id, correo, rol, establecimiento_id, codigo_hash,
    expira_en, creado_por, alcance, permiso
  ) values (
    v_primera,
    nullif(lower(btrim(p_correo)), ''),
    case when v_permiso = 'LECTOR' then 'ESTADISTICA' else 'MEDICO' end,
    (select establecimiento_id from public.organizaciones where id = v_primera),
    extensions.digest(v_codigo, 'sha256'),
    v_expira,
    auth.uid(),
    'SALA',
    v_permiso
  ) returning id into v_invitacion;

  insert into public.invitaciones_alcances (
    invitacion_id, organizacion_id, alcance, eais_id, territorio_id, ficha_id
  )
  select
    v_invitacion,
    (i->>'organizacion_id')::uuid,
    upper(btrim(i->>'alcance')),
    nullif(i->>'eais_id', '')::uuid,
    nullif(i->>'territorio_id', '')::uuid,
    nullif(i->>'ficha_id', '')::uuid
  from jsonb_array_elements(p_items) as i;

  return query select v_codigo, v_expira;
end;
$function$;

-- 5) Al aceptar un codigo con varias partes se entrega cada una; los codigos de siempre se aceptan igual.
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
  v_parte record;
  v_hay_partes boolean;
  v_rol text;
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

  v_rol := case when v_invitacion.permiso = 'LECTOR' then 'ESTADISTICA' else 'MEDICO' end;
  select exists (
    select 1 from public.invitaciones_alcances where invitacion_id = v_invitacion.id
  ) into v_hay_partes;

  if v_hay_partes then
    for v_parte in
      select * from public.invitaciones_alcances where invitacion_id = v_invitacion.id
    loop
      insert into public.miembros_organizacion (
        organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por
      ) values (
        v_parte.organizacion_id,
        v_usuario,
        (select establecimiento_id from public.organizaciones where id = v_parte.organizacion_id),
        v_rol,
        true,
        v_invitacion.creado_por
      )
      on conflict (organizacion_id, usuario_id) do update
      set rol = case
            when public.miembros_organizacion.rol = 'ADMIN' then 'ADMIN'
            else excluded.rol
          end,
          activo = true,
          updated_at = now();

      if v_parte.alcance = 'FICHA' then
        insert into public.accesos_ficha (
          organizacion_id, usuario_id, ficha_id, permiso, activo, creado_por
        ) values (
          v_parte.organizacion_id, v_usuario, v_parte.ficha_id,
          v_invitacion.permiso, true, v_invitacion.creado_por
        )
        on conflict (usuario_id, ficha_id) do update
        set permiso = excluded.permiso, activo = true, updated_at = now();
      else
        insert into public.accesos_sala (
          organizacion_id, usuario_id, alcance, eais_id, territorio_id,
          permiso, activo, creado_por
        ) values (
          v_parte.organizacion_id, v_usuario, v_parte.alcance,
          v_parte.eais_id, v_parte.territorio_id,
          v_invitacion.permiso, true, v_invitacion.creado_por
        )
        on conflict (organizacion_id, usuario_id, alcance, eais_id, territorio_id)
        do update set
          permiso = excluded.permiso,
          activo = true,
          updated_at = now();
      end if;
    end loop;
  else
    insert into public.miembros_organizacion (
      organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por
    ) values (
      v_invitacion.organizacion_id,
      v_usuario,
      v_invitacion.establecimiento_id,
      v_rol,
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
  end if;

  update public.invitaciones_organizacion
  set usado_en = now(), usado_por = v_usuario
  where id = v_invitacion.id;

  return v_invitacion.organizacion_id;
end;
$function$;

revoke all on function public.crear_codigo_acceso_varios(jsonb, text, text, integer) from public;
revoke all on function public.crear_codigo_acceso_varios(jsonb, text, text, integer) from anon;
grant execute on function public.crear_codigo_acceso_varios(jsonb, text, text, integer) to authenticated;
