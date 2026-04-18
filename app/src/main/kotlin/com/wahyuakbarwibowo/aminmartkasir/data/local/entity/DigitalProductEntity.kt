package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digital_products")
data class DigitalProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val provider: String,
    val name: String,
    val nominal: Double = 0.0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val sortOrder: Int = 0,
    val createdAt: String? = null
)
