package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_history",
    indices = [Index("productId"), Index("createdAt")]
)
data class StockHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val changeQty: Int,
    val stockBefore: Int,
    val stockAfter: Int,
    val reason: String,
    val createdAt: String
)

