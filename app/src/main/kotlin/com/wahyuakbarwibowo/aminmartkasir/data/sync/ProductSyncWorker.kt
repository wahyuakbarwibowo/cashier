package com.wahyuakbarwibowo.aminmartkasir.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.TenantDatabaseFiles
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.FeatureNotAvailableException
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductSyncRepository
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

internal fun canAutoSync(boundStoreId: String, profileStoreId: String?, isOffline: Boolean): Boolean =
    profileStoreId == boundStoreId && !isOffline

object ProductSyncScheduler {
    private const val PREFS = "product_auto_sync"
    private const val STORE_ID = "store_id"
    private const val LAST_SYNC = "last_sync:"
    private val _lastSyncedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastSyncedAt: StateFlow<Map<String, Long>> = _lastSyncedAt

    fun init(context: Context) {
        _lastSyncedAt.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
            .mapNotNull { (key, value) ->
                if (key.startsWith(LAST_SYNC) && value is Long) key.removePrefix(LAST_SYNC) to value else null
            }.toMap()
    }

    fun enqueue(context: Context, storeId: String) {
        if (TenantDatabaseFiles.isPlatformStore(storeId)) return
        val request = OneTimeWorkRequestBuilder<ProductSyncWorker>()
            .setInputData(Data.Builder().putString(STORE_ID, storeId).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("product-sync:$storeId", ExistingWorkPolicy.REPLACE, request)
    }

    fun markSynced(context: Context, storeId: String) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("$LAST_SYNC$storeId", now).apply()
        _lastSyncedAt.update { it + (storeId to now) }
    }
}

class ProductSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val storeId = inputData.getString("store_id") ?: return Result.success()
        if (TenantDatabaseFiles.needsChoice(applicationContext, storeId)) return Result.success()
        val auth = withTimeoutOrNull(30_000) {
            AuthManager.state.first { state ->
                state is AuthManager.UiState.SignedOut ||
                    state is AuthManager.UiState.Authenticated && !state.isOffline
            }
        } ?: return Result.retry()
        val authenticated = auth as? AuthManager.UiState.Authenticated ?: return Result.success()
        if (!canAutoSync(storeId, authenticated.profile.storeId, authenticated.isOffline)) return Result.success()

        return try {
            ProductSyncRepository(AppDatabase.getDatabase(applicationContext, storeId).productDao()).sync(storeId)
            ProductSyncScheduler.markSynced(applicationContext, storeId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: FeatureNotAvailableException) {
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
