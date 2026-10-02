-- Datos estructurados para la dispensarizacion automatica individual.
-- Se agregan a la tabla existente para conservar sus grants y politicas RLS.
alter table public.miembros_familia
    add column if not exists estado_nutricional text not null default '',
    add column if not exists hipertension_arterial boolean,
    add column if not exists diabetes_mellitus boolean,
    add column if not exists tuberculosis boolean,
    add column if not exists problema_salud_mental boolean,
    add column if not exists consumo_alcohol_drogas boolean,
    add column if not exists enfermedad_cronica boolean,
    add column if not exists discapacidad_visual boolean,
    add column if not exists discapacidad_auditiva boolean,
    add column if not exists discapacidad_lenguaje boolean,
    add column if not exists discapacidad_fisica boolean,
    add column if not exists discapacidad_intelectual boolean;

alter table public.miembros_familia
    drop constraint if exists miembros_familia_estado_nutricional_check;

alter table public.miembros_familia
    add constraint miembros_familia_estado_nutricional_check
    check (
        estado_nutricional in (
            '',
            'SIN_ALTERACION',
            'OBESIDAD',
            'DESNUTRICION_AGUDA',
            'DESNUTRICION_CRONICA'
        )
    );

comment on column public.miembros_familia.estado_nutricional is
    'Estado registrado por el profesional; no sustituye una evaluacion antropometrica.';
