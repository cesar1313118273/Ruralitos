-- Credencial profesional usada automáticamente en las fichas familiares.

alter table public.perfiles
  add column if not exists codigo_senescyt text not null default '';

alter table public.perfiles
  drop constraint if exists perfiles_codigo_senescyt_formato;

alter table public.perfiles
  add constraint perfiles_codigo_senescyt_formato
  check (
    codigo_senescyt = '' or
    codigo_senescyt ~ '^[A-Za-z0-9._/-]{3,40}$'
  );

create unique index if not exists perfiles_codigo_senescyt_unico
  on public.perfiles (upper(codigo_senescyt))
  where codigo_senescyt <> '';

create or replace function private.crear_perfil_nuevo_usuario()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.perfiles (
    id, cedula, nombres, cargo, correo, telefono, codigo_senescyt
  )
  values (
    new.id,
    nullif(btrim(new.raw_user_meta_data ->> 'cedula'), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'nombres'), ''), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'cargo'), ''), ''),
    coalesce(new.email, ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'telefono'), ''), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'codigo_senescyt'), ''), '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

revoke execute on function private.crear_perfil_nuevo_usuario() from public, anon, authenticated;

