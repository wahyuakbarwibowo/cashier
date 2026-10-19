create table public.products (
    remote_id uuid primary key,
    store_id uuid not null references public.stores(id) on delete cascade,
    code text,
    name text not null,
    purchase_price double precision not null default 0,
    purchase_package_price double precision not null default 0,
    purchase_package_qty integer not null default 0,
    selling_price double precision not null default 0,
    package_price double precision not null default 0,
    package_qty integer not null default 0,
    discount double precision not null default 0,
    stock integer not null default 0,
    low_stock_threshold integer not null default 5,
    created_at text,
    updated_at text,
    sync_updated_at bigint not null,
    is_deleted boolean not null default false
);

create index products_store_id_idx on public.products (store_id);
alter table public.products enable row level security;

create policy products_select on public.products for select to authenticated
    using (public.is_store_member(store_id));
create policy products_insert on public.products for insert to authenticated
    with check (public.is_store_member(store_id));
create policy products_update on public.products for update to authenticated
    using (public.is_store_member(store_id))
    with check (public.is_store_member(store_id));
