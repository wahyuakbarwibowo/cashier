package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.Store
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.StoreFeatureFlag
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TenantUiState(
    val stores: List<Store> = emptyList(),
    val featureStoreId: String? = null,
    val featureFlags: List<StoreFeatureFlag>? = null,
    val isLoading: Boolean = true,
    val message: String? = null
)

// Kelola tenant (toko) langsung ke Supabase; hak akses dijaga RLS (hanya super_admin boleh tulis).
class TenantViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState

    private val table get() = SupabaseProvider.client.postgrest.from("stores")

    init {
        load()
    }

    fun load() = run("Gagal memuat toko") {
        val stores = table.select { order("created_at", Order.ASCENDING) }.decodeList<Store>()
        _uiState.update { it.copy(stores = stores) }
    }

    fun save(store: Store) = run("Gagal menyimpan toko", "Toko disimpan") {
        if (store.id == null) {
            table.insert(store)
        } else {
            table.update({
                set("name", store.name)
                set("address", store.address)
                set("phone", store.phone)
            }) { filter { eq("id", store.id) } }
        }
        reload()
    }

    fun toggleActive(store: Store) = run("Gagal mengubah status") {
        table.update({ set("is_active", !store.isActive) }) { filter { eq("id", store.id!!) } }
        reload()
    }

    fun delete(store: Store) = run("Gagal menghapus toko", "Toko \"${store.name}\" dihapus") {
        table.delete { filter { eq("id", store.id!!) } }
        reload()
    }

    fun loadFeatures(storeId: String) {
        _uiState.update { it.copy(featureStoreId = storeId, featureFlags = null) }
        run("Gagal memuat paket Pro") {
            val flags = SupabaseProvider.client.postgrest.from("store_feature_flags")
                .select { filter { eq("store_id", storeId) } }
                .decodeList<StoreFeatureFlag>()
            _uiState.update { if (it.featureStoreId == storeId) it.copy(featureFlags = flags) else it }
        }
    }

    fun saveFeatures(flags: List<StoreFeatureFlag>, onSaved: () -> Unit) =
        run("Gagal menyimpan paket Pro", "Paket Pro disimpan") {
            SupabaseProvider.client.postgrest.from("store_feature_flags")
                .upsert(flags) { onConflict = "store_id,feature_key" }
            onSaved()
        }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private suspend fun reload() {
        val stores = table.select { order("created_at", Order.ASCENDING) }.decodeList<Store>()
        _uiState.update { it.copy(stores = stores) }
    }

    private fun run(errorPrefix: String, success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val msg = try {
                block()
                success
            } catch (e: Exception) {
                "$errorPrefix: ${e.message}"
            }
            _uiState.update { it.copy(isLoading = false, message = msg) }
        }
    }
}
