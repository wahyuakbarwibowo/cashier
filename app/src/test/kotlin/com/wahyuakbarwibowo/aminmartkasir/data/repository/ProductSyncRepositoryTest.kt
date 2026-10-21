package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.CloudProduct
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.Mockito.times

class ProductSyncRepositoryTest {
    private lateinit var productDao: ProductDao
    private lateinit var cloud: FakeProductCloudDataSource
    private lateinit var syncRepository: ProductSyncRepository

    @Before
    fun setup() {
        productDao = org.mockito.kotlin.mock()
        cloud = FakeProductCloudDataSource()
        syncRepository = ProductSyncRepository(productDao, cloud) { _, _ -> }
    }

    @Test
    fun `pending product is uploaded and marked synced`() = runTest {
        val local = product(id = 4, syncUpdatedAt = 100)
        whenever(productDao.getPendingSync()).thenReturn(listOf(local))

        val pushed = syncRepository.sync("store-1")

        assertEquals(1, pushed)
        assertEquals("store-1", cloud.uploads.single().storeId)
        assertNotNull(cloud.uploads.single().remoteId)
        val persisted = argumentCaptor<ProductEntity>()
        verify(productDao).update(persisted.capture())
        assertEquals(cloud.uploads.single().remoteId, persisted.firstValue.remoteId)
        verify(productDao).markSynced(4, 100)
    }

    @Test
    fun `newer cloud product wins over pending local version`() = runTest {
        val local = product(id = 4, remoteId = "product-1", syncUpdatedAt = 100)
        val remote = cloudProduct(remoteId = "product-1", syncUpdatedAt = 200, name = "Cloud name")
        whenever(productDao.getPendingSync()).thenReturn(listOf(local))
        whenever(productDao.getByRemoteId("product-1")).thenReturn(local)
        cloud.products += remote

        val pushed = syncRepository.sync("store-1")

        assertEquals(0, pushed)
        assertTrue(cloud.uploads.isEmpty())
        verify(productDao).applyRemoteIfUnchanged(
            eq(4), eq("A1"), eq("Cloud name"), eq(10.0), eq(0.0), eq(0),
            eq(12.0), eq(0.0), eq(0), eq(0.0), eq(8), eq(5), eq(null), eq(null),
            eq("product-1"), eq(200L), eq(false), eq(100L), eq(false)
        )
    }

    @Test
    fun `unknown cloud product is inserted locally as synced`() = runTest {
        val remote = cloudProduct(remoteId = "product-2", syncUpdatedAt = 300, name = "Pulled")
        whenever(productDao.getPendingSync()).thenReturn(emptyList())
        whenever(productDao.getByRemoteId("product-2")).thenReturn(null)
        cloud.products += remote

        syncRepository.sync("store-1")

        val inserted = argumentCaptor<ProductEntity>()
        verify(productDao).insertIfAbsent(inserted.capture())
        assertEquals("Pulled", inserted.firstValue.name)
        assertEquals("product-2", inserted.firstValue.remoteId)
        assertEquals(300L, inserted.firstValue.syncUpdatedAt)
        assertTrue(inserted.firstValue.isSynced)
    }

    @Test
    fun `cloud timestamp tie leaves local version unchanged`() = runTest {
        val local = product(id = 4, remoteId = "product-3", syncUpdatedAt = 400, isSynced = true)
        whenever(productDao.getPendingSync()).thenReturn(emptyList())
        whenever(productDao.getByRemoteId("product-3")).thenReturn(local)
        cloud.products += cloudProduct(remoteId = "product-3", syncUpdatedAt = 400)

        syncRepository.sync("store-1")

        org.mockito.kotlin.verify(productDao, org.mockito.Mockito.never())
            .applyRemoteIfUnchanged(any(), anyOrNull(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), any(), any(), any(), any(), any())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `manual and background sync serialize access to pending products`() = runTest {
        val gate = CompletableDeferred<Unit>()
        cloud.uploadGate = gate
        whenever(productDao.getPendingSync()).thenReturn(listOf(product(id = 4, syncUpdatedAt = 100)), emptyList())
        val secondRepository = ProductSyncRepository(productDao, cloud) { _, _ -> }

        val first = launch { syncRepository.sync("store-1") }
        runCurrent()
        val second = launch { secondRepository.sync("store-1") }
        runCurrent()
        verify(productDao, times(1)).getPendingSync()

        gate.complete(Unit)
        advanceUntilIdle()
        verify(productDao, times(2)).getPendingSync()
        assertTrue(first.isCompleted && second.isCompleted)
    }

    private fun product(
        id: Long,
        remoteId: String? = null,
        syncUpdatedAt: Long,
        isSynced: Boolean = false
    ) = ProductEntity(
        id = id, remoteId = remoteId, code = "A1", name = "Local name",
        purchasePrice = 10.0, sellingPrice = 12.0, stock = 8,
        syncUpdatedAt = syncUpdatedAt, isSynced = isSynced
    )

    private fun cloudProduct(remoteId: String, syncUpdatedAt: Long, name: String = "Cloud") =
        CloudProduct(
            remoteId = remoteId, storeId = "store-1", code = "A1", name = name,
            purchasePrice = 10.0, purchasePackagePrice = 0.0, purchasePackageQty = 0,
            sellingPrice = 12.0, packagePrice = 0.0, packageQty = 0, discount = 0.0,
            stock = 8, lowStockThreshold = 5, syncUpdatedAt = syncUpdatedAt, isDeleted = false
        )

    private class FakeProductCloudDataSource : ProductCloudDataSource {
        val products = mutableListOf<CloudProduct>()
        val uploads = mutableListOf<CloudProduct>()
        var uploadGate: CompletableDeferred<Unit>? = null

        override suspend fun findProduct(storeId: String, remoteId: String) =
            products.firstOrNull { it.storeId == storeId && it.remoteId == remoteId }

        override suspend fun upsertProduct(product: CloudProduct) {
            uploadGate?.await()
            uploads += product
            products.removeAll { it.remoteId == product.remoteId }
            products += product
        }

        override suspend fun getProducts(storeId: String) = products.filter { it.storeId == storeId }
    }
}
