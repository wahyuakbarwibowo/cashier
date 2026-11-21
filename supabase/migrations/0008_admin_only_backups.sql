-- ============================================================
-- AminMart Kasir - Backup cloud hanya untuk admin toko
-- Satu toko hanya punya satu baris backup (store_id = primary key),
-- jadi kasir yang bisa menulis bisa menimpa satu-satunya backup, dan
-- kasir yang bisa membaca bisa melihat seluruh data pelanggan & piutang.
-- ============================================================

create or replace function public.is_store_admin(target_store uuid)
returns boolean language sql stable security definer set search_path = public as $$
    select public.is_store_member(target_store) and exists(
        select 1 from public.profiles where id = auth.uid() and role = 'admin'
    )
$$;

drop policy store_backups_select on public.store_backups;
drop policy store_backups_insert on public.store_backups;
drop policy store_backups_update on public.store_backups;
create policy store_backups_select on public.store_backups for select to authenticated
    using (public.is_store_admin(store_id) and public.has_store_feature(store_id, 'cloud_backup'));
create policy store_backups_insert on public.store_backups for insert to authenticated
    with check (public.is_store_admin(store_id) and public.has_store_feature(store_id, 'cloud_backup'));
create policy store_backups_update on public.store_backups for update to authenticated
    using (public.is_store_admin(store_id) and public.has_store_feature(store_id, 'cloud_backup'))
    with check (public.is_store_admin(store_id) and public.has_store_feature(store_id, 'cloud_backup'));
