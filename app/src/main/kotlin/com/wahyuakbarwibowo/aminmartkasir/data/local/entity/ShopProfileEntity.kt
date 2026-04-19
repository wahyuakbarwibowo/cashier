package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String? = null,
    val footerNote: String? = null,
    val cashierName: String? = null,
    val phoneNumber: String? = null,
    val address: String? = null,
    val poinEnabled: Int = 0,
    val logoPath: String? = null
)
