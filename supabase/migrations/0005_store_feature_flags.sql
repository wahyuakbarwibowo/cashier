create table public.store_feature_flags (
    store_id uuid not null references public.stores(id) on delete cascade,
    feature_key text not null check (feature_key in ('cloud_product_sync', 'cloud_backup')),
    enabled boolean not null default false,
    expires_at timestamptz,
    updated_at timestamptz not null default now(),
    primary key (store_id, feature_key)
);

alter table public.store_feature_flags enable row level security;

create policy store_feature_flags_select on public.store_feature_flags
    for select to authenticated using (public.is_store_member(store_id));
create policy store_feature_flags_manage on public.store_feature_flags
    for all to authenticated
    using (public.is_super_admin())
    with check (public.is_super_admin());

create trigger store_feature_flags_set_updated_at
    before update on public.store_feature_flags
    for each row execute function public.set_updated_at();

create or replace function public.has_store_feature(target_store uuid, target_feature text)
returns boolean language sql stable security definer set search_path = public as $$
    select public.is_store_member(target_store) and exists (
        select 1 from public.store_feature_flags f
        where f.store_id = target_store
          and f.feature_key = target_feature
          and f.enabled
          and (f.expires_at is null or f.expires_at > now())
    )
$$;

drop policy products_select on public.products;
drop policy products_insert on public.products;
drop policy products_update on public.products;
create policy products_select on public.products for select to authenticated
    using (public.has_store_feature(store_id, 'cloud_product_sync'));
create policy products_insert on public.products for insert to authenticated
    with check (public.has_store_feature(store_id, 'cloud_product_sync'));
create policy products_update on public.products for update to authenticated
    using (public.has_store_feature(store_id, 'cloud_product_sync'))
    with check (public.has_store_feature(store_id, 'cloud_product_sync'));

drop policy store_backups_select on public.store_backups;
drop policy store_backups_insert on public.store_backups;
drop policy store_backups_update on public.store_backups;
create policy store_backups_select on public.store_backups for select to authenticated
    using (public.has_store_feature(store_id, 'cloud_backup'));
create policy store_backups_insert on public.store_backups for insert to authenticated
    with check (public.has_store_feature(store_id, 'cloud_backup'));
create policy store_backups_update on public.store_backups for update to authenticated
    using (public.has_store_feature(store_id, 'cloud_backup'))
    with check (public.has_store_feature(store_id, 'cloud_backup'));
