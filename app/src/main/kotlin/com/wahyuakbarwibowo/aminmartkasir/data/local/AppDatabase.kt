package com.wahyuakbarwibowo.aminmartkasir.data.local

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.converter.Converters
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
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
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
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_database"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                    // Versi 1-8 mendahului histori Migration object (dulu pakai fallbackToDestructiveMigration
                    // tanpa batas); tidak ada schema snapshot untuk menulis migrasi asli, jadi scope destructive
                    // fallback hanya untuk versi lama ini. Versi 9+ tetap wajib Migration object di atas.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getViewModelFactory(context: Context): ViewModelProvider.Factory {
            val database = getDatabase(context)
            return ViewModelFactory(database)
        }
    }
}
