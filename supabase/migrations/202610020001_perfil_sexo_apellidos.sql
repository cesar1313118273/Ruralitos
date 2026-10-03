-- Sexo y apellidos del profesional.
-- El sexo define la lista de cargos (Doctora/Doctor, Enfermera/Enfermero, ...) y el título del
-- saludo de la app (Dra./Dr.). Los apellidos permiten saludar con "primer nombre + primer apellido".
-- La app funciona aunque esta migración aún no se haya aplicado: guarda esos datos solo en el teléfono.

alter table public.perfiles
  add column if not exists sexo text not null default '',
  add column if not exists apellidos text not null default '';

alter table public.perfiles
  drop constraint if exists perfiles_sexo_valor;

alter table public.perfiles
  add constraint perfiles_sexo_valor check (sexo in ('', 'H', 'M'));

alter table public.perfiles
  drop constraint if exists perfiles_apellidos_longitud;

alter table public.perfiles
  add constraint perfiles_apellidos_longitud check (char_length(apellidos) <= 160);

-- El perfil nuevo toma también sexo y apellidos de los datos de registro.
create or replace function private.crear_perfil_nuevo_usuario()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.perfiles (
    id, cedula, nombres, cargo, correo, telefono, codigo_senescyt, sexo, apellidos
  )
  values (
    new.id,
    nullif(btrim(new.raw_user_meta_data ->> 'cedula'), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'nombres'), ''), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'cargo'), ''), ''),
    coalesce(new.email, ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'telefono'), ''), ''),
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'codigo_senescyt'), ''), ''),
    case when new.raw_user_meta_data ->> 'sexo' in ('H', 'M')
         then new.raw_user_meta_data ->> 'sexo' else '' end,
    coalesce(nullif(btrim(new.raw_user_meta_data ->> 'apellidos'), ''), '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

revoke execute on function private.crear_perfil_nuevo_usuario() from public, anon, authenticated;
