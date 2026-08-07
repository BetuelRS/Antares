
insert into public.entitlements (user_id, status, product_id, expires_at, updated_at)
select
  id,
  'ACTIVE',
  'manual_grant',
  null,
  (extract(epoch from now()) * 1000)::bigint
from auth.users
order by created_at desc
limit 1
on conflict (user_id) do update
  set status     = 'ACTIVE',
      expires_at = null,
      updated_at = (extract(epoch from now()) * 1000)::bigint;

select user_id, status, product_id, expires_at from public.entitlements;

