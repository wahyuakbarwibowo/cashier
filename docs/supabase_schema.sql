-- Skema Postgres untuk Supabase — lihat docs/SUPABASE_MIGRATION.md untuk rencana lengkap.
-- Status: draft, belum dijalankan di project manapun.
--
-- Konvensi:
--   - PK semua tabel: uuid (default gen_random_uuid()), ini yang jadi `remote_id` di Room.
--   - Tiap tabel punya shop_id -> scoping multi-toko, dipakai RLS.
--   - updated_at dipakai untuk last-write-wins saat pull ke Room.
--   - FK antar tabel pakai remote_id (uuid), bukan id lokal (Long) di Room.

create extension if not exists pgcrypto; -- gen_random_uuid()

-- ============================================================
-- Master: shops (1 baris = 1 toko, terhubung ke 1 auth.users pemilik)
-- ============================================================
create table shops (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    name text,
    footer_note text,
    cashier_name text,
    phone_number text,
    address text,
    poin_enabled boolean not null default false,
    logo_path text,
    updated_at timestamptz not null default now()
);

create index on shops (owner_id);

-- ============================================================
-- Master data kecil
-- ============================================================
create table payment_methods (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    name text not null,
    sort_order int not null default 0,
    updated_at timestamptz not null default now(),
    is_synced boolean not null default true -- selalu true di server, kolom disamakan dgn Room untuk konsistensi payload
);

create table digital_categories (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    name text not null,
    icon text,
    sort_order int not null default 0,
    updated_at timestamptz not null default now()
);

create table suppliers (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    name text not null,
    phone text,
    address text,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create table customers (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    name text not null,
    phone text,
    address text,
    points int not null default 0,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create index on customers (shop_id);
create index on suppliers (shop_id);

-- ============================================================
-- Produk
-- ============================================================
create table products (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    code text,
    name text not null,
    purchase_price numeric not null default 0,
    purchase_package_price numeric not null default 0,
    purchase_package_qty int not null default 0,
    selling_price numeric not null default 0,
    package_price numeric not null default 0,
    package_qty int not null default 0,
    discount numeric not null default 0,
    stock int not null default 0,
    low_stock_threshold int not null default 5,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create index on products (shop_id);
create index on products (shop_id, code);

create table product_variants (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    product_id uuid not null references products(id) on delete cascade,
    name text not null,
    sku text,
    barcode text,
    purchase_price numeric not null default 0,
    selling_price numeric not null default 0,
    stock int not null default 0,
    updated_at timestamptz not null default now()
);

create index on product_variants (product_id);

-- ============================================================
-- Penjualan
-- ============================================================
create table sales (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    customer_id uuid references customers(id) on delete set null,
    payment_method_id uuid references payment_methods(id) on delete set null,
    total numeric not null default 0,
    paid numeric not null default 0,
    change numeric not null default 0,
    points_earned int not null default 0,
    points_redeemed int not null default 0,
    profit numeric not null default 0,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create index on sales (shop_id, created_at);

create table sale_items (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    sale_id uuid not null references sales(id) on delete cascade,
    product_id uuid references products(id) on delete set null,
    product_name text not null default '',
    variant_id uuid references product_variants(id) on delete set null,
    variant_name text,
    qty int not null default 0,
    price numeric not null default 0,
    cost_price numeric not null default 0,
    subtotal numeric not null default 0
    -- append-only: tidak butuh updated_at, dibuat sekali saat sale_id dibuat
);

create index on sale_items (sale_id);
create index on sale_items (product_id);

-- ============================================================
-- Piutang / Hutang
-- ============================================================
create table receivables (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    sale_id uuid references sales(id) on delete cascade,
    customer_id uuid references customers(id) on delete cascade,
    amount numeric not null default 0,
    paid_amount numeric not null default 0,
    due_date date,
    status text not null default 'pending',
    notes text,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create index on receivables (shop_id, status);

create table purchases (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    supplier_id uuid references suppliers(id) on delete set null,
    supplier text,
    total numeric not null default 0,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create table purchase_items (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    purchase_id uuid not null references purchases(id) on delete cascade,
    product_id uuid references products(id) on delete set null,
    product_name text not null default '',
    qty int not null default 0,
    price numeric not null default 0,
    subtotal numeric not null default 0
);

create index on purchase_items (purchase_id);

create table payables (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    purchase_id uuid references purchases(id) on delete cascade,
    supplier_id uuid references suppliers(id) on delete cascade,
    supplier text,
    amount numeric not null default 0,
    paid_amount numeric not null default 0,
    due_date date,
    status text not null default 'pending',
    notes text,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create index on payables (shop_id, status);

-- ============================================================
-- Poin, stok, shift, biaya (log/append-only kecuali dinyatakan lain)
-- ============================================================
create table customer_points_history (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    customer_id uuid not null references customers(id) on delete cascade,
    sale_id uuid references sales(id) on delete set null,
    points int not null default 0,
    type text not null, -- EARNED, REDEEMED, ADJUSTMENT
    notes text,
    created_at timestamptz
);

create index on customer_points_history (customer_id);

create table stock_history (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    product_id uuid not null references products(id) on delete cascade,
    product_name text not null,
    change_qty int not null,
    stock_before int not null,
    stock_after int not null,
    reason text not null,
    created_at timestamptz not null
);

create index on stock_history (product_id, created_at);

create table expenses (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    category text not null,
    amount numeric not null default 0,
    notes text,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create table shifts (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    opened_at timestamptz not null,
    closed_at timestamptz,
    opening_cash numeric not null default 0,
    counted_cash numeric,
    total_sales numeric not null default 0,
    total_expenses numeric not null default 0,
    expected_cash numeric,
    difference numeric,
    note text,
    status text not null default 'open', -- 'open' | 'closed'
    updated_at timestamptz not null default now()
);

create index on shifts (shop_id, status);

-- ============================================================
-- PPOB / digital
-- ============================================================
create table digital_products (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    category text not null,
    provider text not null,
    name text not null,
    nominal numeric not null default 0,
    cost_price numeric not null default 0,
    selling_price numeric not null default 0,
    sort_order int not null default 0,
    created_at timestamptz,
    updated_at timestamptz not null default now()
);

create table phone_history (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references shops(id) on delete cascade,
    category text not null default 'PULSA',
    phone_number text,
    customer_name text,
    provider text,
    sender_name text,
    receiver_name text,
    amount numeric not null default 0,
    cost_price numeric not null default 0,
    selling_price numeric not null default 0,
    profit numeric not null default 0,
    notes text,
    paid numeric not null default 0,
    created_at timestamptz not null default now()
);

create index on phone_history (shop_id, created_at);

-- ============================================================
-- Row Level Security — tiap toko hanya lihat/ubah datanya sendiri
-- ============================================================
-- security definer + stable: bypass RLS pada tabel shops saat dicek dari policy
-- tabel lain (hindari recursive RLS check), dan boleh di-cache per statement.
create or replace function is_shop_owner(check_shop_id uuid)
returns boolean
language sql
security definer
stable
as $$
    select exists (
        select 1 from shops
        where id = check_shop_id
        and owner_id = auth.uid()
    );
$$;

alter table shops enable row level security;
create policy shops_owner on shops
    using (owner_id = auth.uid())
    with check (owner_id = auth.uid());

-- policy generik per tabel via is_shop_owner(shop_id), diulang manual per tabel
-- karena Postgres tidak punya "apply to all tables" bawaan.
do $$
declare
    t text;
begin
    foreach t in array array[
        'payment_methods', 'digital_categories', 'suppliers', 'customers',
        'products', 'product_variants', 'sales', 'sale_items', 'receivables',
        'purchases', 'purchase_items', 'payables', 'customer_points_history',
        'stock_history', 'expenses', 'shifts', 'digital_products', 'phone_history'
    ]
    loop
        execute format('alter table %I enable row level security', t);
        execute format(
            'create policy %I_shop_owner on %I
                using (is_shop_owner(shop_id))
                with check (is_shop_owner(shop_id))',
            t, t
        );
    end loop;
end $$;
