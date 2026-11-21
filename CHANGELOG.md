# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Versioning Convention

`versionName` di `app/build.gradle` mengikuti `MAJOR.MINOR.PATCH`:
- **MAJOR**: perubahan besar/breaking (redesign besar, migrasi arsitektur, dsb.)
- **MINOR**: fitur baru yang backward-compatible
- **PATCH**: bug fix saja, tanpa fitur baru

`versionCode` naik +1 di setiap rilis ke Play Store, terlepas dari besar-kecilnya perubahan `versionName`. Tag git pakai format `vMAJOR.MINOR.PATCH` (mis. `v2.0.0`), dibuat saat versi tersebut dirilis.

## [Unreleased]

## [3.8.0] - 2026-11-21

### Added
- Tukar poin pelanggan langsung di dialog pembayaran (1 poin = Rp100), dibatasi saldo poin dan sisa tagihan.
- Tandai metode pembayaran sebagai tunai/non-tunai di Pengaturan; kas shift hanya menghitung metode tunai.

### Changed
- Stok produk disinkronkan sebagai selisih (delta), sehingga penjualan offline di beberapa perangkat sama-sama mengurangi stok cloud. Semua perangkat toko perlu diperbarui ke versi ini.
- Password baru minimal 8 karakter dan berisi huruf serta angka.
- Link reset password memakai token yang baru diverifikasi saat tombol Simpan ditekan.
- APK rilis lebih kecil (12 MB → 4 MB) karena optimasi R8 diaktifkan.

### Fixed
- Histori piutang/hutang yang sudah lunas tidak lagi ikut terhapus saat pelanggan/supplier dihapus.
- Keranjang kasir tidak hilang saat Android menutup aplikasi di latar belakang.
- Pencarian dengan karakter % atau _ tidak lagi dianggap wildcard.
- Daftar produk langsung menampilkan produk yang baru ditambah atau diubah.
- Restore backup tidak mengirim ulang selisih stok yang sudah tersinkron.

## [3.7.1] - 2026-11-21

### Security
- Hanya admin/super admin yang bisa melihat dan menimpa backup cloud; menu Backup & Restore disembunyikan dari kasir.
- Role, status aktif, toko, dan email di profil hanya bisa diubah oleh admin toko yang sama atau super admin.
- Anggota toko yang dinonaktifkan kehilangan akses data tokonya.
- Token sesi dan pengaturan tidak lagi ikut backup Android; lalu lintas HTTP tanpa enkripsi dimatikan.
- Logout saat offline benar-benar menghapus sesi Supabase.
- Ekspor CSV aman dari formula injection; permission yang tidak dipakai dihapus.

### Fixed
- Stok dicek ulang di dalam transaksi checkout; simpan produk tidak lagi menimpa stok yang berubah di tempat lain.
- Harga paket dan diskon produk dihitung benar; total dibulatkan ke rupiah dan poin dihitung setelah diskon.
- Checkout dan simpan pembelian tidak bisa terkirim dua kali; error transaksi ditampilkan.
- Edit transaksi tidak lagi mengubah tanggal, mengurangi poin dua kali, atau menghapus cicilan piutang.
- Pembayaran piutang dan transaksi digital beserta piutangnya disimpan secara atomik.
- Pelanggan/supplier yang masih berhutang tidak bisa dihapus; poin pelanggan tidak bisa minus.
- Laba bersih tidak lagi memotong harga modal dua kali; kas shift dihitung dari uang tunai yang diterima.
- Cetak ulang struk memakai tanggal transaksi; gagal cetak dilaporkan.
- Stok menipis memakai ambang per produk; nama produk terlaris konsisten.
- Sync produk ditarik per halaman dan jam perangkat yang maju tidak lagi selalu menang konflik.
- Restore ditolak bila ada ID ganda; sel Excel kosong tidak lagi gagal di-restore.
- Keranjang tidak hilang saat ukuran font atau bahasa diubah; pencarian riwayat digital mencakup seluruh data.
- Aplikasi tidak logout saat server Supabase bermasalah; pesan error login lebih jelas.

### Changed
- Index tanggal di pengeluaran dan pembelian (migrasi DB 12 → 13) untuk mempercepat laporan.

## [3.7.0] - 2026-10-31

### Added
- Tombol "Lupa Password?" di halaman login; link di email membuka halaman web untuk membuat password baru.

## [3.6.0] - 2026-10-31

### Added
- Konfirmasi sebelum menghapus kategori/produk digital, metode pembayaran, dan pesanan tertahan.

### Fixed
- Tombol hapus kategori digital dan menu transaksi digital terlalu kecil untuk diketuk.

## [3.5.0] - 2026-10-31

### Changed
- Ukuran teks di seluruh aplikasi diperbesar 15% agar mudah dibaca pengguna lanjut usia.

## [3.4.0] - 2026-10-31

### Added
- Kolom "Ulangi Password" dan validasi format email di form daftar.

### Changed
- Form login/daftar lebih ramah pengguna lanjut usia: teks dan tombol lebih besar, bisa di-scroll saat keyboard muncul, keyboard email/password sesuai, tombol Enter langsung masuk.
- Pesan error pendaftaran dalam Bahasa Indonesia.

## [3.3.0] - 2026-10-30

### Added
- Tombol mata untuk menampilkan/menyembunyikan password di halaman login.

### Changed
- Halaman login memakai logo Aminmart.

## [3.2.0] - 2026-10-30

### Changed
- Ikon aplikasi diganti dengan logo Aminmart baru.

## [3.1.0] - 2026-09-30

### Added
- Daftar akun dari aplikasi; akun baru berstatus pending sampai disetujui
- Layar Kelola Pegawai: setujui/tolak pendaftar, tambah akun kasir, nonaktifkan pegawai
- Badge jumlah pendaftar yang menunggu persetujuan di menu Lainnya
- Layar Kelola Tenant untuk super admin (tambah, ubah, nonaktifkan, hapus toko)
- Migrasi Supabase `0002_account_approval.sql` (wajib dijalankan sebelum rilis)

### Fixed
- Tambah produk digital gagal tanpa keterangan; kini ada pesan kategori belum dipilih, field wajib, dan error penyimpanan

## [2.2.1] - 2026-08-23

### Fixed
- File backup `.json` tidak bisa dipilih saat impor karena filter MIME picker terlalu ketat

## [2.2.0] - 2026-08-23

### Added
- Filter periode (Semua/Hari Ini/7 Hari/Bulan Ini/Kustom) di Pengeluaran, Riwayat Stok, dan Laporan
- Total pengeluaran mengikuti periode yang dipilih
- Pemisah ribuan otomatis di semua input nominal rupiah
- Konfirmasi sebelum mengosongkan keranjang, dan aksi "Urungkan" saat item dihapus

### Changed
- Menu "Lainnya" dikelompokkan per kategori (Transaksi, Laporan, Data Master, Sistem)
- Notifikasi di layar kasir memakai Snackbar, bukan Toast

### Fixed
- Ringkasan di layar Laporan sebelumnya hanya menjumlah 20 baris pertama sehingga omzet tampil lebih kecil dari sebenarnya

## [2.0.1] - 2026-07-05

### Fixed
- Crash `IllegalStateException: A migration from 8 to 11 was required but not found` pada device yang masih di skema DB versi lama (1-8, pra-histori Migration object)

---

### Added
- Unit test setup with Mockito and Kotlin Coroutines Test
- `signing.properties.example` for release signing configuration guidance
- ProGuard rules for Room, Gson, and Kotlinx Coroutines

### Changed
- **Theme**: Updated color scheme to match Aminmart pink logo colors
  - Primary: `#E91E8B` (bright pink from cashier machine & logo text)
  - Secondary: `#800020` (dark maroon from silhouette & outlines)
- Moved `MainActivity.kt` and `MainApplication.kt` to consistent Kotlin folder structure
- Enabled R8 full mode with minification and resource shrinking for release builds
- Updated `colors.xml` with dark mode support in `values-night/colors.xml`

### Fixed
- Data import crash "Parameter specified as non-null is null" when importing older backup files with missing fields
- Color theme inconsistency between XML and Compose themes

---

## [1.0.0] - 2026-03-05

### Added
- Retail POS with barcode scanner
- PPOB & Digital Services (Pulsa, PLN, E-Wallet, BPJS, etc.)
- Profit & Loss reporting
- Debt & Receivables management
- Expense tracking
- Low stock alerts
- Package price calculation
- Thermal printer support (58mm Bluetooth)
- Transaction history with date filtering
- Backup & Restore database
- Edit digital transactions
- Backdate transactions
- Dynamic digital categories
- Enhanced UX with keyboard avoidance
- Indonesian date format throughout the app

### Changed
- Kotlin 2.3.10
- Jetpack Compose with Material 3
- Room Database 2.8.4
- Navigation Compose 2.8.5

---

## Version History

| Version | Release Date | Notes |
|---------|--------------|-------|
| 1.0.0   | 2026-03-05   | Initial release with full POS & PPOB features |

---

## Contributing

When contributing to this project, please:
1. Update the CHANGELOG.md with your changes under the [Unreleased] section
2. Follow the existing code style and conventions
3. Add tests for new features
4. Update documentation as needed

---

**Developed with ❤️ by Wahyu Akbar Wibowo**
