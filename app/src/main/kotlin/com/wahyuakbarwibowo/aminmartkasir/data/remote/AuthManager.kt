package com.wahyuakbarwibowo.aminmartkasir.data.remote

import android.content.Context
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException

object AuthManager {

    sealed interface UiState {
        data object Loading : UiState
        data object SignedOut : UiState
        data class Authenticated(val profile: UserProfile, val isOffline: Boolean = false) : UiState
    }

    private const val PREFS_NAME = "auth_session"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_CACHED_PROFILE = "cached_profile"
    private const val RESET_PASSWORD_URL = "https://wahyuakbarwibowo.github.io/cashier/reset-password.html"
    const val PENDING_MESSAGE = "Akun menunggu persetujuan admin."

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private lateinit var prefs: android.content.SharedPreferences
    private val json = Json

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedProfile = readCachedProfile()
        if (cachedProfile != null) _state.value = UiState.Authenticated(cachedProfile, isOffline = true)
        if (!SupabaseProvider.isConfigured) {
            if (cachedProfile == null) _state.value = UiState.SignedOut
            return
        }

        // Simpan refresh token setiap kali sesi diperbarui
        scope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    prefs.edit()
                        .putString(KEY_REFRESH_TOKEN, status.session.refreshToken)
                        .apply()
                }
            }
        }

        restoreSession()
    }

    /** Coba pulihkan sesi terakhir lewat refresh token tersimpan. */
    private fun restoreSession() {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (refreshToken == null) {
            if (readCachedProfile() == null) _state.value = UiState.SignedOut
            return
        }
        scope.launch {
            try {
                SupabaseProvider.client.auth.refreshSession(refreshToken)
                val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
                    ?: error("Sesi tidak valid")
                val profile = fetchProfile(userId)
                if (profile != null && profile.isActive && profile.isApproved) {
                    cacheProfile(profile)
                    _state.value = UiState.Authenticated(profile)
                } else {
                    clearLocalSession()
                    clearCachedProfile()
                    _state.value = UiState.SignedOut
                }
            } catch (e: IOException) {
                if (readCachedProfile() == null) _state.value = UiState.SignedOut
            } catch (_: Exception) {
                clearLocalSession()
                clearCachedProfile()
                _state.value = UiState.SignedOut
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            SupabaseProvider.client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val userId = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id
                ?: error("Sesi tidak valid")
            val profile = fetchProfile(userId)
                ?: run {
                    SupabaseProvider.client.auth.signOut()
                    clearLocalSession()
                    return Result.failure(IllegalStateException("Profil tidak ditemukan. Hubungi admin."))
                }
            if (!profile.isActive) {
                SupabaseProvider.client.auth.signOut()
                clearLocalSession()
                return Result.failure(IllegalStateException("Akun dinonaktifkan. Hubungi admin."))
            }
            if (!profile.isApproved) {
                SupabaseProvider.client.auth.signOut()
                clearLocalSession()
                return Result.failure(IllegalStateException(PENDING_MESSAGE))
            }
            _state.value = UiState.Authenticated(profile)
            cacheProfile(profile)
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Email atau password salah."))
        } catch (e: IOException) {
            Result.failure(Exception("Tidak ada koneksi internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Daftar akun baru; akun berstatus pending sampai disetujui. */
    suspend fun signUp(fullName: String, email: String, password: String): Result<Unit> {
        return try {
            SupabaseProvider.client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject { put("full_name", fullName.trim()) }
            }
            // Jangan biarkan akun pending tetap login
            SupabaseProvider.client.auth.signOut()
            clearLocalSession()
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception(signUpErrorMessage(e.error)))
        } catch (e: IOException) {
            Result.failure(Exception("Tidak ada koneksi internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Pesan Supabase berbahasa Inggris; terjemahkan yang umum agar mudah dipahami
    private fun signUpErrorMessage(error: String): String = when {
        error.contains("already registered", true) -> "Email sudah terdaftar. Silakan masuk."
        error.contains("rate limit", true) -> "Terlalu banyak percobaan. Coba lagi beberapa menit lagi."
        error.contains("invalid", true) && error.contains("email", true) -> "Format email tidak valid."
        error.contains("password", true) -> "Password terlalu lemah. Gunakan minimal 6 karakter."
        else -> "Pendaftaran gagal. Coba lagi."
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            SupabaseProvider.client.auth.resetPasswordForEmail(email.trim(), redirectUrl = RESET_PASSWORD_URL)
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception(signUpErrorMessage(e.error)))
        } catch (e: IOException) {
            Result.failure(Exception("Tidak ada koneksi internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            SupabaseProvider.client.auth.signOut()
        } catch (_: Exception) {
            // abaikan; tetap bersihkan sesi lokal
        }
        clearLocalSession()
        clearCachedProfile()
        _state.value = UiState.SignedOut
    }

    private suspend fun fetchProfile(userId: String): UserProfile? {
        return SupabaseProvider.client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeList<UserProfile>()
            .firstOrNull()
    }

    private fun clearLocalSession() {
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    private fun cacheProfile(profile: UserProfile) {
        prefs.edit().putString(KEY_CACHED_PROFILE, json.encodeToString<UserProfile>(profile)).apply()
    }

    private fun readCachedProfile(): UserProfile? = runCatching {
        prefs.getString(KEY_CACHED_PROFILE, null)?.let { json.decodeFromString<UserProfile>(it) }
    }.getOrNull()

    private fun clearCachedProfile() {
        prefs.edit().remove(KEY_CACHED_PROFILE).apply()
    }
}
