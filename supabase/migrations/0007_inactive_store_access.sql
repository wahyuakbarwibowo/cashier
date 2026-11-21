-- ============================================================
-- AminMart Kasir - Toko nonaktif kehilangan akses
-- stores.is_active = false sebelumnya hanya label; anggota toko masih
-- bisa membaca/menulis products, backup, dan feature flag.
-- ============================================================

create or replace function public.is_store_member(target_store uuid)
returns boolean language sql stable security definer set search_path = public as $$
    select exists(
        select 1 from public.profiles p
        join public.stores s on s.id = p.store_id
        where p.id = auth.uid() and p.store_id = target_store
          and p.is_approved and p.is_active and s.is_active
    )
$$;
