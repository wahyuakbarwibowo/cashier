# Migrasi ke Supabase (Rencana)

Status: **draft, belum dikerjakan**. Tujuan: aplikasi tetap offline-first (Room lokal
tetap ada), tapi data bisa sync ke cloud (Supabase/Postgres) untuk multi-device /
multi-toko dan pantauan owner dari jarak jauh.

## Kenapa Supabase

- Postgres asli + REST/Realtime + Auth sudah jadi, tidak perlu maintain server sendiri.
- Ada SDK Kotlin (`io.github.jan-tennert.supabase`) yang jalan di Android.
- Auth built-in dipakai untuk multi-user/multi-toko (tiap toko = 1 akun / 1 `shop_id`).

## Prinsip

- **Room tetap sumber kebenaran lokal.** App harus tetap bisa jalan tanpa internet.
- Supabase = lapisan sync, bukan pengganti Room. Setiap entity yang perlu online
  dapat kolom `remote_id`, `updated_at`, `is_synced` (atau `synced_at`), lalu:
  - **push**: baris baru/berubah di Room dikirim ke Supabase saat online.
  - **pull**: perubahan dari Supabase (device lain) ditarik ke Room.
- Konflik: pakai `updated_at` terbaru menang (last-write-wins). Cukup untuk POS kecil;
  tidak perlu CRDT/operational transform.
- Setiap baris butuh `shop_id` (scoping multi-toko) supaya Row Level Security di
  Supabase bisa membatasi akses per toko.

## Semua entity (19) dan rencana sync-nya

Kolom tambahan standar untuk tiap entity yang di-sync: `shop_id`, `remote_id`,
`updated_at`, `is_synced`. Tabel di bawah hanya menyebut kolom lain yang relevan
(FK, khas per entity).

| # | Entity (Room) | Tabel Supabase | Prioritas | FK / catatan |
|---|---|---|---|---|
| 1 | `ProductEntity` | `products` | Tinggi | tidak ada FK, paling sederhana → pilot pertama |
| 2 | `ProductVariantEntity` | `product_variants` | Tinggi | FK `productId` → `products.remote_id` |
| 3 | `SaleEntity` | `sales` | Tinggi | FK `customerId`, `paymentMethodId` (nullable) |
| 4 | `SaleItemEntity` | `sale_items` | Tinggi | FK `saleId`, `productId`, `variantId` (nullable) |
| 5 | `ReceivableEntity` | `receivables` | Tinggi | FK `saleId`, `customerId` |
| 6 | `PayableEntity` | `payables` | Tinggi | FK `purchaseId`, `supplierId` |
| 7 | `ShiftEntity` | `shifts` | Tinggi | tidak ada FK; `status open/closed` dipakai sebagai penanda shift aktif per device |
| 8 | `CustomerEntity` | `customers` | Tinggi | tidak ada FK |
| 9 | `CustomerPointsHistoryEntity` | `customer_points_history` | Tinggi | FK `customerId`, `saleId` (nullable) |
| 10 | `PaymentMethodEntity` | `payment_methods` | Sedang | tidak ada FK, master data kecil |
| 11 | `ShopProfileEntity` | `shop_profile` | Sedang | 1 baris per toko; jadi sumber `shop_id` di app |
| 12 | `SupplierEntity` | `suppliers` | Sedang | tidak ada FK |
| 13 | `PurchaseEntity` | `purchases` | Sedang | FK `supplierId` (nullable) |
| 14 | `PurchaseItemEntity` | `purchase_items` | Sedang | FK `purchaseId`, `productId` (nullable) |
| 15 | `ExpenseEntity` | `expenses` | Sedang | tidak ada FK |
| 16 | `StockHistoryEntity` | `stock_history` | Rendah | FK `productId`; log, append-only (tidak pernah di-update, cukup push) |
| 17 | `DigitalCategoryEntity` | `digital_categories` | Rendah | tidak ada FK, master data kecil |
| 18 | `DigitalProductEntity` | `digital_products` | Rendah | tidak ada FK |
| 19 | `PhoneHistoryEntity` | `phone_history` | Rendah | tidak ada FK; log transaksi PPOB, append-only |

Tidak perlu sync: `BackupData` (bukan entity Room, cuma DTO agregator untuk
backup JSON — dead code untuk online sync, tetap dipakai untuk export lokal).

### Append-only vs mutable

- **Append-only (push saja, tidak perlu pull-conflict)**: `StockHistoryEntity`,
  `PhoneHistoryEntity`, `CustomerPointsHistoryEntity`, `SaleItemEntity`,
  `PurchaseItemEntity` — baris ini dibuat sekali dan tidak pernah diubah, jadi
  cukup di-push begitu dibuat, pull kalau device lain butuh lihat riwayatnya.
- **Mutable (butuh last-write-wins)**: `ProductEntity` (stok berubah),
  `SaleEntity`/`ReceivableEntity`/`PayableEntity` (status/paid amount berubah),
  `CustomerEntity` (poin berubah), `ShiftEntity` (buka→tutup), `ShopProfileEntity`.

### Urutan migrasi FK (harus sync induk dulu sebelum anak)

`ShopProfile` → `Customer`, `Supplier`, `PaymentMethod`, `Product` →
`ProductVariant` → `Purchase` → `PurchaseItem`, `Payable` → `Sale` →
`SaleItem`, `Receivable`, `CustomerPointsHistory` → `StockHistory`.

`DigitalCategory` → `DigitalProduct` → `PhoneHistory` jalur terpisah (tidak
menyentuh entity di atas).

## Yang harus ditambahkan di app

1. **Dependency**: `io.github.jan-tennert.supabase:postgrest-kt`,
   `:realtime-kt` (kalau butuh live update), `:auth-kt`.
2. **Auth**: login toko (email/password atau magic link) sebelum sync aktif.
   Kalau belum login → app tetap jalan 100% offline seperti sekarang.
3. **Migration Room**: tambah kolom `remote_id TEXT`, `updated_at INTEGER`,
   `is_synced INTEGER DEFAULT 0` ke entity yang di-sync (lihat kebijakan migrasi
   di `.claude/CLAUDE.md` — versi DB naik, tulis `Migration` object, jangan
   destructive).
4. **SyncRepository** per entity (atau satu generic kalau shape-nya seragam):
   - `pushPending()`: query baris `is_synced = 0`, upsert ke Supabase, set
     `is_synced = 1` setelah sukses.
   - `pullRemote(since)`: fetch baris Supabase dengan `updated_at > since`,
     upsert ke Room (last-write-wins by `updated_at`).
5. **Trigger sync**: paling malas = tombol manual "Sync sekarang" + auto-sync
   saat app dibuka & ada koneksi. Tidak perlu WorkManager background job di
   awal — tambah kalau ternyata dibutuhkan.
6. **Skema Supabase**: 1 tabel Postgres per entity yang di-sync, kolom `shop_id`
   + RLS policy `shop_id = auth.uid()` (atau via tabel `shops` kalau 1 akun
   punya banyak toko).

## Tahapan (disarankan, incremental)

1. Setup project Supabase + skema tabel untuk entity prioritas tinggi + RLS.
2. Tambah Auth (login toko) di app — tanpa sync dulu, cuma supaya ada `shop_id`.
3. Implementasi push/pull untuk `ProductEntity` saja dulu (paling sederhana,
   tidak ada relasi child). Validasi alur end-to-end.
4. Lanjut ke `SaleEntity` + `SaleItemEntity` (ada relasi parent-child, lebih
   rawan konflik) baru entity sisanya.
5. Entity prioritas rendah menyusul kalau memang dibutuhkan.

## Trade-off

**Untung:**
- Owner bisa pantau semua toko dari 1 device tanpa harus keliling.
- Data tidak hilang kalau HP kasir rusak/hilang (ada salinan di cloud).
- Buka cabang baru tinggal login, tidak perlu backup-restore manual.

**Rugi / risiko:**
- App jadi butuh koneksi internet untuk fitur sync (walau tetap bisa transaksi
  offline, data numpuk sampai online lagi → resiko lupa sync = data beda antar
  device untuk sementara).
- Nambah kompleksitas: auth, RLS policy, migration Room, error handling saat
  sync gagal (retry, partial failure).
- Last-write-wins bisa "menimpa" perubahan device lain kalau 2 kasir edit data
  yang sama offline lalu sync bersamaan (jarang terjadi di POS kecil per-toko,
  tapi mungkin terjadi kalau 1 toko punya >1 device aktif).
- Owner jadi bergantung ke pihak ketiga (Supabase) — kalau mereka down atau
  ubah harga, fitur sync ikut kena.
- Data toko (harga jual, omzet) kini ada di server pihak ketiga, bukan cuma di
  device — perlu dipikirkan dari sisi privasi/kontrak kalau sensitif.

## Biaya (Supabase, estimasi — cek harga terbaru sebelum commit)

Harga di bawah perkiraan umum, bisa berubah — validasi di supabase.com/pricing
sebelum ambil keputusan.

- **Free tier**: cukup untuk pilot/testing 1-beberapa toko kecil — ada batas
  storage (~500MB DB), bandwidth, dan proyek di-pause otomatis kalau tidak
  ada aktivitas 7 hari (harus di-restart manual). Tidak cocok untuk produksi
  yang harus selalu on.
- **Plan berbayar (Pro, mulai ~$25/bulan)**: tidak ada auto-pause, storage &
  bandwidth lebih besar, ada tambahan biaya kalau lewat kuota (per GB storage/
  bandwidth/monthly active user tambahan).
- **Biaya lain**: 0 biaya server terpisah (sudah termasuk), tapi tetap butuh
  waktu dev untuk implementasi (poin "Rugi" di atas) — itu biaya terbesarnya,
  bukan biaya hosting.

Untuk skala "beberapa toko kecil, sync sesekali (bukan realtime tiap detik)",
free tier kemungkinan cukup untuk pilot; baru upgrade ke Pro kalau sudah jalan
produksi / butuh always-on.

## Yang sengaja tidak dikerjakan dulu (YAGNI)

- Realtime push notification antar device — cukup pull saat buka app dulu,
  upgrade ke Supabase Realtime kalau owner butuh update instan.
- Conflict resolution canggih (merge field-level) — last-write-wins cukup
  untuk kasus POS kecil, upgrade kalau ternyata sering bentrok.
- Background sync worker (WorkManager) — sync manual/on-open dulu, tambah
  kalau user komplain lupa sync.
