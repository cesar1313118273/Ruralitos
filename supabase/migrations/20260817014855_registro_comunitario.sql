-- Variables que completan el formato oficial Registro Comunitario.
-- La tabla ya está expuesta y protegida por las políticas RLS existentes;
-- esta migración solo amplía sus columnas, sin alterar privilegios ni políticas.
alter table public.miembros_familia
    add column if not exists discapacidad_psicosocial boolean,
    add column if not exists cuidados_paliativos boolean,
    add column if not exists vih boolean,
    add column if not exists evento_salud boolean,
    add column if not exists caso_confirmado boolean,
    add column if not exists caso_sospechoso_uno boolean,
    add column if not exists caso_sospechoso_dos boolean,
    add column if not exists prestador_comunitario boolean,
    add column if not exists partero_ancestral boolean,
    add column if not exists sabiduria_ancestral boolean;
