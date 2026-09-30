package com.wahyuakbarwibowo.aminmartkasir.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Store(
    val id: String? = null,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
