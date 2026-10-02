alter table public.miembros_familia
    add column if not exists comorbilidades_cie10_json text not null default '[]',
    add column if not exists porcentaje_discapacidad integer,
    add column if not exists necesita_ayuda_tecnica boolean,
    add column if not exists enfermedad_cronica_descompensada boolean,
    add column if not exists riesgo_genetico boolean,
    add column if not exists victima_violencia boolean,
    add column if not exists privado_libertad boolean;

alter table public.miembros_familia
    drop constraint if exists miembros_familia_porcentaje_discapacidad_check;
alter table public.miembros_familia
    add constraint miembros_familia_porcentaje_discapacidad_check
    check (porcentaje_discapacidad is null or porcentaje_discapacidad between 0 and 100);

alter table public.embarazadas
    add column if not exists riesgo_obstetrico text not null default '';

alter table public.embarazadas
    drop constraint if exists embarazadas_riesgo_obstetrico_check;
alter table public.embarazadas
    add constraint embarazadas_riesgo_obstetrico_check
    check (riesgo_obstetrico in ('', 'SIN_RIESGO', 'BAJO', 'ALTO', 'MUY_ALTO'));
