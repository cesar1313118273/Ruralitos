-- Simbolos y textos que se colocan sobre el mapa del croquis de la vivienda (iglesia, escuela, parque, etc.).
-- La columna guarda una lista en texto JSON, p. ej. [{"id":"...","tipo":"SIMBOLO","codigo":"IGLESIA","texto":"Iglesia","lat":-0.22,"lon":-78.51}].
-- Aplicar ANTES de instalar la version de la app que la sincroniza.
alter table public.fichas_familiares
    add column if not exists croquis_elementos_json text not null default '[]';
