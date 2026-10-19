-- Cloud backup per store. RLS keeps each store's operational data isolated.
create table public.store_backups (
    store_id uuid primary key references public.stores(id) on delete cascade,
    data jsonb not null,
    updated_at timestamptz not null default now()
);

alter table public.store_backups enable row level security;

create policy store_backups_select on public.store_backups for select to authenticated
    using (public.is_store_member(store_id));

create policy store_backups_insert on public.store_backups for insert to authenticated
    with check (public.is_store_member(store_id));

create policy store_backups_update on public.store_backups for update to authenticated
    using (public.is_store_member(store_id))
    with check (public.is_store_member(store_id));

create trigger store_backups_set_updated_at
    before update on public.store_backups
    for each row execute function public.set_updated_at();
