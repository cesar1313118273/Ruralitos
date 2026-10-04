-- Quien recibe una ficha (aunque pueda editarla) no puede eliminarla de la nube: eso es solo de su autor.
-- Si alguien que no es el autor intenta marcarla como eliminada, el cambio se ignora sin dar error
-- (asi las bajas viejas que queden en un telefono no producen errores eternos).
-- Aplicar DESPUES de 20261007100000_avisos_y_traspaso_de_fichas.sql.
create or replace function private.proteger_baja_de_ficha()
returns trigger
language plpgsql
set search_path = ''
as $function$
begin
  if new.deleted_at is not null
     and old.deleted_at is null
     and old.creado_por is not null
     and (select auth.uid()) is not null
     and (select auth.uid()) is distinct from old.creado_por then
    new.deleted_at := old.deleted_at;
  end if;
  return new;
end;
$function$;

drop trigger if exists fichas_proteger_baja on public.fichas_familiares;
create trigger fichas_proteger_baja
  before update on public.fichas_familiares
  for each row execute function private.proteger_baja_de_ficha();
