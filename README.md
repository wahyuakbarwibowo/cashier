# 🛒 Aminmart Cashier (Retail & PPOB)

**Aminmart Cashier** adalah aplikasi Point of Sales (POS) berbasis **Kotlin** & **Jetpack Compose** yang intuitif, cepat, dan modern. Dirancang khusus untuk memenuhi kebutuhan toko retail, minimarket, serta agen pulsa & PPOB dalam satu platform yang terintegrasi.

---

### ✨ Fitur Utama
*   **Retail POS**: Manajemen stok barang, barcode scanner (via kamera), transaksi kasir kilat.
*   **PPOB & Layanan Digital**: Transaksi Pulsa, PLN, E-Wallet, BPJS, Transfer Bank, dan Game.
*   **📉 Laporan Laba Rugi (Profit & Loss)**: Analisis keuntungan bersih yang sudah terintegrasi dengan omset penjualan, HPP, pengeluaran, serta pantauan saldo **Hutang & Piutang**.
*   **🤝 Manajemen Hutang & Piutang**: Lacak piutang pelanggan dan hutang ke supplier secara mendetail dengan sistem jatuh tempo dan fitur **Tagih via WhatsApp**.
*   **💸 Manajemen Pengeluaran**: Pencatatan biaya operasional harian (listrik, sewa, gaji) untuk perhitungan laba bersih yang akurat.
*   **✅ Validasi Stok Pintar**: Sistem otomatis mencegah transaksi jika stok barang tidak mencukupi, lengkap dengan peringatan visual di keranjang.
*   **📦 Kalkulasi Otomatis**: Input harga paket/dus dan isi, sistem otomatis menghitung harga modal & jual satuan di manajemen produk.
*   **⚠️ Sistem Alert Stok Rendah**: Notifikasi visual dan daftar khusus untuk produk yang stoknya di bawah ambang batas (Critical Stock).
*   **🌸 Premium UI & Interaktif**: Desain modern yang interaktif; klik item laporan untuk melihat detail transaksi atau produk secara instan.
*   **📅 Format Tanggal Standar**: Format waktu Indonesia yang mudah dibaca (12 Februari 2026, 21:00) di seluruh riwayat.
*   **Manajemen Kategori Dinamis**: Tambah, edit, dan hapus kategori produk digital sesuka hati (PULSA, PDAM, VOUCHER, dll).
*   **Optimasi Struk Thermal**: Cetak struk 58mm yang rapi untuk semua jenis transaksi (Retail & Digital).
*   **📜 Riwayat Transaksi Digital**: Navigasi khusus untuk melihat detail transaksi digital masa lalu.
*   **✏️ Edit Transaksi Digital**: Fitur untuk mengubah data transaksi digital yang sudah tersimpan jika terjadi kesalahan input.
*   **📅 Input Tanggal Transaksi**: Keleluasaan untuk menentukan atau merubah tanggal transaksi (Backdate) pada modul penjualan dan produk digital.
*   **🔄 Reload & Refresh**: Tombol penyegaran data instan di layar transaksi untuk memastikan data stok dan riwayat selalu aktual tanpa harus keluar layar.
*   **⌨️ Enhanced UX (Keyboard Avoidance)**: Optimasi seluruh layar input agar bidang isian tidak tertutup oleh keyboard virtual, memastikan pengalaman mengetik yang nyaman.
*   **📅 Filter Rentang Tanggal**: Filter laporan dan riwayat berdasarkan periode waktu tertentu (Hari, Minggu, Bulan, Tahun).
*   **💾 Backup & Restore**: Amankan data lokal Anda dengan fitur ekspor/impor database.

---

## 🛠️ Tech Stack
*   **Language**: Kotlin 2.3.10
*   **UI Toolkit**: Jetpack Compose (Compose BOM 2024.12.01)
*   **Database**: Room 2.8.4 (SQLite)
*   **Architecture**: MVVM
*   **Async**: Kotlin Coroutines 1.10.1
*   **Navigation**: Navigation Compose 2.8.5
*   **Printing**: Android Bluetooth API (58mm Thermal Printer)
*   **Annotation Processing**: KSP 2.3.5

---

## 🚀 Cara Menjalankan Project

### Prerequisites
- **Android Studio** (Meerkat atau lebih baru)
- **JDK 17** atau lebih baru
- **Android SDK** (API 35)
- **Device Android** (min. Android 7.0 / API 24) atau Emulator

---

### 1. Clone Repository
```bash
git clone https://github.com/wahyuakbarwibowo/rn-cashier-app.git
cd aminmart/app-cashier
```

---

### 2. Build & Run via Android Studio

1.  Buka **Android Studio** → **Open Project** → Pilih folder project
2.  Tunggu Gradle sync selesai
3.  Pilih device yang terhubung atau emulator
4.  Klik tombol **Run** (▶️) atau tekan `Shift + F10`

---

### 3. Build & Run via Command Line (Makefile)

Project ini dilengkapi dengan **Makefile** untuk streamline development workflow:

```bash
# Clean build
make clean

# Build debug APK
make build

# Build + Install ke device
make install

# Build + Install + Run (quick dev)
make dev

# View logs
make logs

# Run tests
make test
```

---

### 4. Build & Run via Command Line (Gradle/ADB)

#### a. Connect Device via USB
- Enable **USB Debugging** di Developer Options pada device Android
- Connect device via USB ke komputer

#### b. Verify Device Connection
```bash
adb devices
```
Output:
```
List of devices attached
ABC123XYZ    device
```

#### c. Build Debug APK
```bash
# Di root folder project
./gradlew assembleDebug
```
APK akan dihasilkan di: `app/build/outputs/apk/debug/app-debug.apk`

#### d. Install APK ke Device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### e. Run Aplikasi
```bash
# Launch aplikasi
adb shell am start -n com.wahyuakbarwibowo.aminmartkasir/.MainActivity

# Atau jika sudah terinstall, buka dari app drawer
```

#### f. Quick Command (Build + Install + Run)
```bash
./gradlew installDebug && adb shell am start -n com.wahyuakbarwibowo.aminmartkasir/.MainActivity
```

Atau gunakan Makefile:
```bash
make dev
```

---

## 🐛 Local Debugging dengan ADB

### View Logs (Logcat)
```bash
# Filter logs berdasarkan tag aplikasi
adb logcat -s "AminmartKasir"

# Filter berdasarkan package name
adb logcat --pid=$(adb shell pidof -s com.wahyuakbarwibowo.aminmartkasir)

# Clear logs
adb logcat -c

# Save logs ke file
adb logcat -d > logs.txt
```

### Debug Database (Room)
```bash
# Pull database dari device
adb pull /data/data/com.wahyuakbarwibowo.aminmartkasir/databases/aminmart_kasir.db

# Buka dengan DB Browser for SQLite atau Android Studio Database Inspector
```

### Screen Capture & Recording
```bash
# Screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Screen record (max 3 menit)
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4
```

### Network Debugging
```bash
# Forward TCP port untuk network inspection
adb reverse tcp:8080 tcp:8080

# Monitor network traffic
adb shell dumpsys netstats
```

### Debug Layout & UI
```bash
# Show layout bounds (enable di Developer Options juga bisa)
adb shell setprop debug.layout true

# Dump window hierarchy untuk UI testing
adb shell dumpsys window windows
```

### Uninstall & Clear Data
```bash
# Uninstall aplikasi
adb uninstall com.wahyuakbarwibowo.aminmartkasir

# Clear data (tanpa uninstall)
adb shell pm clear com.wahyuakbarwibowo.aminmartkasir

# Clear cache only
adb shell pm trim-caches com.wahyuakbarwibowo.aminmartkasir
```

---

## 🔧 Tips Debugging

| Issue | Solution |
|-------|----------|
| Device tidak terdeteksi | `adb kill-server` → `adb start-server` → reconnect USB |
| Permission denied | Enable USB Debugging + "Install via USB" di Developer Options |
| Build gagal | `./gradlew clean` → sync Gradle di Android Studio |
| App crash on startup | Cek `adb logcat` untuk error message |
| Bluetooth tidak connect | Pastikan izin Bluetooth dan Location sudah granted |
| Database corrupt | `adb shell pm clear com.wahyuakbarwibowo.aminmartkasir` |

---

## 🖨️ Panduan Cetak Struk (Thermal Printer)
Aplikasi ini dioptimalkan untuk kertas thermal ukuran **58mm**.
1.  Pastikan Printer Bluetooth sudah terhubung dengan perangkat Android Anda
2.  Setelah transaksi berhasil, klik tombol **"Cetak Struk"**
3.  Pilih Printer Bluetooth yang sesuai pada dialog pairing
4.  Teks dan layout akan otomatis menyesuaikan lebar kertas thermal

---

## 📂 Struktur Database (Digital Services)
Sistem digital menggunakan tabel dinamis:
*   `digital_categories`: Menyimpan daftar kategori (PULSA, PLN, dll).
*   `digital_product_master`: Template produk per kategori & provider.
*   `phone_history`: Log transaksi digital.
*   `receivables`: Data piutang pelanggan.
*   `payables`: Data hutang ke supplier.
*   `expenses`: Data pengeluaran operasional.

---

## 📁 Project Structure
```
app-cashier/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/com/wahyuakbarwibowo/aminmartkasir/
│   │       │   ├── ui/          # Compose UI screens
│   │       │   ├── data/        # Room entities & DAOs
│   │       │   ├── viewmodel/   # MVVM ViewModels
│   │       │   └── MainActivity.kt
│   │       └── res/             # Resources (strings, themes, etc.)
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── Makefile
```

---

Developed with ❤️ by **Wahyu Akbar Wibowo**
