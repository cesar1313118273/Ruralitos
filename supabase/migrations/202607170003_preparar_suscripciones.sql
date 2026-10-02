-- Preparacion comercial sin cobros ni restricciones durante la etapa de prueba.

alter table public.organizaciones
  add column plan_codigo text not null default 'PRUEBA',
  add column suscripcion_estado text not null default 'PRUEBA',
  add column periodo_actual_hasta timestamptz,
  add column gracia_hasta timestamptz,
  add column pagos_habilitados boolean not null default false;

alter table public.organizaciones
  add constraint organizaciones_plan_codigo_valido
    check (plan_codigo in ('PRUEBA', 'BASICO', 'PROFESIONAL', 'INSTITUCIONAL')),
  add constraint organizaciones_suscripcion_estado_valido
    check (suscripcion_estado in ('PRUEBA', 'ACTIVA', 'VENCIDA', 'PAUSADA', 'CANCELADA'));

comment on column public.organizaciones.pagos_habilitados is
  'Permanece false durante las pruebas. No se usa para bloquear funciones.';
