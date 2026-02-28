package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,
    val paymentMethodId: Long? = null,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val change: Double = 0.0,
    val pointsEarned: Int = 0,
    val pointsRedeemed: Int = 0,
    val createdAt: String? = null
)
