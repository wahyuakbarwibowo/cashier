package com.wahyuakbarwibowo.aminmartkasir.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudProduct(
    @SerialName("remote_id") val remoteId: String,
    @SerialName("store_id") val storeId: String,
    val code: String? = null,
    val name: String,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("purchase_package_price") val purchasePackagePrice: Double,
    @SerialName("purchase_package_qty") val purchasePackageQty: Int,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("package_price") val packagePrice: Double,
    @SerialName("package_qty") val packageQty: Int,
    val discount: Double,
    val stock: Int,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("sync_updated_at") val syncUpdatedAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean
)
