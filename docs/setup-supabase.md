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
   migrasi (`0001` sampai `0005`):

   ```bash
   supabase db push --dry-run
   supabase db push
   ```

   `db push` menjalankan migrasi yang belum tercatat di project. Jika migrasi
   `0001`–`0003` sebelumnya sudah ditempel lewat SQL Editor sesuai panduan lama,
   jangan push tanpa memeriksa riwayat migrasi dan kondisi schema. Untuk
   mengaktifkan sync produk saja, jalankan `supabase/migrations/0004_products.sql`
   dan `0005_store_feature_flags.sql` di SQL Editor. Perubahan remote melalui SQL Editor tidak otomatis tercatat
   oleh CLI; selaraskan migration history sebelum memakai `db push` berikutnya.

5. Buka **Connect** atau **Project Settings > API Keys**, lalu salin Project URL
   dan publishable key (atau anon key lama). Salin template dan isi nilainya:

   ```bash
   cp supabase.properties.example supabase.properties
   ```

   Isi `SUPABASE_URL` dan `SUPABASE_ANON_KEY` di file tersebut. Nama property
   kedua tetap `SUPABASE_ANON_KEY`; publishable key dapat ditempatkan di sana.
   File ini diabaikan Git. Jangan gunakan secret/service-role key di aplikasi.

Data kasir lokal disimpan dalam file Room terpisah berdasarkan `store_id` akun.
Saat pertama kali membuka versi baru jika ada database lama, aplikasi meminta
Anda memilih: salin data lama ke toko yang sedang login, atau mulai toko ini
dengan data kosong. File lama tetap disimpan; pilih toko pemilik data dengan
benar sebelum menyalinnya. Pilihan kosong untuk satu toko tidak menghapus data
lama dan toko lain masih dapat mengklaimnya.

## 2. Siapkan akun dan toko

Build dan buka aplikasi, lalu daftarkan akun. Setelah profil dibuat, jalankan
`supabase/promote_super_admin.sql` di SQL Editor setelah mengganti alamat email
contoh dengan email akun. Login kembali sebagai super admin dan buat toko.
Sinkronisasi produk memerlukan akun yang memiliki `store_id`.

## 3. Uji sinkronisasi produk

1. Tambahkan atau edit produk di layar Produk.
2. Tekan ikon cloud untuk mengirim perubahan lokal dan mengambil perubahan cloud.
3. Login pada perangkat kedua ke toko yang sama dan tekan ikon cloud di sana.
4. Setelah login, coba ubah produk offline lalu sambungkan
   kembali internet. WorkManager akan mengirimnya otomatis; lihat waktu sync
   terakhir di layar Produk. Ikon cloud tetap dapat dipakai untuk sync manual.

Room tetap menjadi penyimpanan lokal, sehingga transaksi dapat berjalan saat
offline. Sync produk berjalan asynchronous setelah login. Sinkronisasi
transaksi, pelanggan, dan entitas selain produk belum tersedia.

## 4. Aktifkan entitlement Pro

`cloud_product_sync` dan `cloud_backup` adalah flag berbayar per toko. Flag baru
nonaktif secara default. Setelah migrasi `0005_store_feature_flags.sql` diterapkan,
login sebagai `super_admin`, buka **Kelola Tenant**, lalu tekan ikon bintang pada
toko. Aktifkan fitur yang dibeli dan isi tanggal akhir dalam format `YYYY-MM-DD`
(WIB). Kosongkan tanggal jika akses tidak dibatasi waktu; matikan switch untuk
mencabut akses. Admin toko tidak dapat mengubah flag paketnya sendiri.

Supabase RLS memeriksa flag dan masa berlaku pada setiap akses cloud. Editor ini
adalah aktivasi manual oleh super admin; payment gateway belum terhubung.

## 5. Deploy Edge Function

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

## 6. Build aplikasi

Setelah URL dan anon key terisi, build dan pasang aplikasi:

```bash
make build
make debug
```

Kelola toko hanya tersedia untuk `super_admin`; kelola pegawai tersedia untuk
`admin` dan `super_admin`. Database kasir tetap lokal, sementara backup cloud
tersedia dari menu **Backup & Restore**.
