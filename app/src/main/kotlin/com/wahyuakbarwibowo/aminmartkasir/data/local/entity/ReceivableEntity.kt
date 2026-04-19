package com.wahyuakbarwibowo.aminmartkasir.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receivables",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("customerId")]
)
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long? = null,
    val customerId: Long? = null,
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: String? = null,
    val status: String = "pending",
    val notes: String? = null,
    val createdAt: String? = null
)
