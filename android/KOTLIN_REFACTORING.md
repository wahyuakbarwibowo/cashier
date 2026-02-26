# 📱 Aminmart Cashier - Kotlin Native Android Version

Dokumentasi ini menjelaskan refactoring aplikasi Aminmart Cashier dari React Native ke **Kotlin Native Android** dengan **Jetpack Compose**.

---

## 🏗️ Arsitektur Aplikasi

Aplikasi ini menggunakan **MVVM Architecture** (Model-View-ViewModel) dengan clean architecture principles:

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  ┌─────────────────────────────────┐   │
│  │        Screens/Views            │   │
│  └─────────────────────────────────┘   │
│              ↓ ↑                        │
│  ┌─────────────────────────────────┐   │
│  │       ViewModels (StateFlow)    │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────────┐
│         Repository Layer                │
│  ┌─────────────────────────────────┐   │
│  │         Repositories            │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────────┐
│          Data Layer                     │
│  ┌─────────────────────────────────┐   │
│  │   Room Database (Entities)      │   │
│  │   DAOs (Data Access Objects)    │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 📂 Struktur Folder

```
android/app/src/main/kotlin/com/wahyuakbarwibowo/aminmartkasir/
├── data/
│   ├── local/
│   │   ├── entity/           # Room Entities
│   │   ├── dao/              # Data Access Objects
│   │   ├── converter/        # Type Converters
│   │   └── AppDatabase.kt    # Room Database
│   └── repository/           # Repositories
├── ui/
│   ├── viewmodel/            # ViewModels
│   ├── screens/              # Compose UI Screens
│   ├── navigation/           # Navigation Setup
│   └── theme/                # Theme & Styling
└── MainActivity.kt           # Main Entry Point
```

---

## 🛠️ Tech Stack

### Core Android
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### UI & Navigation
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system
- **Navigation Compose**: In-app navigation

### Architecture & Data
- **Room Database**: Local SQLite database
- **ViewModel**: UI-related data holder
- **StateFlow**: Reactive data streams
- **Coroutines**: Async operations

### Dependencies
```kotlin
// Compose BOM
androidx.compose:compose-bom:2024.02.00

// Navigation
androidx.navigation:navigation-compose:2.7.7

// Room Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// ViewModel
androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3
```

---

## 📋 Fitur yang Diimplementasikan

### ✅ Selesai
1. **Dashboard**
   - Ringkasan penjualan hari ini & bulan ini
   - Total produk, pelanggan, supplier
   - Quick actions ke menu utama
   - Alert stok rendah

2. **Manajemen Produk**
   - List produk dengan pencarian
   - Tambah/edit/hapus produk
   - Manajemen stok
   - Harga beli & jual (satuan & paket)

3. **Manajemen Pelanggan**
   - List pelanggan dengan pencarian
   - Tambah/edit/hapus pelanggan
   - Sistem poin pelanggan

4. **Transaksi Penjualan**
   - Keranjang belanja
   - Pilih produk dari dialog
   - Pilih pelanggan
   - Pilih metode pembayaran
   - Kalkulasi otomatis (subtotal, diskon, kembalian)
   - Poin earned & redeemed
   - **🖨️ Cetak Struk Bluetooth Thermal Printer**

5. **Riwayat Penjualan**
   - List semua transaksi
   - Filter berdasarkan tanggal
   - Detail transaksi lengkap

6. **Manajemen Supplier**
   - List supplier
   - Tambah/edit/hapus supplier

7. **Pengaturan**
   - Profil toko
   - Metode pembayaran
   - Konfigurasi sistem poin

8. **🖨️ Bluetooth Thermal Printer**
   - Connect ke printer Bluetooth 58mm
   - Test print
   - Auto-print setelah transaksi
   - Format struk thermal dengan header toko
   - Support ESC/POS commands

### 🚧 Coming Soon (Placeholder Screens)
- Pembelian & Purchase Orders
- Pengeluaran Operasional
- Hutang & Piutang
- Transaksi Digital (Pulsa, PLN, dll)
- Laporan Laba Rugi
- Backup & Restore Database

---

## 🚀 Cara Menjalankan

### Prerequisites
1. **Android Studio**: Arctic Fox (2020.3.1) atau lebih baru
2. **JDK**: Version 11 atau lebih baru
3. **Android SDK**: API Level 24+

### Langkah-langkah

1. **Buka Project di Android Studio**
   ```bash
   cd android
   # Buka folder ini di Android Studio
   ```

2. **Sync Gradle**
   - Klik "Sync Project with Gradle Files" di Android Studio
   - Atau jalankan: `./gradlew sync`

3. **Build & Run**
   - Pilih device/emulator
   - Klik tombol Run (▶️)
   - Atau jalankan: `./gradlew installDebug`

4. **Build APK Release**
   ```bash
   ./gradlew assembleRelease
   ```

---

## 💾 Database Schema

### Tabel Utama

#### `products`
- id, code, name
- purchase_price, purchase_package_price, purchase_package_qty
- selling_price, package_price, package_qty
- discount, stock
- created_at, updated_at

#### `customers`
- id, name, phone, address
- points
- created_at, updated_at

#### `sales` & `sales_items`
- id, customer_id, payment_method_id
- total, paid, change
- points_earned, points_redeemed
- created_at

#### `suppliers`, `purchases`, `purchase_items`
- Supplier & purchase management

#### `payment_methods`
- id, name

#### `shop_profile`
- name, footer_note, cashier_name
- phone_number, address
- poin_enabled

#### `digital_products`, `digital_categories`
- Digital product catalog

#### `phone_history`
- Digital transaction history

#### `expenses`
- Operational expenses

#### `receivables`, `payables`
- Hutang & Piutang

#### `customer_points_history`
- Poin transaction log

---

## 🎨 UI Components

### Screens
- **DashboardScreen**: Main dashboard dengan summary cards
- **ProductsScreen**: Product list dengan search
- **ProductFormScreen**: Form tambah/edit produk
- **CustomersScreen**: Customer list
- **SalesTransactionScreen**: POS transaction screen
- **SalesHistoryScreen**: Transaction history
- **SaleDetailScreen**: Transaction detail
- **SuppliersScreen**: Supplier management
- **SettingsScreen**: App settings

### Common Components
- SummaryCard: Dashboard summary cards
- QuickActionButton: Dashboard action buttons
- ProductCard: Product list item
- CustomerCard: Customer list item
- CartItemCard: Shopping cart item
- Various Selector Dialogs

---

## 🔄 Migration dari React Native

### Perbedaan Utama

| React Native | Kotlin Native |
|-------------|---------------|
| React Components | Compose Functions |
| Redux/Context | ViewModel + StateFlow |
| AsyncStorage | Room Database |
| React Navigation | Navigation Compose |
| TypeScript | Kotlin |
| Metro Bundler | Gradle Build |

### Data Flow

**React Native:**
```
Component → Redux Store → API/SQLite → Component
```

**Kotlin Native:**
```
Composable → ViewModel → Repository → Room → ViewModel → Composable
```

---

## 📝 Development Guidelines

### 1. Menambah Screen Baru

```kotlin
// 1. Buat Screen di ui/screens/
@Composable
fun NewScreen(
    onNavigateBack: () -> Unit,
    viewModel: NewViewModel = viewModel(factory = ViewModelFactory())
) {
    // UI implementation
}

// 2. Buat ViewModel di ui/viewmodel/
class NewViewModel(
    private val repository: NewRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewUiState())
    val uiState: StateFlow<NewUiState> = _uiState.asStateFlow()
}

// 3. Tambahkan route di ui/navigation/Screen.kt
object NewScreen : Screen("new_screen")

// 4. Tambahkan navigation di AppNavigation.kt
composable(Screen.NewScreen.route) {
    NewScreen(onNavigateBack = { navController.popBackStack() })
}
```

### 2. Menambah Entity Baru

```kotlin
// 1. Buat Entity di data/local/entity/
@Entity(tableName = "new_table")
data class NewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)

// 2. Buat DAO di data/local/dao/
@Dao
interface NewDao {
    @Query("SELECT * FROM new_table")
    fun getAll(): Flow<List<NewEntity>>
    
    @Insert
    suspend fun insert(entity: NewEntity): Long
}

// 3. Tambahkan ke AppDatabase.kt
abstract class AppDatabase : RoomDatabase() {
    abstract fun newDao(): NewDao
}

// 4. Buat Repository di data/repository/
class NewRepository(private val dao: NewDao) {
    val allItems: Flow<List<NewEntity>> = dao.getAll()
}
```

---

## 🐛 Troubleshooting

### Build Errors
1. **Kotlin version mismatch**: Pastikan versi Kotlin di build.gradle konsisten
2. **Compose compiler error**: Update `kotlinCompilerExtensionVersion`
3. **Room kapt error**: Pastikan `kapt` plugin sudah diterapkan

### Runtime Errors
1. **Database migration**: Gunakan `fallbackToDestructiveMigration()` untuk development
2. **ViewModel creation**: Pastikan ViewModelFactory sudah benar

---

## 📦 APK Signing

1. **Generate Keystore**
   ```bash
   keytool -genkey -v -keystore aminmart.keystore -alias aminmart -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Create signing.properties**
   ```properties
   MYAPP_UPLOAD_STORE_FILE=aminmart.keystore
   MYAPP_UPLOAD_KEY_ALIAS=aminmart
   MYAPP_UPLOAD_STORE_PASSWORD=your_password
   MYAPP_UPLOAD_KEY_PASSWORD=your_password
   ```

3. **Build Signed APK**
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📄 License

Developed with ❤️ by **Wahyu Akbar Wibowo**

---

## 🤝 Kontribusi

Untuk kontribusi, silakan:
1. Fork repository
2. Buat feature branch
3. Commit changes
4. Push ke branch
5. Create Pull Request

---

**Catatan**: Refactoring ini mengubah aplikasi dari React Native ke Kotlin Native Android. Beberapa fitur mungkin masih dalam pengembangan.
