-- ============================================================
-- Jalankan SEKALI di Supabase SQL Editor SETELAH:
--  1. Skema 0001_init.sql sudah jalan
--  2. Anda sudah mendaftar lewat halaman login aplikasi Android
--     (pakai email yang akan jadi super admin)
--
-- Ganti email di bawah dengan email Anda, lalu jalankan.
-- ============================================================

update public.profiles
set role = 'super_admin', store_id = null
where id = (select id from auth.users where email = 'GANTI-EMAIL-ANDA@example.com');
