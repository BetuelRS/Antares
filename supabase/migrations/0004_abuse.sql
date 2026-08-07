
alter table public.ai_requests add column if not exists ip_hash text;
alter table public.ai_requests add column if not exists tier text;

create index if not exists ai_requests_ip_idx
  on public.ai_requests (ip_hash, created_at)
  where tier = 'trial';

create index if not exists ai_requests_tier_idx
  on public.ai_requests (tier, status, created_at);

create index if not exists ai_requests_created_idx
  on public.ai_requests (created_at);

create or replace function public.ai_requests_prune(p_days int default 7)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  n integer;
begin
  delete from public.ai_requests
  where created_at < now() - (p_days || ' days')::interval;
  get diagnostics n = row_count;
  return n;
end;
$$;

revoke execute on function public.ai_requests_prune(int) from anon, authenticated;
