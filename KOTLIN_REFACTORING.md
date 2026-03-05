# 📱 Aminmart Cashier - Kotlin Native Android Version

Dokumentasi ini menjelaskan refactoring aplikasi Aminmart Cashier dari React Native ke **Kotlin Native Android** dengan **Jetpack Compose**.

---

## 🎨 Brand Identity

### Pink Theme - Aminmart Logo
Aplikasi menggunakan tema pink yang diambil dari logo Aminmart:

| Warna | Hex Code | Penggunaan |
|-------|----------|--------------|
| **Primary Pink** | `#E91E8B` | Tombol, header, accent UI |
| **Secondary Maroon** | `#800020` | Siluet logo, outlines |
| **Light Pink** | `#FF66B2` | Dark mode primary |
| **Background** | `#FFF5F8` | Light mode background |

**Color Consistency:**
- XML Theme (`colors.xml`): Sinkron dengan Compose Theme
- Dark Mode Support: `values-night/colors.xml`
- Material 3 Color Scheme: `Color.kt` & `Theme.kt`

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
app-cashier/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/wahyuakbarwibowo/aminmartkasir/
│   │       │   ├── data/
│   │       │   │   ├── local/
│   │       │   │   │   ├── entity/           # Room Entities
│   │       │   │   │   ├── dao/              # Data Access Objects
│   │       │   │   │   ├── converter/        # Type Converters
│   │       │   │   │   └── AppDatabase.kt    # Room Database
│   │       │   │   └── repository/           # Repositories
│   │       │   ├── ui/
│   │       │   │   ├── viewmodel/            # ViewModels
│   │       │   │   ├── screens/              # Compose UI Screens
│   │       │   │   ├── navigation/           # Navigation Setup
│   │       │   │   ├── theme/                # Theme & Styling
│   │       │   │   └── components/           # Reusable Components
│   │       │   ├── utils/                    # Utility Classes
│   │       │   ├── MainActivity.kt           # Main Activity
│   │       │   └── MainApplication.kt        # Application Class
│   │       └── res/                          # Android Resources
│   │           ├── values/                   # Colors, Strings, Themes
│   │           ├── values-night/             # Dark Mode Colors
│   │           └── drawable/                 # Assets & Icons
│   └── build.gradle
├── app/src/test/                             # Unit Tests
│   └── kotlin/com/wahyuakbarwibowo/aminmartkasir/
│       └── data/repository/                  # Repository Tests
├── signing.properties.example                # Signing Template
├── CHANGELOG.md                              # Version History
└── KOTLIN_REFACTORING.md                     # This File
```

---

## 🛠️ Tech Stack

### Core Android
- **Language**: Kotlin 2.3.10
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35

### UI & Navigation
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system (Compose BOM 2025.01.00)
- **Navigation Compose**: In-app navigation (2.8.5)

### Architecture & Data
- **Room Database**: Local SQLite database (2.8.4)
- **ViewModel**: UI-related data holder
- **StateFlow**: Reactive data streams
- **Coroutines**: Async operations (1.10.1)

### Testing
- **JUnit4**: Unit testing framework
- **Mockito**: Mocking framework (5.14.2)
- **Mockito Kotlin**: Kotlin extensions (5.4.0)
- **Coroutines Test**: Testing coroutines (1.10.1)

### Build & Optimization
- **R8 Full Mode**: Code shrinking & optimization
- **KSP**: Kotlin Symbol Processing (2.3.5)
- **Gradle 9.1.0**: Build system

### Dependencies
```kotlin
// Compose BOM
implementation platform('androidx.compose:compose-bom:2025.01.00')

// Navigation
implementation 'androidx.navigation:navigation-compose:2.8.5'

// Room Database
implementation "androidx.room:room-runtime:2.8.4"
implementation "androidx.room:room-ktx:2.8.4"
ksp "androidx.room:room-compiler:2.8.4"

// ViewModel
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7'
implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.8.7'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1'

// Testing
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.14.2'
testImplementation 'org.mockito.kotlin:mockito-kotlin:5.4.0'
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1'
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
   - Kalkulasi otomatis harga paket/dus

3. **Manajemen Pelanggan**
   - List pelanggan dengan pencarian
   - Tambah/edit/hapus pelanggan
   - Sistem poin pelanggan
   - Riwayat poin

4. **Transaksi Penjualan (POS)**
   - Keranjang belanja
   - Pilih produk dari dialog
   - Pilih pelanggan
   - Pilih metode pembayaran
   - Kalkulasi otomatis (subtotal, diskon, kembalian)
   - Poin earned & redeemed
   - **🖨️ Cetak Struk Bluetooth Thermal Printer**
   - Validasi stok pintar

5. **Riwayat Penjualan**
   - List semua transaksi
   - Filter berdasarkan tanggal
   - Detail transaksi lengkap
   - Format tanggal Indonesia

6. **Manajemen Supplier**
   - List supplier
   - Tambah/edit/hapus supplier

7. **Transaksi Digital (PPOB)**
   - Pulsa, PLN, E-Wallet, BPJS, Game
   - Kategori dinamis
   - Riwayat transaksi digital
   - Edit transaksi digital
   - Input tanggal transaksi

8. **Laporan & Analytics**
   - Laporan Laba Rugi
   - Filter rentang tanggal
   - Detail transaksi per produk

9. **Hutang & Piutang**
   - Manajemen piutang pelanggan
   - Manajemen hutang supplier
   - Jatuh tempo
   - Tagih via WhatsApp

10. **Pengeluaran Operasional**
    - Catatan biaya harian
    - Kategori pengeluaran
    - Filter tanggal

11. **Stok History**
    - Tracking perubahan stok
    - Riwayat masuk/keluar

12. **Pengaturan**
    - Profil toko
    - Metode pembayaran
    - Konfigurasi sistem poin

13. **🖨️ Bluetooth Thermal Printer**
    - Connect ke printer Bluetooth 58mm
    - Test print
    - Auto-print setelah transaksi
    - Format struk thermal dengan header toko
    - Support ESC/POS commands

14. **💾 Backup & Restore**
    - Ekspor database ke JSON
    - Impor database dari backup
    - Restore data lengkap

15. **⚠️ Low Stock Alert**
    - Notifikasi visual
    - Daftar produk stok kritis

16. **🌸 Premium UI/UX**
    - Pink theme sesuai logo
    - Dark mode support
    - Keyboard avoidance
    - Interactive reports

---

## 🚀 Cara Menjalankan

### Prerequisites
1. **Android Studio**: Meerkat atau lebih baru
2. **JDK**: Version 17 atau lebih baru
3. **Android SDK**: API Level 35
4. **Min Device**: Android 7.0 (API 24)

### Langkah-langkah

#### 1. Clone Repository
```bash
git clone https://github.com/wahyuakbarwibowo/rn-cashier-app.git
cd aminmart/app-cashier
```

#### 2. Build & Run via Android Studio
1. Buka **Android Studio** → **Open Project**
2. Tunggu Gradle sync selesai
3. Pilih device/emulator
4. Klik **Run** (▶️) atau `Shift + F10`

#### 3. Build & Run via Command Line (Makefile)
```bash
# Clean build
make clean

# Build debug APK
make build

# Build + Install ke device
make install

# Build + Install + Run
make dev

# View logs
make logs

# Run tests
make test

# Run lint
make lint
```

#### 4. Build & Run via Gradle
```bash
# Build debug APK
./gradlew assembleDebug

# Install ke device
./gradlew installDebug

# Build + Install + Run
./gradlew installDebug && \
  adb shell am start -n com.wahyuakbarwibowo.aminmartkasir/.MainActivity

# Build release APK (unsigned)
./gradlew assembleRelease --property unsignedRelease=true

# Run tests
./gradlew test

# Run lint
./gradlew lint
```

#### 5. Build Signed Release APK
1. Buat `signing.properties` dari template:
   ```bash
   cp signing.properties.example signing.properties
   ```
2. Edit `signing.properties` dengan credentials Anda
3. Build:
   ```bash
   ./gradlew assembleRelease
   ```
   APK akan dihasilkan di: `app/build/outputs/apk/release/app-release.apk`

---

## 💾 Database Schema

### Tabel Utama

#### `products`
| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK) | Auto-generated ID |
| code | String | Product code/SKU |
| name | String | Product name |
| purchasePrice | Double | Harga beli satuan |
| purchasePackagePrice | Double | Harga beli per paket/dus |
| purchasePackageQty | Int | Isi per paket |
| sellingPrice | Double | Harga jual satuan |
| packagePrice | Double | Harga jual per paket |
| packageQty | Int | Isi per paket |
| discount | Double | Diskon produk |
| stock | Int | Stok tersedia |
| createdAt | String | Tanggal dibuat |
| updatedAt | String | Tanggal update |

#### `customers`
| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK) | Auto-generated ID |
| name | String | Nama pelanggan |
| phone | String | Nomor telepon |
| address | String | Alamat |
| points | Int | Poin akumulasi |
| createdAt | String | Tanggal dibuat |
| updatedAt | String | Tanggal update |

#### `sales` & `sales_items`
| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK) | Auto-generated ID |
| customer_id | Long (FK) | Referensi ke customers |
| payment_method_id | Long (FK) | Referensi ke payment_methods |
| total | Double | Total transaksi |
| paid | Double | Uang dibayarkan |
| change | Double | Kembalian |
| points_earned | Int | Poin didapat |
| points_redeemed | Int | Poin ditukar |
| createdAt | String | Tanggal transaksi |

#### `digital_products` & `digital_categories`
- Katalog produk digital
- Kategori dinamis (PULSA, PLN, E-Wallet, dll)
- Provider-based (Telkomsel, XL, Tri, dll)

#### `phone_history`
- Log transaksi digital
- Notes/token hasil transaksi

#### `expenses`
- Pengeluaran operasional
- Kategori, jumlah, tanggal

#### `receivables` & `payables`
- Hutang piutang
- Jatuh tempo, status pembayaran

#### `customer_points_history`
- Riwayat transaksi poin
- Earned & redeemed log

#### `shop_profile`
- Nama toko, footer struk
- Phone, address
- Settings (poin enabled, etc)

---

## 🎨 UI Components

### Screens
- **DashboardScreen**: Main dashboard dengan summary cards & quick actions
- **ProductsScreen**: Product list dengan search & filter
- **ProductFormScreen**: Form tambah/edit produk
- **CustomersScreen**: Customer list dengan search
- **SalesTransactionScreen**: POS transaction screen dengan keranjang
- **SalesHistoryScreen**: Transaction history dengan filter tanggal
- **SaleDetailScreen**: Detail transaksi lengkap
- **SuppliersScreen**: Supplier management
- **SettingsScreen**: App settings
- **DigitalTransactionScreen**: Transaksi produk digital
- **DigitalManagementScreen**: Manajemen katalog digital
- **DigitalReportsScreen**: Laporan transaksi digital
- **ExpensesScreen**: Pencatatan pengeluaran
- **ProfitLossScreen**: Laporan laba rugi
- **StockHistoryScreen**: Riwayat stok
- **LowStockScreen**: Produk stok rendah
- **BackupScreen**: Backup & restore database
- **BluetoothPrinterDialog**: Dialog pairing printer

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
3. **Room kapt error**: Pastikan `kapt`/`KSP` plugin sudah diterapkan
4. **R8/ProGuard errors**: Check proguard-rules.pro untuk keep rules

### Runtime Errors
1. **Database migration**: Gunakan `fallbackToDestructiveMigration()` untuk development
2. **ViewModel creation**: Pastikan ViewModelFactory sudah benar
3. **Bluetooth permission**: Pastikan permission Bluetooth sudah granted (Android 12+)

### Testing Errors
1. **Mockito initialization**: Pastikan `MockitoAnnotations.openMocks()` dipanggil di @Before
2. **Coroutines test**: Gunakan `runTest` untuk test suspend functions
3. **Flow test**: Gunakan `first()` atau `toList()` untuk collect Flow

---

## 📦 APK Signing

### 1. Generate Keystore
```bash
keytool -genkey -v -keystore aminmart.keystore \
  -alias aminmart -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Create signing.properties
```bash
cp signing.properties.example signing.properties
```

Edit `signing.properties`:
```properties
MYAPP_UPLOAD_STORE_FILE=aminmart.keystore
MYAPP_UPLOAD_KEY_ALIAS=aminmart
MYAPP_UPLOAD_STORE_PASSWORD=your_password
MYAPP_UPLOAD_KEY_PASSWORD=your_password
```

### 3. Build Signed APK
```bash
# Signed release
./gradlew assembleRelease

# Unsigned release (for testing)
./gradlew assembleRelease --property unsignedRelease=true
```

APK akan dihasilkan di: `app/build/outputs/apk/release/`

---

## 🧪 Testing

### Run Tests
```bash
# All tests
./gradlew test

# Unit tests only
./gradlew testDebugUnitTest

# With coverage
./gradlew testDebugUnitTest jacocoTestReport
```

### Write Unit Test
```kotlin
class ProductRepositoryTest {

    @Mock
    private lateinit var productDao: ProductDao

    private lateinit var productRepository: ProductRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        productRepository = ProductRepository(productDao)
    }

    @Test
    fun `insert product should call dao insert`() = runTest {
        // Given
        val product = ProductEntity(name = "Test", stock = 10)
        `when`(productDao.insert(product)).thenReturn(1L)

        // When
        val result = productRepository.insert(product)

        // Then
        assertEquals(1L, result)
        verify(productDao).insert(product)
    }
}
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

### Coding Guidelines
- Follow Kotlin conventions (4 spaces, camelCase, PascalCase)
- Add KDoc untuk public functions
- Write tests untuk new features
- Update CHANGELOG.md

---

## 📚 Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Material 3](https://m3.material.io/)
- [Android Developers](https://developer.android.com/)

---

## 📋 Checklist Fitur Lengkap

| Module | Status | Notes |
|--------|--------|-------|
| Dashboard | ✅ | Summary cards, quick actions |
| Products | ✅ | CRUD, stock management |
| Customers | ✅ | CRUD, points system |
| Sales (POS) | ✅ | Cart, payment, print |
| Sales History | ✅ | Filter, detail view |
| Suppliers | ✅ | CRUD |
| Digital Products | ✅ | PPOB, categories |
| Reports | ✅ | Profit/Loss, filters |
| Hutang/Piutang | ✅ | Management, WhatsApp |
| Expenses | ✅ | Tracking, categories |
| Stock History | ✅ | In/out tracking |
| Low Stock | ✅ | Alert system |
| Backup/Restore | ✅ | JSON export/import |
| Bluetooth Printer | ✅ | 58mm thermal |
| Settings | ✅ | Shop profile, payment |

---

**Version**: 1.0.0 | **Last Updated**: March 2026 | **Kotlin**: 2.3.10 | **Compose BOM**: 2025.01.00
