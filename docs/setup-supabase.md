# Setup Supabase untuk Aplikasi AminMart

Panduan menyiapkan autentikasi, manajemen admin Android, backup cloud, dan
Edge Function penghapusan akun.

## 1. Buat project Supabase

1. Buat project di [supabase.com](https://supabase.com).
2. Buka **Project Settings > API** dan catat Project URL serta anon key.
3. Di **SQL Editor**, jalankan file migration sesuai urutan:
   `0001_init.sql`, `0002_account_approval.sql`, lalu `0003_store_backups.sql`.
4. Salin konfigurasi aplikasi dan isi URL serta anon key:

   ```bash
   cp supabase.properties.example supabase.properties
   ```

   `supabase.properties` tidak boleh dimasukkan ke Git.

## 2. Buat akun super admin

Daftar melalui aplikasi Android. Setelah profil dibuat, jalankan
`supabase/promote_super_admin.sql` di SQL Editor setelah mengganti alamat email
contoh dengan email akun Anda.

## 3. Deploy Edge Function

Fungsi `delete-user` memakai service-role key di sisi Supabase untuk menghapus
akun Auth. Key tersebut tidak disimpan dalam APK.

**Status saat ini:** source fungsi sudah ada di repo, tetapi belum di-deploy ke
project Supabase. Karena itu, hapus akun dari aplikasi belum aktif sampai
perintah deploy di bawah berhasil dijalankan.

```bash
supabase login
supabase link --project-ref PROJECT_REF
supabase functions deploy delete-user
```

Supabase menyediakan `SUPABASE_SERVICE_ROLE_KEY` untuk Edge Function. Jangan
menambahkan key itu ke `supabase.properties` atau source Android.

## 4. Build aplikasi

Setelah URL dan anon key terisi, build dan pasang aplikasi:

```bash
make build
make debug
```

Kelola toko hanya tersedia untuk `super_admin`; kelola pegawai tersedia untuk
`admin` dan `super_admin`. Database kasir tetap lokal, sementara backup cloud
tersedia dari menu **Backup & Restore**.
