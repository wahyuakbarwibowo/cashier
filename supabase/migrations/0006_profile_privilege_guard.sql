-- ============================================================
-- AminMart Kasir - Kunci kolom hak akses di profiles
-- Policy profiles_update membolehkan user mengubah barisnya sendiri
-- (untuk ganti nama), tapi tidak membatasi kolom. Tanpa guard ini,
-- kasir bisa PATCH dirinya jadi admin atau mengaktifkan lagi akun
-- yang sudah dinonaktifkan.
-- ============================================================

-- role, is_active, store_id, dan email hanya boleh diubah oleh:
--   - service role / SQL editor (auth.uid() null)
--   - super admin
--   - admin toko yang sudah disetujui & aktif, untuk user LAIN di tokonya
create or replace function public.guard_profile_privileges()
returns trigger language plpgsql security definer set search_path = public as $$
declare
    me public.profiles;
begin
    if auth.uid() is null
        or (new.role is not distinct from old.role
            and new.is_active is not distinct from old.is_active
            and new.store_id is not distinct from old.store_id
            and new.email is not distinct from old.email) then
        return new;
    end if;
    select * into me from public.profiles where id = auth.uid();
    if me.role = 'super_admin' then
        return new;
    end if;
    if me.role = 'admin' and me.is_approved and me.is_active
        and new.id <> me.id
        and me.store_id is not null
        and old.store_id = me.store_id
        and new.store_id = me.store_id
        and new.role <> 'super_admin'
        and new.email is not distinct from old.email then
        return new;
    end if;
    raise exception 'Tidak berhak mengubah role, status, toko, atau email akun ini';
end $$;

create trigger profiles_guard_privileges
    before update on public.profiles
    for each row execute function public.guard_profile_privileges();
