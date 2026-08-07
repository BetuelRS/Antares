
do $$
declare
  t text;
  tbls text[] := array['goal_history'];
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
