# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aminmart Cashier** — Android POS + PPOB app for small retail businesses. Kotlin + Jetpack Compose + Room (SQLite). Fully offline. UI in Indonesian.

## Development Commands

```bash
make build          # Build debug APK
make install        # Install debug APK to connected device
make dev            # clean + build + install + run (full cycle)
make run            # Launch app on connected device
make test           # Run unit tests
make lint           # Run lint checks
make logs           # View app logcat (filters to this process)
make db-pull        # Pull database file from device
make db-clear       # Clear all app data on device
```

Signed release:
```bash
make release-signed   # Requires signing.properties (see signing.properties.example)
make bundle-signed    # Signed AAB
```

Package name: `com.wahyuakbarwibowo.aminmartkasir`
DB name: `kasir_database` (Room, version 9)

## Architecture

### Stack
- **Language**: Kotlin 2.3.10
- **UI**: Jetpack Compose (BOM 2024.12.01)
- **Database**: Room 2.8.4 with KSP annotation processing
- **Architecture**: MVVM
- **Async**: Kotlin Coroutines
- **Navigation**: Navigation Compose 2.8.5

### Code Structure

```
app/src/main/kotlin/.../aminmartkasir/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room DB singleton, version 9, fallbackToDestructiveMigration
│   │   ├── dao/                 # 18 DAOs
│   │   ├── entity/              # Room entities
│   │   └── converter/           # TypeConverters
│   └── repository/              # Repository per domain (15 repos)
├── ui/
│   ├── screens/                 # Compose screens
│   ├── viewmodel/               # ViewModels + ViewModelFactory + UiState
│   ├── components/              # Shared composables
│   ├── navigation/              # Nav graph
│   ├── scanner/                 # Barcode camera
│   └── theme/
├── utils/
│   ├── BluetoothPrinterHelper.kt
│   ├── CurrencyUtils.kt
│   └── ExcelExportUtils.kt
└── MainActivity.kt              # Entry: initializes DB, sets content
```

### Database

`AppDatabase` is a singleton (`@Volatile` + `synchronized`). Get instance via `AppDatabase.getDatabase(context)`. **Schema migrations use `fallbackToDestructiveMigration`** — adding entities/columns requires bumping `version` and wiping data on device during development.

18 entities including: `ProductEntity`, `ProductVariantEntity`, `SaleEntity`, `SaleItemEntity`, `PurchaseEntity`, `ReceivableEntity`, `PayableEntity`, `PhoneHistoryEntity`, `DigitalProductEntity`, `DigitalCategoryEntity`, `ExpenseEntity`, `CustomerPointsHistoryEntity`, `StockHistoryEntity`.

### State Management

All screen state lives in ViewModels (no shared state/global store). `ViewModelFactory` manually wires repositories into ViewModels — add new VMs there. `UiState.kt` defines sealed class for loading/success/error.

### Key Business Logic

- **Auto-Receivables**: When sale payment < total, `SaleRepository` auto-creates a `ReceivableEntity`. Payment method containing "hutang" allows partial/zero payments.
- **Product Variants**: `ProductVariantEntity` links to `ProductEntity`. Dual pricing: unit vs package (`package_price` / `package_qty`).
- **Stock Validation**: `SalesViewModel` blocks checkout when stock insufficient.
- **Backdate**: Sales and digital transactions accept custom dates.
- **Thermal Printing**: `BluetoothPrinterHelper.kt` handles 58mm receipt layout via Android Bluetooth API.
- **Digital Categories**: Dynamic CRUD in `DigitalCategoryDao` — categories (PULSA, PLN, PDAM, etc.) are user-managed, not hardcoded.
- **Backup/Restore**: `BackupRepository` exports/imports full DB as JSON.

### Navigation

Single `NavHost` in `ui/navigation/`. All routes are flat (no nested graphs). Screens receive `navController` and `viewModelFactory` as params.

## Skills

- `/commit` — Conventional Commits format commit
- `/pr` — Create GitHub PR to master

## Language

App UI and user-facing strings are in Indonesian. Code comments may also be in Indonesian.
