package com.wahyuakbarwibowo.aminmartkasir.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val db = "migration-test"
    private val migrations = arrayOf(
        AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12,
        AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15,
        AppDatabase.MIGRATION_15_16
    )

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrateFrom10ToLatestMatchesSchema() {
        helper.createDatabase(db, 10).close()
        helper.runMigrationsAndValidate(db, 16, true, *migrations).close()
    }

    @Test
    fun migrateFrom13KeepsDataAndFillsNewColumns() {
        helper.createDatabase(db, 13).apply {
            val product = "INSERT INTO products (id, name, purchasePrice, purchasePackagePrice, purchasePackageQty, sellingPrice, packagePrice, packageQty, discount, stock, lowStockThreshold, syncUpdatedAt, isSynced, isDeleted, remoteId) VALUES "
            execSQL("$product(1, 'Synced', 0, 0, 0, 0, 0, 0, 0, 7, 5, 1, 1, 0, 'r1')")
            execSQL("$product(2, 'Local', 0, 0, 0, 0, 0, 0, 0, 4, 5, 1, 0, 0, NULL)")
            execSQL("$product(3, 'Pending', 0, 0, 0, 0, 0, 0, 0, 9, 5, 1, 0, 0, 'r3')")
            execSQL("INSERT INTO customers (id, name, points) VALUES (1, 'Budi', 0)")
            execSQL("INSERT INTO receivables (id, customerId, amount, paidAmount, status) VALUES (1, 1, 5000, 5000, 'paid')")
            execSQL("INSERT INTO payment_methods (id, name, sortOrder) VALUES (1, 'Tunai', 0), (2, 'QRIS BCA', 1)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(db, 16, true, *migrations)

        assertEquals(listOf(7, 0, null), migrated.ints("SELECT syncedStock FROM products ORDER BY id"))
        assertEquals(listOf(1, 0), migrated.ints("SELECT isCash FROM payment_methods ORDER BY id"))

        migrated.execSQL("PRAGMA foreign_keys = ON")
        migrated.execSQL("DELETE FROM customers WHERE id = 1")
        assertEquals(listOf(null), migrated.ints("SELECT customerId FROM receivables"))
        assertTrue(migrated.ints("SELECT amount FROM receivables").single() == 5000)
        migrated.close()
    }

    private fun SupportSQLiteDatabase.ints(sql: String): List<Int?> = query(sql).use { c ->
        buildList { while (c.moveToNext()) add(if (c.isNull(0)) null else c.getInt(0)) }
    }
}
