-- Quien comparte una ficha no decide sobre la agenda de otra persona: se elimina la asignacion de visitas.
-- Aplicar DESPUES de 20261007300000_info_compartidas_por_sala.sql.
drop function if exists public.asignar_visita(uuid, uuid, bigint, text, text);
