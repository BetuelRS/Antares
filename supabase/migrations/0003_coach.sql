
create table if not exists public.coach_report (
  id         text        not null,
  user_id    uuid        not null default auth.uid(),
  updated_at bigint      not null,
  deleted    boolean     not null default false,
  data       jsonb       not null,
  primary key (user_id, id)
);

create index if not exists coach_report_user_updated_idx
  on public.coach_report (user_id, updated_at);

alter table public.coach_report enable row level security;

drop policy if exists coach_report_sel on public.coach_report;
create policy coach_report_sel on public.coach_report
  for select using (user_id = auth.uid());

drop policy if exists coach_report_ins on public.coach_report;
create policy coach_report_ins on public.coach_report
  for insert with check (user_id = auth.uid());

drop policy if exists coach_report_upd on public.coach_report;
create policy coach_report_upd on public.coach_report
  for update using (user_id = auth.uid()) with check (user_id = auth.uid());
