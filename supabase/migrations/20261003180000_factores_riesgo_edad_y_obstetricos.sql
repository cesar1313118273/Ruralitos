-- Factores de riesgo por grupo de edad (integrantes) y criterios de la escala de riesgo obstetrico (embarazadas).
-- Cada columna guarda una lista de codigos en texto, p. ej. ["SEDENTARISMO","VIOLENCIA"].
-- Aplicar ANTES de instalar la version de la app que los sincroniza.
alter table public.miembros_familia
    add column if not exists factores_riesgo_edad_json text not null default '[]';

alter table public.embarazadas
    add column if not exists factores_obstetricos_json text not null default '[]';
