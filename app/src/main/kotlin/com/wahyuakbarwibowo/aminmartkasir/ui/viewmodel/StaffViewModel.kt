package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AccountManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.Store
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserProfile
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserRole
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffUiState(
    val profiles: List<UserProfile> = emptyList(),
    val stores: List<Store> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class StaffViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StaffUiState())
    val uiState: StateFlow<StaffUiState> = _uiState

    val me: UserProfile? = (AuthManager.state.value as? AuthManager.UiState.Authenticated)?.profile
    val isSuperAdmin get() = me?.role == UserRole.SUPER_ADMIN

    init {
        load()
    }

    fun load() = run("Gagal memuat akun") {}

    fun approve(user: UserProfile, storeId: String?, role: UserRole) =
        run("Gagal menyetujui", "${user.displayName} disetujui") {
            AccountManager.approve(user.id, storeId ?: me?.storeId, role)
        }

    fun setActive(user: UserProfile, active: Boolean) =
        run("Gagal mengubah status", if (active) null else "${user.displayName} dinonaktifkan") {
            AccountManager.setActive(user.id, active)
        }

    fun createStaff(fullName: String, email: String, password: String, storeId: String?) =
        run("Gagal menambah pegawai", "Akun $email dibuat") {
            AccountManager.createStaff(fullName, email, password, storeId ?: me?.storeId ?: error("Toko belum dipilih"))
        }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun run(errorPrefix: String, success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val msg = try {
                block()
                val profiles = AccountManager.listProfiles()
                val stores = SupabaseProvider.client.postgrest.from("stores").select().decodeList<Store>()
                _uiState.update { it.copy(profiles = profiles, stores = stores) }
                success
            } catch (e: Exception) {
                "$errorPrefix: ${e.message}"
            }
            _uiState.update { it.copy(isLoading = false, message = msg) }
        }
    }
}

val UserProfile.displayName: String get() = fullName.ifBlank { email }
