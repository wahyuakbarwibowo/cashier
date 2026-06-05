package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phone_history",
    indices = [Index("createdAt")]
)
data class PhoneHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String = "PULSA",
    val phoneNumber: String? = null,
    val customerName: String? = null,
    val provider: String? = null,
    val senderName: String? = null,
    val receiverName: String? = null,
    val amount: Double = 0.0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val profit: Double = 0.0,
    val notes: String? = null,
    val paid: Double = 0.0,
    val createdAt: String? = null
)
