-- ============================================================
-- AminMart Kasir - Batasi sync_updated_at dari klien
-- Sync produk memakai last-write-wins berdasarkan sync_updated_at
-- (epoch ms) yang dikirim perangkat. Perangkat dengan jam maju, atau
-- klien yang mengirim nilai sangat besar, akan selalu menang konflik.
-- Nilai di masa depan dipotong ke jam server + 5 menit toleransi.
-- ============================================================

create or replace function public.clamp_product_sync_updated_at()
returns trigger language plpgsql as $$
declare
    max_allowed bigint := (extract(epoch from now()) * 1000)::bigint + 300000;
begin
    if new.sync_updated_at > max_allowed then
        new.sync_updated_at := max_allowed;
    end if;
    return new;
end $$;

create trigger products_clamp_sync_updated_at
    before insert or update on public.products
    for each row execute function public.clamp_product_sync_updated_at();
