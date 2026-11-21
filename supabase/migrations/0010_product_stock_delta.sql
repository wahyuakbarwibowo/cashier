-- ============================================================
-- AminMart Kasir - Sync stok produk berbasis delta
-- Dulu stok ikut last-write-wins bersama data produk, sehingga dua
-- perangkat yang berjualan offline saling menimpa stok. Sekarang
-- perangkat mengirim selisih stok lewat fungsi ini, dan server
-- menjumlahkannya secara atomik. Upsert produk tidak lagi mengirim
-- kolom stock (kecuali transisi sekali jalan dari versi lama).
-- Catatan: semua perangkat harus memakai versi app yang mendukung
-- delta; versi lama masih mengirim stok absolut dan akan menimpa.
-- ============================================================

-- security invoker: RLS products_update (is_store_member) tetap berlaku.
create or replace function public.add_product_stock(p_remote_id uuid, p_delta integer)
returns integer language sql security invoker set search_path = public as $$
    update public.products
    set stock = stock + p_delta
    where remote_id = p_remote_id
    returning stock
$$;

revoke execute on function public.add_product_stock(uuid, integer) from public, anon;
grant execute on function public.add_product_stock(uuid, integer) to authenticated;
