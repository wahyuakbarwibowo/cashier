package com.wahyuakbarwibowo.aminmartkasir.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val jakartaZone = ZoneId.of("Asia/Jakarta")

fun expiryDate(expiresAt: String?): String = expiresAt?.let {
    Instant.parse(it).atZone(jakartaZone).toLocalDate().toString()
}.orEmpty()

fun expiryFromDate(date: String, existing: String?): String? {
    if (date.isBlank()) return null
    val parsed = LocalDate.parse(date)
    if (existing != null && expiryDate(existing) == parsed.toString()) return existing
    return parsed.plusDays(1).atStartOfDay(jakartaZone).toInstant().toString()
}

@Serializable
data class StoreFeatureFlag(
    @SerialName("store_id") val storeId: String,
    @SerialName("feature_key") val featureKey: String,
    val enabled: Boolean,
    @SerialName("expires_at") val expiresAt: String? = null
) {
    fun isActive(now: Instant = Instant.now()): Boolean = enabled &&
        (expiresAt == null || runCatching { Instant.parse(expiresAt).isAfter(now) }.getOrDefault(false))
}
