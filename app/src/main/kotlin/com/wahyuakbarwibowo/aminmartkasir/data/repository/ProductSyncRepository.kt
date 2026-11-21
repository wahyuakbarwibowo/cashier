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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ProductCloudDataSource {
    suspend fun findProduct(storeId: String, remoteId: String): CloudProduct?
    /** Upsert data produk tanpa kolom stock; baris baru mulai dari stok 0 di server. */
    suspend fun upsertProduct(product: CloudProduct)
    suspend fun getProducts(storeId: String): List<CloudProduct>
    /** Tambahkan [delta] ke stok di server secara atomik; null bila produk tidak ada/ditolak RLS. */
    suspend fun addStock(remoteId: String, delta: Int): Int?
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
        // Stok hanya berubah lewat add_product_stock agar delta antarperangkat tidak saling timpa.
        val row = JsonObject(Json.encodeToJsonElement(CloudProduct.serializer(), product).jsonObject - "stock")
        SupabaseProvider.client.postgrest.from("products")
            .upsert(row) { onConflict = "remote_id" }
    }

    override suspend fun addStock(remoteId: String, delta: Int): Int? =
        SupabaseProvider.client.postgrest.rpc(
            "add_product_stock",
            buildJsonObject {
                put("p_remote_id", remoteId)
                put("p_delta", delta)
            }
        ).decodeAs<Int?>()

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
            val remoteId = local.remoteId ?: UUID.randomUUID().toString().also {
                productDao.assignRemoteId(local.id, it)
            }
            val product = local.copy(remoteId = remoteId)
            val existingRemote = cloud.findProduct(storeId, remoteId)
            // isSynced = true di sini berarti hanya delta stok yang tertunda.
            if (!product.isSynced) {
                if (existingRemote != null && existingRemote.syncUpdatedAt > product.syncUpdatedAt) {
                    applyRemote(existingRemote.toLocal(product.id), product.syncUpdatedAt, false)
                } else {
                    cloud.upsertProduct(product.toCloud(storeId))
                    productDao.markSynced(product.id, product.syncUpdatedAt)
                    pushed++
                }
            }
            pushStock(product, existingRemote)
        }

        cloud.getProducts(storeId).forEach { remoteProduct ->
            val local = productDao.getByRemoteId(remoteProduct.remoteId)
            if (local == null) {
                productDao.insertIfAbsent(remoteProduct.toLocal(0))
            } else {
                if (local.isSynced && remoteProduct.syncUpdatedAt > local.syncUpdatedAt) {
                    applyRemote(remoteProduct.toLocal(local.id), local.syncUpdatedAt, true)
                }
                productDao.reconcileStock(local.id, remoteProduct.stock)
            }
        }
        pushed
    }

    /**
     * Kirim selisih stok sejak sync terakhir. Basis null (data versi lama) memakai stok
     * server saat ini, sehingga stok lokal menimpa sekali lalu sesudahnya berbasis delta.
     * ponytail: tidak idempoten; bila respons RPC hilang di jaringan lalu sync diulang,
     * delta terkirim dua kali. Tambah op_id per delta + tabel log di server bila terjadi.
     */
    private suspend fun pushStock(local: ProductEntity, remoteBefore: CloudProduct?) {
        val base = local.syncedStock ?: remoteBefore?.stock ?: 0
        val delta = local.stock - base
        val serverStock = if (delta == 0) base else
            cloud.addStock(requireNotNull(local.remoteId), delta) ?: error("Stok ${local.name} ditolak server")
        if (delta != 0 || local.syncedStock == null) {
            productDao.markStockPushed(local.id, local.stock, serverStock)
        }
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
        isDeleted = isDeleted, syncedStock = stock
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
