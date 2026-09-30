package com.wahyuakbarwibowo.aminmartkasir.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("store_id") val storeId: String? = null,
    val email: String,
    @SerialName("full_name") val fullName: String = "",
    val role: UserRole,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_approved") val isApproved: Boolean = true
)
