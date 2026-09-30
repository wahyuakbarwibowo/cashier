-- ============================================================
-- AminMart Kasir - Persetujuan akun
-- Akun hasil daftar mandiri berstatus pending (is_approved = false)
-- sampai disetujui super admin, atau admin toko untuk kasir di tokonya.
-- ============================================================

alter table public.profiles add column is_approved boolean not null default false;

-- Akun yang sudah ada dianggap sudah disetujui
update public.profiles set is_approved = true;

-- Akun yang dibuat lewat service role (web admin) boleh langsung disetujui
-- dengan app_metadata { "approved": true }; app_metadata tidak bisa diisi
-- oleh klien saat signup, jadi aman.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
    meta jsonb := new.raw_user_meta_data;
    requested_role public.user_role;
    valid_store uuid;
begin
    if coalesce(meta->>'role', '') in ('super_admin', 'admin') then
        if exists (
            select 1 from public.profiles p
            where p.id = auth.uid() and p.role = 'super_admin'
        ) then
            requested_role := (meta->>'role')::public.user_role;
        else
            requested_role := 'kasir';
        end if;
    else
        requested_role := 'kasir';
    end if;

    if coalesce(meta->>'store_id', '') <> '' then
        select s.id into valid_store from public.stores s where s.id = (meta->>'store_id')::uuid;
    end if;

    insert into public.profiles (id, email, full_name, role, store_id, is_approved)
    values (
        new.id,
        new.email,
        coalesce(meta->>'full_name', ''),
        requested_role,
        valid_store,
        coalesce((new.raw_app_meta_data->>'approved')::boolean, false)
    );
    return new;
end $$;

-- Hanya super admin, atau admin toko untuk kasir di tokonya, yang boleh
-- mengubah is_approved. auth.uid() null = service role / SQL editor.
create or replace function public.guard_profile_approval()
returns trigger language plpgsql security definer set search_path = public as $$
declare
    me public.profiles;
begin
    if auth.uid() is null or new.is_approved is not distinct from old.is_approved then
        return new;
    end if;
    select * into me from public.profiles where id = auth.uid();
    if me.role = 'super_admin' then
        return new;
    end if;
    if me.role = 'admin' and me.is_approved and new.role = 'kasir'
        and new.store_id is not null and new.store_id = me.store_id then
        return new;
    end if;
    raise exception 'Tidak berhak menyetujui akun ini';
end $$;

create trigger profiles_guard_approval
    before update on public.profiles
    for each row execute function public.guard_profile_approval();

-- Akun pending belum dianggap anggota toko
create or replace function public.is_store_member(target_store uuid)
returns boolean language sql stable security definer set search_path = public as $$
    select exists(
        select 1 from public.profiles
        where id = auth.uid() and store_id = target_store and store_id is not null
          and is_approved and is_active
    )
$$;
