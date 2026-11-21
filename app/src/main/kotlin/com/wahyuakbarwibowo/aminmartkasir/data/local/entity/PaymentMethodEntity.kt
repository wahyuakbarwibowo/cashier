package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    /** Uang masuk laci kas; dipakai untuk menghitung kas seharusnya saat tutup shift. */
    @ColumnInfo(defaultValue = "1")
    val isCash: Boolean = true
)
