package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.remote.FeatureAccessManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.FeatureAccess
import com.wahyuakbarwibowo.aminmartkasir.data.remote.PaidFeature
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.CloudProduct
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ProductCloudDataSource {
    suspend fun findProduct(storeId: String, remoteId: String): CloudProduct?
    suspend fun upsertProduct(product: CloudProduct)
    suspend fun getProducts(storeId: String): List<CloudProduct>
}

class SupabaseProductCloudDataSource : ProductCloudDataSource {
    override suspend fun findProduct(storeId: String, remoteId: String): CloudProduct? =
        SupabaseProvider.client.postgrest.from("products")
            .select { filter {
                eq("store_id", storeId)
                eq("remote_id", remoteId)
            } }
            .decodeList<CloudProduct>()
            .firstOrNull()

    override suspend fun upsertProduct(product: CloudProduct) {
        SupabaseProvider.client.postgrest.from("products")
            .upsert(product) { onConflict = "remote_id" }
    }

    /** PostgREST memotong hasil di max_rows (1000), jadi ambil per halaman sampai habis. */
    override suspend fun getProducts(storeId: String): List<CloudProduct> {
        val all = mutableListOf<CloudProduct>()
        var from = 0L
        while (true) {
            val page = SupabaseProvider.client.postgrest.from("products")
                .select {
                    filter { eq("store_id", storeId) }
                    order("remote_id", Order.ASCENDING)
                    range(from, from + PAGE_SIZE - 1)
                }
                .decodeList<CloudProduct>()
            all += page
            if (page.size < PAGE_SIZE) return all
            from += PAGE_SIZE
        }
    }

    private companion object {
        // Harus <= max_rows server supaya halaman penuh tidak disangka halaman terakhir.
        const val PAGE_SIZE = 500L
    }
}

class ProductSyncRepository(
    private val productDao: ProductDao,
    private val cloud: ProductCloudDataSource = SupabaseProductCloudDataSource(),
    private val featureAccess: FeatureAccess = FeatureAccessManager
) {
    companion object {
        private val syncMutex = Mutex()
    }

    suspend fun sync(storeId: String): Int = syncMutex.withLock {
        featureAccess.requireFeature(storeId, PaidFeature.PRODUCT_SYNC)
        var pushed = 0
        for (local in productDao.getPendingSync()) {
            val product = local.copy(remoteId = local.remoteId ?: UUID.randomUUID().toString())
            if (product.remoteId != local.remoteId) productDao.update(product)
            val remoteId = requireNotNull(product.remoteId)
            val existingRemote = cloud.findProduct(storeId, remoteId)
            if (existingRemote != null && existingRemote.syncUpdatedAt > product.syncUpdatedAt) {
                applyRemote(existingRemote.toLocal(product.id), product.syncUpdatedAt, false)
                continue
            }
            cloud.upsertProduct(product.toCloud(storeId))
            productDao.markSynced(product.id, product.syncUpdatedAt)
            pushed++
        }

        cloud.getProducts(storeId).forEach { remoteProduct ->
            val local = productDao.getByRemoteId(remoteProduct.remoteId)
            if (local == null) {
                productDao.insertIfAbsent(remoteProduct.toLocal(0))
            } else if (local.isSynced && remoteProduct.syncUpdatedAt > local.syncUpdatedAt) {
                applyRemote(remoteProduct.toLocal(local.id), local.syncUpdatedAt, true)
            }
        }
        pushed
    }

    private fun ProductEntity.toCloud(storeId: String) = CloudProduct(
        remoteId = requireNotNull(remoteId), storeId = storeId, code = code, name = name,
        purchasePrice = purchasePrice, purchasePackagePrice = purchasePackagePrice,
        purchasePackageQty = purchasePackageQty, sellingPrice = sellingPrice,
        packagePrice = packagePrice, packageQty = packageQty, discount = discount,
        stock = stock, lowStockThreshold = lowStockThreshold, createdAt = createdAt,
        updatedAt = updatedAt, syncUpdatedAt = syncUpdatedAt, isDeleted = isDeleted
    )

    private fun CloudProduct.toLocal(id: Long) = ProductEntity(
        id = id, remoteId = remoteId, code = code, name = name,
        purchasePrice = purchasePrice, purchasePackagePrice = purchasePackagePrice,
        purchasePackageQty = purchasePackageQty, sellingPrice = sellingPrice,
        packagePrice = packagePrice, packageQty = packageQty, discount = discount,
        stock = stock, lowStockThreshold = lowStockThreshold, createdAt = createdAt,
        updatedAt = updatedAt, syncUpdatedAt = syncUpdatedAt, isSynced = true,
        isDeleted = isDeleted
    )

    private suspend fun applyRemote(product: ProductEntity, expectedAt: Long, expectedSynced: Boolean) {
        productDao.applyRemoteIfUnchanged(
            id = product.id,
            code = product.code,
            name = product.name,
            purchasePrice = product.purchasePrice,
            purchasePackagePrice = product.purchasePackagePrice,
            purchasePackageQty = product.purchasePackageQty,
            sellingPrice = product.sellingPrice,
            packagePrice = product.packagePrice,
            packageQty = product.packageQty,
            discount = product.discount,
            stock = product.stock,
            lowStockThreshold = product.lowStockThreshold,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
            remoteId = requireNotNull(product.remoteId),
            syncUpdatedAt = product.syncUpdatedAt,
            isDeleted = product.isDeleted,
            expectedSyncUpdatedAt = expectedAt,
            expectedIsSynced = expectedSynced
        )
    }
}
