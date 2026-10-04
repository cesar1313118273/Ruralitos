-- Cada usuario comparte SUS fichas y quita el acceso a quienes el invito; nadie necesita ser administrador.
-- Reemplaza el modelo de la migracion 20261005120000 en tres puntos: quien puede crear codigos, que entrega un codigo de
-- Sala/EAIS/barrio (solo las fichas de quien lo creo) y como se quita el acceso. Aplicar DESPUES de 20261005120000.

-- 1) Acceso a las fichas que un autor creo, dentro de un centro, EAIS o barrio.
create table if not exists public.accesos_autor (
  id uuid primary key default gen_random_uuid(),
  organizacion_id uuid not null references public.organizaciones(id) on delete cascade,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  autor_id uuid not null references auth.users(id) on delete cascade,
  alcance text not null check (alcance in ('SALA', 'EAIS', 'TERRITORIO')),
  eais_id uuid,
  territorio_id uuid,
  permiso text not null check (permiso in ('LECTOR', 'EDITOR')),
  activo boolean not null default true,
  creado_en timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint accesos_autor_eais_org_fk foreign key (eais_id, organizacion_id)
    references public.eais(id, organizacion_id) on delete cascade,
  constraint accesos_autor_territorio_org_fk foreign key (territorio_id, organizacion_id)
    references public.territorios(id, organizacion_id) on delete cascade,
  constraint accesos_autor_alcance_coherente check (
    (alcance = 'SALA' and eais_id is null and territorio_id is null)
    or (alcance = 'EAIS' and eais_id is not null and territorio_id is null)
    or (alcance = 'TERRITORIO' and eais_id is not null and territorio_id is not null)
  ),
  constraint accesos_autor_clave unique nulls not distinct
    (organizacion_id, usuario_id, autor_id, alcance, eais_id, territorio_id)
);

create index if not exists accesos_autor_usuario_idx
  on public.accesos_autor(usuario_id, activo, organizacion_id);
create index if not exists accesos_autor_autor_idx
  on public.accesos_autor(autor_id, activo);
create index if not exists accesos_autor_eais_org_fk_idx
  on public.accesos_autor(eais_id, organizacion_id);
create index if not exists accesos_autor_territorio_org_fk_idx
  on public.accesos_autor(territorio_id, organizacion_id);

alter table public.accesos_autor enable row level security;

-- Cada quien ve lo que recibio y lo que dio; solo se escribe con las funciones de abajo.
drop policy if exists accesos_autor_leer on public.accesos_autor;
create policy accesos_autor_leer on public.accesos_autor
  for select to authenticated
  using (usuario_id = (select auth.uid()) or autor_id = (select auth.uid()));
grant select on public.accesos_autor to authenticated;

-- 2) Una ficha tambien se ve o se edita si su autor se la compartio a esta persona.
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

-- 3) Cualquier miembro puede crear un codigo, pero solo con SUS fichas.
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
  if auth.uid() is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
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

    if v_org is null or not exists (
      select 1 from public.miembros_organizacion m
      where m.organizacion_id = v_org and m.usuario_id = auth.uid() and m.activo
    ) then
      raise exception 'No perteneces a esa Sala.' using errcode = '42501';
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
        where id = v_ficha and organizacion_id = v_org
          and deleted_at is null and creado_por = auth.uid()
      ) then
        raise exception 'Solo puedes compartir las fichas que tu creaste y que ya estan sincronizadas.' using errcode = '42501';
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

-- 4) Aceptar un codigo: lo de centro/EAIS/barrio queda limitado a las fichas de quien lo creo.
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
  if v_invitacion.creado_por = v_usuario then
    raise exception 'Este codigo lo creaste tu: compartelo con otra persona.' using errcode = '22023';
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
        set permiso = excluded.permiso, activo = true,
            creado_por = excluded.creado_por, updated_at = now();
      else
        insert into public.accesos_autor (
          organizacion_id, usuario_id, autor_id, alcance, eais_id, territorio_id, permiso, activo
        ) values (
          v_parte.organizacion_id, v_usuario, v_invitacion.creado_por, v_parte.alcance,
          v_parte.eais_id, v_parte.territorio_id, v_invitacion.permiso, true
        )
        on conflict (organizacion_id, usuario_id, autor_id, alcance, eais_id, territorio_id)
        do update set permiso = excluded.permiso, activo = true, updated_at = now();
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

-- 5) Personas con las que YO compartí mis fichas, con un resumen de lo compartido.
create or replace function public.listar_accesos_otorgados()
returns table(
  organizacion_id uuid,
  usuario_id uuid,
  nombres text,
  apellidos text,
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
  )
  select
    p.organizacion_id,
    p.usuario_id,
    pr.nombres,
    pr.apellidos,
    pr.cargo,
    pr.correo,
    case when bool_or(p.permiso = 'EDITOR') then 'EDITOR' else 'LECTOR' end,
    (count(*) filter (where p.alcance = 'SALA'))::integer,
    (count(*) filter (where p.alcance = 'EAIS'))::integer,
    (count(*) filter (where p.alcance = 'TERRITORIO'))::integer,
    (count(*) filter (where p.alcance = 'FICHA'))::integer
  from partes p
  left join public.perfiles pr on pr.id = p.usuario_id
  group by p.organizacion_id, p.usuario_id, pr.nombres, pr.apellidos, pr.cargo, pr.correo
  order by pr.nombres nulls last, pr.apellidos nulls last;
$function$;

-- 6) Quitar el acceso que YO di a una persona (solo lo mio; no toca lo que le dieron otros).
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

revoke all on function public.listar_accesos_otorgados() from public;
revoke all on function public.listar_accesos_otorgados() from anon;
grant execute on function public.listar_accesos_otorgados() to authenticated;
revoke all on function public.quitar_acceso_compartido(uuid, uuid) from public;
revoke all on function public.quitar_acceso_compartido(uuid, uuid) from anon;
grant execute on function public.quitar_acceso_compartido(uuid, uuid) to authenticated;
