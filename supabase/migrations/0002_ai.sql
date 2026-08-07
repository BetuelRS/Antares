
create table if not exists public.ai_profile (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  trial_used int  not null default 0,
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);
alter table public.ai_profile enable row level security;

drop policy if exists ai_profile_select_own on public.ai_profile;
create policy ai_profile_select_own on public.ai_profile
  for select using (auth.uid() = user_id);

create table if not exists public.ai_usage (
  user_id uuid not null references auth.users(id) on delete cascade,
  day     date not null,
  count   int  not null default 0,
  primary key (user_id, day)
);
alter table public.ai_usage enable row level security;

drop policy if exists ai_usage_select_own on public.ai_usage;
create policy ai_usage_select_own on public.ai_usage
  for select using (auth.uid() = user_id);

create table if not exists public.ai_requests (
  id         bigserial primary key,
  user_id    uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  fn         text not null,
  status     text not null
);
alter table public.ai_requests enable row level security;

create index if not exists ai_requests_user_time_idx
  on public.ai_requests (user_id, created_at desc);

create table if not exists public.food_cache (
  key        text primary key,
  nutrition  jsonb not null,
  source     text  not null,
  hit_count  int   not null default 0,
  updated_at bigint not null
);
alter table public.food_cache enable row level security;

create or replace function public.ai_usage_increment(p_user uuid, p_day date, p_limit int)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_count int;
begin
  insert into public.ai_usage (user_id, day, count)
  values (p_user, p_day, 1)
  on conflict (user_id, day) do update
    set count = public.ai_usage.count + 1
    where public.ai_usage.count < p_limit
  returning count into v_count;

  if v_count is null then
    return -1;
  end if;
  return v_count;
end;
$$;

create or replace function public.ai_usage_decrement(p_user uuid, p_day date)
returns void
language sql
security definer
set search_path = public
as $$
  update public.ai_usage
     set count = greatest(count - 1, 0)
   where user_id = p_user and day = p_day;
$$;

create or replace function public.ai_trial_increment(p_user uuid, p_limit int)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_used int;
begin
  insert into public.ai_profile (user_id, trial_used)
  values (p_user, 1)
  on conflict (user_id) do update
    set trial_used = public.ai_profile.trial_used + 1,
        updated_at = (extract(epoch from now()) * 1000)::bigint
    where public.ai_profile.trial_used < p_limit
  returning trial_used into v_used;

  if v_used is null then
    return -1;
  end if;
  return v_used;
end;
$$;

create or replace function public.ai_trial_decrement(p_user uuid)
returns void
language sql
security definer
set search_path = public
as $$
  update public.ai_profile
     set trial_used = greatest(trial_used - 1, 0)
   where user_id = p_user;
$$;

create or replace function public.food_cache_hit(p_key text)
returns void
language sql
security definer
set search_path = public
as $$
  update public.food_cache set hit_count = hit_count + 1 where key = p_key;
$$;

revoke execute on function public.food_cache_hit(text) from anon, authenticated;
revoke execute on function public.ai_usage_increment(uuid, date, int) from anon, authenticated;
revoke execute on function public.ai_usage_decrement(uuid, date) from anon, authenticated;
revoke execute on function public.ai_trial_increment(uuid, int) from anon, authenticated;
revoke execute on function public.ai_trial_decrement(uuid) from anon, authenticated;

