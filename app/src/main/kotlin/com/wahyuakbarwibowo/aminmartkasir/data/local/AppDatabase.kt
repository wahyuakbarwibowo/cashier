package com.wahyuakbarwibowo.aminmartkasir.data.local

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ViewModelFactory

@Database(
    entities = [
        ProductEntity::class,
        ProductVariantEntity::class,
        CustomerEntity::class,
        PaymentMethodEntity::class,
        ShopProfileEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        ReceivableEntity::class,
        PayableEntity::class,
        PhoneHistoryEntity::class,
        DigitalProductEntity::class,
        DigitalCategoryEntity::class,
        ExpenseEntity::class,
        CustomerPointsHistoryEntity::class,
        StockHistoryEntity::class,
        ShiftEntity::class
    ],
    version = 15,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productVariantDao(): ProductVariantDao
    abstract fun customerDao(): CustomerDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun shopProfileDao(): ShopProfileDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun payableDao(): PayableDao
    abstract fun phoneHistoryDao(): PhoneHistoryDao
    abstract fun digitalProductDao(): DigitalProductDao
    abstract fun digitalCategoryDao(): DigitalCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerPointsHistoryDao(): CustomerPointsHistoryDao
    abstract fun stockHistoryDao(): StockHistoryDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        private val instances = mutableMapOf<String, AppDatabase>()

        // Add new migrations here when bumping version. Never remove old ones.
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_createdAt ON sales (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_phone_history_createdAt ON phone_history (createdAt)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shifts (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        openedAt TEXT NOT NULL,
                        closedAt TEXT,
                        openingCash REAL NOT NULL DEFAULT 0,
                        countedCash REAL,
                        totalSales REAL NOT NULL DEFAULT 0,
                        totalExpenses REAL NOT NULL DEFAULT 0,
                        expectedCash REAL,
                        difference REAL,
                        note TEXT,
                        status TEXT NOT NULL DEFAULT 'open'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN syncUpdatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_remoteId ON products (remoteId)")
            }
        }

        // Dashboard, shift, dan laba rugi menjumlahkan expenses/purchases per rentang createdAt.
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_createdAt ON expenses (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_createdAt ON purchases (createdAt)")
            }
        }

        // Sync stok berbasis delta: syncedStock = stok terakhir yang disepakati dengan cloud.
        // Produk yang belum pernah di-upload mulai dari basis 0 (seluruh stok = delta);
        // produk tertunda yang sudah punya remoteId basisnya tidak diketahui (NULL).
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN syncedStock INTEGER")
                db.execSQL("UPDATE products SET syncedStock = stock WHERE isSynced = 1 AND remoteId IS NOT NULL")
                db.execSQL("UPDATE products SET syncedStock = 0 WHERE remoteId IS NULL")
            }
        }

        // Hapus pelanggan/supplier dulu ikut menghapus histori piutang/hutang yang sudah lunas
        // (FK CASCADE). SQLite tidak bisa ALTER foreign key, jadi tabel dibuat ulang dengan SET NULL.
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `receivables_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `saleId` INTEGER, `customerId` INTEGER, `amount` REAL NOT NULL, `paidAmount` REAL NOT NULL, `dueDate` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `createdAt` TEXT, FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("INSERT INTO receivables_new (id, saleId, customerId, amount, paidAmount, dueDate, status, notes, createdAt) SELECT id, saleId, customerId, amount, paidAmount, dueDate, status, notes, createdAt FROM receivables")
                db.execSQL("DROP TABLE receivables")
                db.execSQL("ALTER TABLE receivables_new RENAME TO receivables")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receivables_saleId` ON `receivables` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_receivables_customerId` ON `receivables` (`customerId`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `payables_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `purchaseId` INTEGER, `supplierId` INTEGER, `supplier` TEXT, `amount` REAL NOT NULL, `paidAmount` REAL NOT NULL, `dueDate` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `createdAt` TEXT, FOREIGN KEY(`purchaseId`) REFERENCES `purchases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
                db.execSQL("INSERT INTO payables_new (id, purchaseId, supplierId, supplier, amount, paidAmount, dueDate, status, notes, createdAt) SELECT id, purchaseId, supplierId, supplier, amount, paidAmount, dueDate, status, notes, createdAt FROM payables")
                db.execSQL("DROP TABLE payables")
                db.execSQL("ALTER TABLE payables_new RENAME TO payables")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payables_purchaseId` ON `payables` (`purchaseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payables_supplierId` ON `payables` (`supplierId`)")
            }
        }

        fun getDatabase(context: Context, storeId: String): AppDatabase {
            val name = TenantDatabaseFiles.name(storeId)
            return synchronized(this) {
                instances.getOrPut(name) {
                    check(!TenantDatabaseFiles.needsChoice(context, storeId)) {
                        "Pilih toko untuk data lokal lama sebelum membuka kasir."
                    }
                    Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    name
                    )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    // Versi 1-8 mendahului histori Migration object (dulu pakai fallbackToDestructiveMigration
                    // tanpa batas); tidak ada schema snapshot untuk menulis migrasi asli, jadi scope destructive
                    // fallback hanya untuk versi lama ini. Versi 9+ tetap wajib Migration object di atas.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8)
                    .build()
                }
            }
        }

        fun getViewModelFactory(context: Context, storeId: String): ViewModelProvider.Factory {
            val database = getDatabase(context, storeId)
            return ViewModelFactory(database, context.applicationContext, storeId)
        }
    }
}
