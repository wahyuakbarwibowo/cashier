# Ekspor dan Impor Data Excel/PDF

Buka **Lainnya → Backup & Restore → Excel dan PDF** pada akun toko yang aktif.
Pilih **Unduh Semua Data Excel** untuk menyimpan workbook `.xlsx` berisi 19 sheet
data lokal toko: produk, varian, pelanggan, transaksi, pembelian, hutang/piutang,
pengeluaran, layanan digital, riwayat, profil toko, dan shift. Produk yang sudah
dihapus juga ikut disimpan agar status sinkronisasinya tidak hilang.

## Template Excel

Pilih **Unduh Template Excel** untuk workbook kosong dengan sheet dan kolom yang
sama. Isi satu baris per catatan. Biarkan sheet yang tidak dipakai tetap kosong,
tetapi jangan hapus atau ubah nama sheet/kolom. Pertahankan kolom `id` dan ID
referensi (`productId`, `saleId`, `customerId`, dll.) ketika memindahkan data
yang saling terkait. Kolom diformat sebagai teks agar nomor telepon dengan nol
di depan tidak berubah. Simpan sebagai `.xlsx` sebelum diimpor. Formula Excel
tidak didukung.

## Impor dan PDF

**Impor Excel mengganti seluruh data lokal toko yang sedang login**, termasuk
sheet yang dibiarkan kosong. Simpan cadangan JSON atau Excel terlebih dahulu.
Aplikasi meminta konfirmasi, menolak template yang masih kosong atau struktur
sheet yang tidak lengkap, dan membatalkan seluruh impor jika ada kesalahan
database. Data akun/login Supabase dan entitlement Pro tidak termasuk workbook.
Gunakan file untuk toko yang sama; ID sinkronisasi produk dari toko lain tidak
boleh dipindahkan langsung ke toko ini. Jalur migrasi antartoko perlu pemetaan ID
tersendiri.

**Unduh Semua Data PDF** menyimpan salinan baca saja dari 19 kelompok data.
PDF tidak dapat dipakai untuk impor. File Excel dan PDF dapat memuat nomor
telepon, transaksi, dan informasi keuangan toko; simpan di lokasi yang sesuai.
