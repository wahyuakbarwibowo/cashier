package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Shift kasir (buka/tutup laci). expectedCash = openingCash + totalSales - totalExpenses.
 * difference = countedCash - expectedCash (selisih laci saat tutup).
 */
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val openedAt: String,
    val closedAt: String? = null,
    val openingCash: Double = 0.0,
    val countedCash: Double? = null,
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val expectedCash: Double? = null,
    val difference: Double? = null,
    val note: String? = null,
    val status: String = "open" // "open" | "closed"
)
