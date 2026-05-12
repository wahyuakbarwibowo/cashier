package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class ProductVariantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stock: Int = 0
)
