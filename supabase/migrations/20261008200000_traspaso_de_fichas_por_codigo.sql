-- Traspasar fichas a otra persona con un codigo, por centro, EAIS, barrio o ficha: la otra persona pasa a ser la duena
-- (la misma ficha, sin copias) y quien las entrega pierde el acceso que tenia por ser su autor.
-- Aplicar DESPUES de 20261008100000_huella_de_cambios.sql.

create table if not exists public.traspasos (
  id uuid primary key default gen_random_uuid(),
  de_usuario uuid not null references auth.users(id) on delete cascade,
  codigo_hash bytea not null unique,
  expira_en timestamptz not null,
  usado_en timestamptz,
  a_usuario uuid references auth.users(id) on delete set null,
  creado_en timestamptz not null default now()
);

create table if not exists public.traspasos_fichas (
  traspaso_id uuid not null references public.traspasos(id) on delete cascade,
  ficha_id uuid not null references public.fichas_familiares(id) on delete cascade,
  primary key (traspaso_id, ficha_id)
);

-- Registro de lo que ya se traspaso, para que el telefono de quien entrego retire sus copias.
create table if not exists public.fichas_traspasadas (
  id uuid primary key default gen_random_uuid(),
  ficha_id uuid not null references public.fichas_familiares(id) on delete cascade,
  de_usuario uuid not null references auth.users(id) on delete cascade,
  a_usuario uuid not null references auth.users(id) on delete cascade,
  en timestamptz not null default now()
);
create index if not exists fichas_traspasadas_de_idx on public.fichas_traspasadas(de_usuario, en desc);
create index if not exists fichas_traspasadas_a_idx on public.fichas_traspasadas(a_usuario);
create index if not exists fichas_traspasadas_ficha_idx on public.fichas_traspasadas(ficha_id);
create index if not exists traspasos_de_idx on public.traspasos(de_usuario);
create index if not exists traspasos_a_idx on public.traspasos(a_usuario);
create index if not exists traspasos_fichas_ficha_idx on public.traspasos_fichas(ficha_id);

alter table public.traspasos enable row level security;
alter table public.traspasos_fichas enable row level security;
alter table public.fichas_traspasadas enable row level security;
-- Solo se escribe con las funciones de abajo; cada quien ve su propio registro.
drop policy if exists traspasos_leer on public.traspasos;
create policy traspasos_leer on public.traspasos
  for select to authenticated using (de_usuario = (select auth.uid()) or a_usuario = (select auth.uid()));
drop policy if exists fichas_traspasadas_leer on public.fichas_traspasadas;
create policy fichas_traspasadas_leer on public.fichas_traspasadas
  for select to authenticated using (de_usuario = (select auth.uid()) or a_usuario = (select auth.uid()));
revoke all on public.traspasos, public.traspasos_fichas, public.fichas_traspasadas from anon;
grant select on public.traspasos, public.fichas_traspasadas to authenticated;

-- 1) Crear el codigo con las fichas que YO creo dentro de lo elegido (se fija la lista en este momento).
create or replace function public.crear_codigo_traspaso(
  p_items jsonb,
  p_horas_vigencia integer default 12
)
returns table(codigo text, expira_en timestamptz, total integer)
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_item jsonb;
  v_org uuid;
  v_alcance text;
  v_eais uuid;
  v_territorio uuid;
  v_ficha uuid;
  v_codigo text;
  v_expira timestamptz;
  v_traspaso uuid;
  v_total integer;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_items is null or jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) not between 1 and 500 then
    raise exception 'Elige entre 1 y 500 partes para traspasar.' using errcode = '22023';
  end if;
  if p_horas_vigencia not between 1 and 720 then
    raise exception 'La vigencia debe estar entre 1 y 720 horas.' using errcode = '22023';
  end if;

  v_codigo := upper(encode(extensions.gen_random_bytes(8), 'hex'));
  v_expira := now() + make_interval(hours => p_horas_vigencia);
  insert into public.traspasos (de_usuario, codigo_hash, expira_en)
  values (v_yo, extensions.digest(v_codigo, 'sha256'), v_expira)
  returning id into v_traspaso;

  for v_item in select * from jsonb_array_elements(p_items) loop
    v_org := nullif(v_item->>'organizacion_id', '')::uuid;
    v_alcance := upper(btrim(coalesce(v_item->>'alcance', '')));
    v_eais := nullif(v_item->>'eais_id', '')::uuid;
    v_territorio := nullif(v_item->>'territorio_id', '')::uuid;
    v_ficha := nullif(v_item->>'ficha_id', '')::uuid;

    if v_org is null or not exists (
      select 1 from public.miembros_organizacion m
      where m.organizacion_id = v_org and m.usuario_id = v_yo and m.activo
    ) then
      raise exception 'No perteneces a esa Sala.' using errcode = '42501';
    end if;

    insert into public.traspasos_fichas (traspaso_id, ficha_id)
    select v_traspaso, f.id
    from public.fichas_familiares f
    where f.organizacion_id = v_org
      and f.creado_por = v_yo
      and f.deleted_at is null
      and (
        v_alcance = 'SALA'
        or (v_alcance = 'EAIS' and f.eais_id = v_eais)
        or (v_alcance = 'TERRITORIO' and f.territorio_id = v_territorio)
        or (v_alcance = 'FICHA' and f.id = v_ficha)
      )
    on conflict do nothing;
  end loop;

  select count(*)::integer into v_total from public.traspasos_fichas where traspaso_id = v_traspaso;
  if v_total = 0 then
    raise exception 'No hay fichas tuyas y sincronizadas en lo que elegiste.' using errcode = '22023';
  end if;
  if v_total > 5000 then
    raise exception 'Son demasiadas fichas para un solo codigo (maximo 5000). Elige menos.' using errcode = '22023';
  end if;

  return query select v_codigo, v_expira, v_total;
end;
$function$;

revoke all on function public.crear_codigo_traspaso(jsonb, integer) from public, anon;
grant execute on function public.crear_codigo_traspaso(jsonb, integer) to authenticated;

-- 2) Aceptar: las fichas pasan a ser de quien acepta; quien las entrego deja de tener acceso por ser su autor.
create or replace function public.aceptar_traspaso(p_codigo text)
returns integer
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_traspaso public.traspasos%rowtype;
  v_ficha record;
  v_total integer := 0;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;

  select * into v_traspaso
  from public.traspasos
  where codigo_hash = extensions.digest(upper(btrim(p_codigo)), 'sha256')
    and usado_en is null
    and expira_en > now()
  for update;
  if not found then
    raise exception 'El codigo es invalido, ya fue usado o expiro.' using errcode = '22023';
  end if;
  if v_traspaso.de_usuario = v_yo then
    raise exception 'Este codigo lo creaste tu: entregaselo a otra persona.' using errcode = '22023';
  end if;

  for v_ficha in
    select f.id, f.organizacion_id
    from public.traspasos_fichas tf
    join public.fichas_familiares f on f.id = tf.ficha_id
    where tf.traspaso_id = v_traspaso.id
      and f.creado_por = v_traspaso.de_usuario
      and f.deleted_at is null
  loop
    -- quien recibe debe ser miembro de la Sala de la ficha
    insert into public.miembros_organizacion (organizacion_id, usuario_id, establecimiento_id, rol, activo, creado_por)
    values (
      v_ficha.organizacion_id, v_yo,
      (select establecimiento_id from public.organizaciones where id = v_ficha.organizacion_id),
      'MEDICO', true, v_traspaso.de_usuario
    )
    on conflict (organizacion_id, usuario_id) do update
    set rol = case when public.miembros_organizacion.rol = 'ADMIN' then 'ADMIN' else 'MEDICO' end,
        activo = true,
        updated_at = now();

    -- los accesos que dio quien entrega ya no valen: la nueva duena decide con quien se comparte
    update public.accesos_ficha
    set activo = false, updated_at = now()
    where ficha_id = v_ficha.id and activo and creado_por = v_traspaso.de_usuario;

    update public.fichas_familiares set creado_por = v_yo where id = v_ficha.id;

    -- quien recibe ve y edita sus fichas
    insert into public.accesos_ficha (organizacion_id, usuario_id, ficha_id, permiso, activo, creado_por)
    values (v_ficha.organizacion_id, v_yo, v_ficha.id, 'EDITOR', true, v_yo)
    on conflict (usuario_id, ficha_id) do update
    set permiso = 'EDITOR', activo = true, creado_por = v_yo, updated_at = now();

    insert into public.fichas_traspasadas (ficha_id, de_usuario, a_usuario)
    values (v_ficha.id, v_traspaso.de_usuario, v_yo);

    v_total := v_total + 1;
  end loop;

  update public.traspasos set usado_en = now(), a_usuario = v_yo where id = v_traspaso.id;
  return v_total;
end;
$function$;

revoke all on function public.aceptar_traspaso(text) from public, anon;
grant execute on function public.aceptar_traspaso(text) to authenticated;

-- 3) Fichas que yo entregue (ultimos 60 dias): el telefono las retira si ya no tengo acceso.
create or replace function public.listar_fichas_traspasadas()
returns table(ficha_id uuid)
language sql
stable
security definer
set search_path = ''
as $function$
  select distinct t.ficha_id
  from public.fichas_traspasadas t
  where t.de_usuario = (select auth.uid()) and t.en > now() - interval '60 days';
$function$;

revoke all on function public.listar_fichas_traspasadas() from public, anon;
grant execute on function public.listar_fichas_traspasadas() to authenticated;

-- 4) Traspasar UNA ficha a alguien que ya la ve: mismo resultado que el codigo, sin que quien entrega conserve acceso.
create or replace function public.traspasar_ficha(p_ficha_id uuid, p_usuario_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $function$
declare
  v_yo uuid := auth.uid();
  v_org uuid;
begin
  if v_yo is null then
    raise exception 'Debes iniciar sesion.' using errcode = '42501';
  end if;
  if p_usuario_id = v_yo then
    raise exception 'La ficha ya es tuya.' using errcode = '22023';
  end if;
  select organizacion_id into v_org
  from public.fichas_familiares
  where id = p_ficha_id and creado_por = v_yo and deleted_at is null;
  if v_org is null then
    raise exception 'Solo quien creo la ficha puede traspasarla.' using errcode = '42501';
  end if;
  if not exists (select 1 from private.accesos_de_ficha(p_ficha_id, v_yo) x where x.usuario_id = p_usuario_id) then
    raise exception 'Primero comparte la ficha con esa persona.' using errcode = '42501';
  end if;

  update public.accesos_ficha
  set activo = false, updated_at = now()
  where ficha_id = p_ficha_id and activo and creado_por = v_yo;

  update public.fichas_familiares set creado_por = p_usuario_id where id = p_ficha_id;

  insert into public.accesos_ficha (organizacion_id, usuario_id, ficha_id, permiso, activo, creado_por)
  values (v_org, p_usuario_id, p_ficha_id, 'EDITOR', true, p_usuario_id)
  on conflict (usuario_id, ficha_id) do update
  set permiso = 'EDITOR', activo = true, creado_por = p_usuario_id, updated_at = now();

  insert into public.fichas_traspasadas (ficha_id, de_usuario, a_usuario) values (p_ficha_id, v_yo, p_usuario_id);
end;
$function$;
