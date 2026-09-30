package com.wahyuakbarwibowo.aminmartkasir.data.remote

import com.wahyuakbarwibowo.aminmartkasir.BuildConfig
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserProfile
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserRole
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Kelola akun pegawai & persetujuan akun. Hak akses dijaga RLS + trigger di Supabase. */
object AccountManager {

    private val _pendingCount = MutableStateFlow(0)
    /** Jumlah akun yang menunggu persetujuan, untuk badge menu. */
    val pendingCount: StateFlow<Int> = _pendingCount

    private val profiles get() = SupabaseProvider.client.postgrest.from("profiles")

    /** RLS otomatis membatasi: super admin lihat semua, admin hanya tokonya. */
    suspend fun listProfiles(): List<UserProfile> {
        val list = profiles.select { order("created_at", Order.DESCENDING) }.decodeList<UserProfile>()
        _pendingCount.value = list.count { it.isPending }
        return list
    }

    suspend fun refreshPendingCount() {
        runCatching { listProfiles() }
    }

    suspend fun approve(userId: String, storeId: String?, role: UserRole) {
        profiles.update({
            set("store_id", storeId)
            set("role", role)
            set("is_approved", true)
        }) { filter { eq("id", userId) } }
    }

    suspend fun setActive(userId: String, active: Boolean) {
        profiles.update({ set("is_active", active) }) { filter { eq("id", userId) } }
    }

    /**
     * Buat akun kasir untuk toko [storeId] lalu langsung setujui.
     * Pakai klien sementara tanpa penyimpanan sesi supaya sesi admin tidak tertimpa.
     */
    suspend fun createStaff(fullName: String, email: String, password: String, storeId: String) {
        val temp = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth) {
                autoLoadFromStorage = false
                autoSaveToStorage = false
                alwaysAutoRefresh = false
            }
        }
        try {
            val user = temp.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName.trim())
                    put("store_id", storeId)
                }
            } ?: temp.auth.currentUserOrNull()
            val userId = user?.id ?: error("Akun gagal dibuat")
            approve(userId, storeId, UserRole.KASIR)
        } finally {
            runCatching { temp.auth.signOut() }
            temp.close()
        }
    }
}

val UserProfile.isPending: Boolean get() = !isApproved && isActive
