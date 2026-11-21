package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val amount: Double = 0.0,
    val notes: String? = null,
    val createdAt: String? = null
)

/** Kategori pengeluaran otomatis dari pembelian stok ke supplier. */
const val STOCK_PURCHASE_CATEGORY = "Pembelian Stok"
