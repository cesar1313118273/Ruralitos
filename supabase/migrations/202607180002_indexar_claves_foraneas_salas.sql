-- Índices de soporte para claves foráneas y crecimiento multi-Sala.

create index if not exists eais_creado_por_idx
  on public.eais(creado_por);

create index if not exists territorios_eais_org_fk_idx
  on public.territorios(eais_id, organizacion_id);
create index if not exists territorios_creado_por_idx
  on public.territorios(creado_por);

create index if not exists accesos_sala_eais_org_fk_idx
  on public.accesos_sala(eais_id, organizacion_id);
create index if not exists accesos_sala_territorio_org_fk_idx
  on public.accesos_sala(territorio_id, organizacion_id);
create index if not exists accesos_sala_creado_por_idx
  on public.accesos_sala(creado_por);

create index if not exists fichas_eais_org_fk_idx
  on public.fichas_familiares(eais_id, organizacion_id);
create index if not exists fichas_territorio_org_fk_idx
  on public.fichas_familiares(territorio_id, organizacion_id);

create index if not exists invitaciones_eais_org_fk_idx
  on public.invitaciones_organizacion(eais_id, organizacion_id);
create index if not exists invitaciones_territorio_org_fk_idx
  on public.invitaciones_organizacion(territorio_id, organizacion_id);