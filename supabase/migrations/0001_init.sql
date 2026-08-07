
do $$
declare
  t text;
  tbls text[] := array[
    'user_profile', 'weight_log', 'daily_target_override',
    'food', 'food_log', 'water_log',
    'recipe', 'recipe_ingredient',
    'exercise_log', 'exercise',
    'routine', 'routine_item', 'routine_schedule',
    'workout_session', 'workout_set',
    'fasting_protocol', 'fasting_session',
    'run'
  ];
begin
  foreach t in array tbls loop
    execute format($f$
      create table if not exists public.%1$I (
        id         text        not null,
        user_id    uuid        not null default auth.uid(),
        updated_at bigint      not null,
        deleted    boolean     not null default false,
        data       jsonb       not null,
        primary key (user_id, id)
      );
    $f$, t);

    execute format(
      'create index if not exists %1$I on public.%2$I (user_id, updated_at);',
      t || '_user_updated_idx', t
    );

    execute format('alter table public.%1$I enable row level security;', t);

    execute format('drop policy if exists %1$I on public.%2$I;', t || '_sel', t);
    execute format(
      'create policy %1$I on public.%2$I for select using (user_id = auth.uid());',
      t || '_sel', t
    );

    execute format('drop policy if exists %1$I on public.%2$I;', t || '_ins', t);
    execute format(
      'create policy %1$I on public.%2$I for insert with check (user_id = auth.uid());',
      t || '_ins', t
    );

    execute format('drop policy if exists %1$I on public.%2$I;', t || '_upd', t);
    execute format(
      'create policy %1$I on public.%2$I for update using (user_id = auth.uid()) with check (user_id = auth.uid());',
      t || '_upd', t
    );
  end loop;
end $$;

create table if not exists public.entitlements (
  user_id       uuid primary key,
  status        text not null,
  product_id    text,
  expires_at    bigint,
  rc_app_user_id text,
  updated_at    bigint not null
);
alter table public.entitlements enable row level security;

drop policy if exists entitlements_sel on public.entitlements;
create policy entitlements_sel on public.entitlements
  for select using (user_id = auth.uid());

create table if not exists public.purchase_events (
  id          bigint generated always as identity primary key,
  received_at timestamptz not null default now(),
  event       jsonb not null
);
alter table public.purchase_events enable row level security;

