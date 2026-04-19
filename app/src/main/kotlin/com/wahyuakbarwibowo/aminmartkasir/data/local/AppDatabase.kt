package com.wahyuakbarwibowo.aminmartkasir.data.local

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wahyuakbarwibowo.aminmartkasir.data.local.converter.Converters
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ViewModelFactory

@Database(
    entities = [
        ProductEntity::class,
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
        StockHistoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
