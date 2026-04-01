create table if not exists public.dashboard_layouts (
  id             text        primary key,
  user_id        uuid        not null references auth.users(id) on delete cascade,
  layout_json    text        not null,
  generated_at   bigint      not null,
  expires_at     bigint      not null,
  schema_version integer     not null default 1
);

create index if not exists dashboard_layouts_user_generated_at_idx
  on public.dashboard_layouts (user_id, generated_at desc);

alter table public.dashboard_layouts enable row level security;

create policy "Users can manage their own layouts"
  on public.dashboard_layouts
  for all
  using  (auth.uid() = user_id)
  with check (auth.uid() = user_id);
