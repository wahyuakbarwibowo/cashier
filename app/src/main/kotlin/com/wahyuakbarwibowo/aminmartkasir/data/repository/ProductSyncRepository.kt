package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.CloudProduct
import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID

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

    override suspend fun getProducts(storeId: String): List<CloudProduct> =
        SupabaseProvider.client.postgrest.from("products")
            .select { filter { eq("store_id", storeId) } }
            .decodeList()
}

class ProductSyncRepository(
    private val productDao: ProductDao,
    private val cloud: ProductCloudDataSource = SupabaseProductCloudDataSource()
) {
    suspend fun sync(storeId: String): Int {
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
        return pushed
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
