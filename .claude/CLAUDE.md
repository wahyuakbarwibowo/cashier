# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aminmart Cashier** — Android POS + PPOB app for small retail businesses. Kotlin + Jetpack Compose + Room (SQLite). Offline-first: transactions live in a local DB per store; Supabase handles login, staff/tenant management, cloud backup, and product sync. UI in Indonesian.

## Development Commands

```bash
make build          # Build debug APK
make install        # Install debug APK to connected device
make dev            # clean + build + install + run (full cycle)
make run            # Launch app on connected device
make test           # Run all unit tests (gradlew testDebugUnitTest)
make lint           # Run lint checks
make logs           # View app logcat (filters to this process)
make db-pull        # Pull database file from device
make db-clear       # Clear all app data on device
```

Room migration tests (instrumented, need a device/emulator; schema JSON in `app/schemas/`):
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.wahyuakbarwibowo.aminmartkasir.data.local.MigrationTest
```
Every new `Migration(n, n+1)` must be added to `MigrationTest`.

Single test class/method (no make target — call gradle directly):
```bash
./gradlew testDebugUnitTest --tests "com.wahyuakbarwibowo.aminmartkasir.SomeTest"
./gradlew testDebugUnitTest --tests "*.SomeTest.someMethod"
```

Signed release:
```bash
make release-signed   # Requires signing.properties (see signing.properties.example)
make bundle-signed    # Signed AAB
```

Package name: `com.wahyuakbarwibowo.aminmartkasir`
DB name: one file per store via `TenantDatabaseFiles.name(storeId)` (Room, version 16)

## Architecture

### Stack
- **Language**: Kotlin 2.3.10
- **UI**: Jetpack Compose (BOM 2025.01.00), Material3 1.3.1
- **Database**: Room 2.8.4 with KSP annotation processing
- **Architecture**: MVVM
- **Async**: Kotlin Coroutines
- **Navigation**: Navigation Compose 2.8.5
- **Charts**: Vico 2.0.0-alpha.27 (compose + compose-m3)

### Code Structure

```
app/src/main/kotlin/.../aminmartkasir/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room DB per store, version 16, explicit Migration objects
│   │   ├── dao/                 # 19 DAOs
│   │   └── entity/              # 20 Room entities
│   ├── remote/                  # Supabase: AuthManager, AccountManager, FeatureAccessManager
│   ├── sync/                    # ProductSyncWorker (WorkManager)
│   └── repository/              # Repository per domain
├── ui/
│   ├── screens/                 # Compose screens
│   ├── viewmodel/               # ViewModels + ViewModelFactory
│   ├── components/              # Shared composables
│   ├── navigation/              # Nav graph
│   ├── scanner/                 # Barcode camera; use rememberBarcodeScanner() for permission + scan
│   └── theme/
├── utils/
│   ├── BluetoothPrinterHelper.kt
│   ├── CurrencyUtils.kt
│   └── ExcelExportUtils.kt
└── MainActivity.kt              # Entry: initializes DB, sets content
```

### Database

`AppDatabase` keeps one instance per store (`synchronized` map). Get it via `AppDatabase.getDatabase(context, storeId)`. Current `version = 16`. **Schema changes use real `Migration` objects** (registered via `.addMigrations(...)` in `getDatabase`) for version 9 and above — adding entities/columns requires bumping `version` AND writing a `Migration(n, n+1)` object with the raw SQL. Do not wipe device data; data must survive upgrades. Versions 1-8 predate this policy and have no schema snapshots, so `.fallbackToDestructiveMigrationFrom(1..8)` is scoped to just those legacy starting versions to unblock devices stuck there — never widen that range going forward.

20 entities (19 DAOs) including: `ProductEntity`, `ProductVariantEntity`, `SaleEntity`, `SaleItemEntity`, `PurchaseEntity`, `ReceivableEntity`, `PayableEntity`, `PhoneHistoryEntity`, `DigitalProductEntity`, `DigitalCategoryEntity`, `ExpenseEntity`, `CustomerPointsHistoryEntity`, `StockHistoryEntity`, `ShiftEntity`.

### State Management

All screen state lives in ViewModels (no shared state/global store, except `AuthManager.state` for the signed-in profile). `ViewModelFactory` manually wires repositories into ViewModels — add new VMs there.

### Key Business Logic

- **Auto-Receivables**: When sale payment < total, `SaleRepository` auto-creates a `ReceivableEntity`. Payment method containing "hutang" allows partial/zero payments. Payments use atomic SQL (`paidAmount = paidAmount + :x`).
- **Stock writes**: Change stock only through DAO `@Transaction` helpers (`insertWithInitialStock`, `updateWithStockDelta`, `adjustStockWithHistory`, checkout in `SaleRepository`) so every change is a relative delta with a `StockHistoryEntity` row. Never write an absolute stock value read earlier.
- **Product Variants**: `ProductVariantEntity` links to `ProductEntity`. Dual pricing: unit vs package (`package_price` / `package_qty`).
- **Stock Validation**: `SaleRepository` re-checks stock inside the checkout transaction and throws when insufficient; line totals come from `productLineSubtotal()`.
- **Backdate**: Sales and digital transactions accept custom dates.
- **Thermal Printing**: `BluetoothPrinterHelper.kt` handles 58mm receipt layout via Android Bluetooth API.
- **Digital Categories**: Dynamic CRUD in `DigitalCategoryDao` — categories (PULSA, PLN, PDAM, etc.) are user-managed, not hardcoded.
- **Backup/Restore**: `BackupRepository` exports/imports full DB as JSON.
- **Shift Management**: `ShiftEntity` tracks cash drawer sessions (opening/counted cash, expected vs difference, open/closed status). Expected cash counts only payment methods with `PaymentMethodEntity.isCash`.
- **Global Search**: `GlobalSearchViewModel` runs DB-backed cross-entity search (products, sales, customers, etc.) from `GlobalSearchScreen`.

### Supabase (server)

- Schema lives in `supabase/migrations/` (numbered SQL, applied manually by the operator) and edge functions in `supabase/functions/`. Add a new numbered file; never edit an applied migration.
- Access control is enforced by RLS + triggers on the server; UI role checks (`AppMenus.kt`) are convenience only.
- Roles: `super_admin`, `admin`, `kasir`. `AuthManager` keeps a cached profile so the POS works offline; only a 400/401 on refresh ends the session.
- Product sync: metadata is last-write-wins on `sync_updated_at` (server clamps future timestamps). Stock is synced as deltas: `ProductEntity.syncedStock` is the last stock agreed with the cloud, `stock - syncedStock` is pushed via RPC `add_product_stock`, and pulls keep unpushed local deltas. Upserts never send `stock`.

### Navigation

Single `NavHost` in `ui/navigation/`. All routes are flat (no nested graphs). Screens receive `navController` and `viewModelFactory` as params.

## Skills

- `/commit` — Conventional Commits format commit
- `/pr` — Create GitHub PR to master

## Language

App UI and user-facing strings are in Indonesian. Code comments may also be in Indonesian.

## Versioning Workflow

Setiap perubahan kode yang di-commit **wajib** disertai bump versi di `app/build.gradle`:

- `versionCode` selalu +1.
- `versionName` (`MAJOR.MINOR.PATCH`): PATCH untuk bug fix, MINOR untuk fitur baru, MAJOR untuk perubahan besar/breaking.
- Tambahkan entri di `CHANGELOG.md` di bawah heading versi baru (Added/Changed/Fixed), di atas versi sebelumnya.
- Bump versi + changelog masuk **commit terpisah** (`chore: naikkan versi ke X.Y.Z`) setelah commit perubahan kodenya.
- Dikecualikan: perubahan yang tidak menyentuh kode aplikasi (dokumentasi, `.gitignore`, konfigurasi tooling).
