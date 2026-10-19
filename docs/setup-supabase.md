# Setup Supabase untuk Aplikasi AminMart

Panduan menyiapkan autentikasi, manajemen admin Android, backup cloud, dan
sinkronisasi produk. Folder `supabase/` dan `config.toml` sudah ada di repository,
jadi `supabase init` tidak perlu dijalankan lagi.

## 1. Buat atau pilih project cloud

1. Buat project di [Supabase Dashboard](https://supabase.com/dashboard), atau
   gunakan project yang sudah menyimpan akun dan toko aplikasi. Project baru
   tidak memiliki akun atau data dari project lama.
2. Catat **Project Ref** dari URL Dashboard atau Project Settings.
3. Dari root repository, login dan tautkan CLI:

   ```bash
   supabase login
   supabase link --project-ref PROJECT_REF
   ```

4. Untuk database project baru yang masih kosong, periksa lalu terapkan semua
   migrasi (`0001` sampai `0004`):

   ```bash
   supabase db push --dry-run
   supabase db push
   ```

   `db push` menjalankan migrasi yang belum tercatat di project. Jika migrasi
   `0001`–`0003` sebelumnya sudah ditempel lewat SQL Editor sesuai panduan lama,
   jangan push tanpa memeriksa riwayat migrasi dan kondisi schema. Untuk
   mengaktifkan sync produk saja, jalankan `supabase/migrations/0004_products.sql`
   di SQL Editor. Perubahan remote melalui SQL Editor tidak otomatis tercatat
   oleh CLI; selaraskan migration history sebelum memakai `db push` berikutnya.

5. Buka **Connect** atau **Project Settings > API Keys**, lalu salin Project URL
   dan publishable key (atau anon key lama). Salin template dan isi nilainya:

   ```bash
   cp supabase.properties.example supabase.properties
   ```

   Isi `SUPABASE_URL` dan `SUPABASE_ANON_KEY` di file tersebut. Nama property
   kedua tetap `SUPABASE_ANON_KEY`; publishable key dapat ditempatkan di sana.
   File ini diabaikan Git. Jangan gunakan secret/service-role key di aplikasi.

## 2. Siapkan akun dan toko

Build dan buka aplikasi, lalu daftarkan akun. Setelah profil dibuat, jalankan
`supabase/promote_super_admin.sql` di SQL Editor setelah mengganti alamat email
contoh dengan email akun. Login kembali sebagai super admin dan buat toko.
Sinkronisasi produk memerlukan akun yang memiliki `store_id`.

## 3. Uji sinkronisasi produk

1. Tambahkan atau edit produk di layar Produk.
2. Tekan ikon cloud untuk mengirim perubahan lokal dan mengambil perubahan cloud.
3. Login pada perangkat kedua ke toko yang sama dan tekan ikon cloud di sana.
4. Coba juga ubah produk offline, sambungkan kembali internet, lalu tekan sync.

Room tetap menjadi penyimpanan lokal, sehingga transaksi dapat berjalan saat
offline. Sync produk dipicu manual dan berjalan asynchronous. Sinkronisasi
transaksi, pelanggan, dan entitas selain produk belum tersedia.

## 4. Deploy Edge Function

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

## 5. Build aplikasi

Setelah URL dan anon key terisi, build dan pasang aplikasi:

```bash
make build
make debug
```

Kelola toko hanya tersedia untuk `super_admin`; kelola pegawai tersedia untuk
`admin` dan `super_admin`. Database kasir tetap lokal, sementara backup cloud
tersedia dari menu **Backup & Restore**.
