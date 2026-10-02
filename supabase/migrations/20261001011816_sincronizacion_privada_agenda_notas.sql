-- Datos personales de trabajo: solo la cuenta propietaria puede consultarlos.
-- Las notas históricas sin autor identificado no se migran a estas tablas.
create table if not exists public.agenda_privada (
  id uuid primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  organizacion_id uuid not null,
  version bigint not null default 1 check (version > 0),
  payload jsonb not null,
  actualizado_en bigint not null,
  eliminado_en bigint,
  created_at timestamptz not null default now()
);

create table if not exists public.notas_privadas (
  id uuid primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  organizacion_id uuid not null,
  version bigint not null default 1 check (version > 0),
  payload jsonb not null,
  actualizado_en bigint not null,
  eliminado_en bigint,
  created_at timestamptz not null default now()
);

create index if not exists agenda_privada_owner_org_idx
  on public.agenda_privada(owner_id, organizacion_id);
create index if not exists notas_privadas_owner_org_idx
  on public.notas_privadas(owner_id, organizacion_id);

alter table public.agenda_privada enable row level security;
alter table public.notas_privadas enable row level security;

revoke all on public.agenda_privada from anon;
revoke all on public.notas_privadas from anon;
grant select, insert, update on public.agenda_privada to authenticated;
grant select, insert, update on public.notas_privadas to authenticated;

create policy agenda_privada_leer on public.agenda_privada
  for select to authenticated using (owner_id = (select auth.uid()));
create policy agenda_privada_crear on public.agenda_privada
  for insert to authenticated with check (owner_id = (select auth.uid()));
create policy agenda_privada_editar on public.agenda_privada
  for update to authenticated
  using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));

create policy notas_privadas_leer on public.notas_privadas
  for select to authenticated using (owner_id = (select auth.uid()));
create policy notas_privadas_crear on public.notas_privadas
  for insert to authenticated with check (owner_id = (select auth.uid()));
create policy notas_privadas_editar on public.notas_privadas
  for update to authenticated
  using (owner_id = (select auth.uid()))
  with check (owner_id = (select auth.uid()));
