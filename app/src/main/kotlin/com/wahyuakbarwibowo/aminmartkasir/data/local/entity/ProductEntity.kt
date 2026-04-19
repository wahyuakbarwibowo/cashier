package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String? = null,
    val name: String,
    val purchasePrice: Double = 0.0,
    val purchasePackagePrice: Double = 0.0,
    val purchasePackageQty: Int = 0,
    val sellingPrice: Double = 0.0,
    val packagePrice: Double = 0.0,
    val packageQty: Int = 0,
    val discount: Double = 0.0,
    val stock: Int = 0,
    val lowStockThreshold: Int = 5,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
