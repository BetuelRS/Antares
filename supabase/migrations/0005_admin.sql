
create table if not exists public.ai_admin (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  unlimited  boolean not null default false,
  updated_at bigint  not null default (extract(epoch from now()) * 1000)::bigint
);
alter table public.ai_admin enable row level security;

drop policy if exists ai_admin_select_own on public.ai_admin;
create policy ai_admin_select_own on public.ai_admin
  for select using (auth.uid() = user_id);
