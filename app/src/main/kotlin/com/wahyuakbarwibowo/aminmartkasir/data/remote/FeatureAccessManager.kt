package com.wahyuakbarwibowo.aminmartkasir.data.remote

import android.content.Context
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.StoreFeatureFlag
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

enum class PaidFeature(val key: String, val label: String) {
    PRODUCT_SYNC("cloud_product_sync", "Sinkronisasi cloud"),
    CLOUD_BACKUP("cloud_backup", "Backup cloud")
}

fun interface FeatureAccess {
    suspend fun requireFeature(storeId: String, feature: PaidFeature)
}

object FeatureAccessManager : FeatureAccess {
    private const val PREFS_NAME = "store_feature_flags"
    private lateinit var prefs: android.content.SharedPreferences
    private val json = Json

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun requireFeature(storeId: String, feature: PaidFeature) {
        val cached = readCached(storeId)
        if (SupabaseProvider.isConfigured) {
            try {
                val grants = SupabaseProvider.client.postgrest.from("store_feature_flags")
                    .select { filter { eq("store_id", storeId) } }
                    .decodeList<StoreFeatureFlag>()
                writeCached(storeId, grants)
                if (grants.any { it.featureKey == feature.key && it.isActive() }) return
                throw FeatureNotAvailableException(feature)
            } catch (e: FeatureNotAvailableException) {
                throw e
            } catch (e: IOException) {
                if (cached.any { it.featureKey == feature.key && it.isActive() }) return
                throw e
            }
        }
        if (cached.none { it.featureKey == feature.key && it.isActive() }) {
            throw FeatureNotAvailableException(feature)
        }
    }

    private fun readCached(storeId: String): List<StoreFeatureFlag> = runCatching {
        prefs.getString(storeId, null)?.let { json.decodeFromString<List<StoreFeatureFlag>>(it) }
    }.getOrNull().orEmpty()

    private fun writeCached(storeId: String, grants: List<StoreFeatureFlag>) {
        prefs.edit().putString(storeId, json.encodeToString(grants)).apply()
    }
}

class FeatureNotAvailableException(feature: PaidFeature) :
    IllegalStateException("${feature.label} memerlukan paket Pro yang aktif.")
