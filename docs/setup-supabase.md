# Setup Supabase + Web Admin (v2 SaaS)

Panduan menghubungkan aplikasi kasir Android dan web admin ke Supabase,
lalu men-deploy web admin ke Vercel.

## Arsitektur

```
+--------------------+        +---------------------------+
| Aplikasi Kasir     |  auth  |                           |
| (Android, offline- +------->+      Supabase             |
| first, Room lokal) |  sync  |  - Auth (email/password)  |
+--------------------+        |  - Postgres + RLS         |
                              +------------+--------------+
+--------------------+                     |
| Web Admin          |        auth+db       |
| (SvelteKit di      +<--------------------+
|  Vercel)           |
+--------------------+
```

- **Role**: `super_admin` (pemilik SaaS), `admin` (pemilik toko), `kasir`.
- **Multi-tenant**: setiap toko = 1 baris di `stores`; semua user & data
  nantinya terikat `store_id`, dipisahkan oleh Row Level Security (RLS).

## 1. Buat project Supabase

1. Daftar/masuk di [supabase.com](https://supabase.com) lalu **New project**.
2. Simpan database password (untuk keperluan lain, tidak dipakai app langsung).
3. Buka **Project Settings > API**, catat:
   - `Project URL`
   - `anon public` key
   - `service_role` key (**RAHASIA**)

## 2. Jalankan skema SQL

1. Buka **SQL Editor** di dashboard Supabase.
2. Jalankan file di `supabase/migrations/` berurutan berdasarkan nomor.
3. Migrasi membuat tabel akun/toko, kebijakan RLS, dan penyimpanan cadangan
   data kasir per toko.

## 3. Jadikan diri Anda super admin

1. Buka halaman login web admin (langkah 5) dan daftar/login dengan email
   Anda. Trigger otomatis membuat profil dengan role `kasir`.
2. Di SQL Editor jalankan `supabase/promote_super_admin.sql`
   (ganti email dulu). Selesai - akun Anda kini `super_admin`.

## 4. Konfigurasi aplikasi Android

Secrets TIDAK boleh masuk git. Aplikasi membaca dari `supabase.properties`
di root repo (sudah di-gitignore):

```bash
cp supabase.properties.example supabase.properties
# lalu isi SUPABASE_URL dan SUPABASE_ANON_KEY dari Project Settings > API
```

Build ulang (`make debug`) -> aplikasi akan meminta login.

## 5. Menjalankan web admin secara lokal

```bash
cd admin-web
cp .env.example .env.local   # isi nilai asli dari Supabase
bun install
bun run dev                  # http://localhost:5173
```

## 6. Deploy web admin ke Vercel

1. Push repo ini ke GitHub.
2. Di [vercel.com](https://vercel.com): **Add New > Project > Import**
   repo ini, set **Root Directory** = `admin-web`.
3. Framework Preset terdeteksi otomatis (SvelteKit / adapter-vercel).
4. Tambahkan Environment Variables (Production + Preview):
   - `PUBLIC_SUPABASE_URL`
   - `PUBLIC_SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY` (rahasia; hanya server)
5. Deploy. Vercel juga bisa menambahkan domain gratis (`*.vercel.app`).

> Catatan keamanan: `SUPABASE_SERVICE_ROLE_KEY` hanya pernah dipakai di
> server route `/api/users` (membuat/menghapus user auth). Nilainya tidak
> pernah dikirim ke browser karena tidak berprefix `PUBLIC_`.

## 7. Alur kerja harian

| Aktor | Dari mana | Bisa apa |
|---|---|---|
| Super Admin (Anda) | Web admin | Kelola semua toko & semua user (buat admin/kasir, ubah role, nonaktifkan/hapus) |
| Admin toko | Web admin | Kelola kasir di tokonya sendiri |
| Kasir/Admin | App Android | Login, transaksi kasir (data lokal Room; sinkronisasi cloud menyusul) |

## Langkah lanjutan (fase berikutnya)

- Sinkronisasi data transaksi/produk Room <-> Supabase (offline-first).
- Reset password via Supabase Auth email.
- Audit log aktivitas user.
