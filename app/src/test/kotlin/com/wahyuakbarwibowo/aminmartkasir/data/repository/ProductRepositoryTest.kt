package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class ProductRepositoryTest {

    @Mock
    private lateinit var productDao: ProductDao

    private lateinit var productRepository: ProductRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        productRepository = ProductRepository(productDao)
    }

    @Test
    fun `insert product should call dao insert`() = runTest {
        // Given
        val product = ProductEntity(
            id = 0,
            code = "TEST001",
            name = "Test Product",
            purchasePrice = 5000.0,
            purchasePackagePrice = 0.0,
            purchasePackageQty = 0,
            sellingPrice = 7000.0,
            packagePrice = 0.0,
            packageQty = 0,
            discount = 0.0,
            stock = 10,
            createdAt = null,
            updatedAt = null
        )
        val generatedId = 1L

        `when`(productDao.insert(product)).thenReturn(generatedId)

        // When
        val result = productRepository.insert(product)

        // Then
        assertEquals(generatedId, result)
        verify(productDao).insert(product)
    }

    @Test
    fun `getProductById should return product when exists`() = runTest {
        // Given
        val productId = 1L
        val product = ProductEntity(
            id = productId,
            code = "TEST001",
            name = "Test Product",
            purchasePrice = 5000.0,
            purchasePackagePrice = 0.0,
            purchasePackageQty = 0,
            sellingPrice = 7000.0,
            packagePrice = 0.0,
            packageQty = 0,
            discount = 0.0,
            stock = 10,
            createdAt = null,
            updatedAt = null
        )

        `when`(productDao.getProductById(productId)).thenReturn(product)

        // When
        val result = productRepository.getProductById(productId)

        // Then
        assertEquals(product, result)
    }

    @Test
    fun `getProductById should return null when not exists`() = runTest {
        // Given
        val productId = 999L
        `when`(productDao.getProductById(productId)).thenReturn(null)

        // When
        val result = productRepository.getProductById(productId)

        // Then
        assertNull(result)
    }

    @Test
    fun `update product should call dao update`() = runTest {
        // Given
        val product = ProductEntity(
            id = 1L,
            code = "TEST001",
            name = "Updated Product",
            purchasePrice = 5000.0,
            purchasePackagePrice = 0.0,
            purchasePackageQty = 0,
            sellingPrice = 7500.0,
            packagePrice = 0.0,
            packageQty = 0,
            discount = 0.0,
            stock = 15,
            createdAt = null,
            updatedAt = null
        )

        // When
        productRepository.update(product)

        // Then
        verify(productDao).update(product)
    }

    @Test
    fun `delete product should call dao delete`() = runTest {
        // Given
        val product = ProductEntity(
            id = 1L,
            code = "TEST001",
            name = "Product to Delete",
            purchasePrice = 5000.0,
            purchasePackagePrice = 0.0,
            purchasePackageQty = 0,
            sellingPrice = 7000.0,
            packagePrice = 0.0,
            packageQty = 0,
            discount = 0.0,
            stock = 10,
            createdAt = null,
            updatedAt = null
        )

        // When
        productRepository.delete(product)

        // Then
        verify(productDao).delete(product)
    }

    @Test
    fun `decreaseStock should call dao decreaseStock`() = runTest {
        // Given
        val productId = 1L
        val quantity = 5

        // When
        productRepository.decreaseStock(productId, quantity)

        // Then
        verify(productDao).decreaseStock(productId, quantity)
    }

    @Test
    fun `increaseStock should call dao increaseStock`() = runTest {
        // Given
        val productId = 1L
        val quantity = 10

        // When
        productRepository.increaseStock(productId, quantity)

        // Then
        verify(productDao).increaseStock(productId, quantity)
    }

    @Test
    fun `searchProducts should return matching products`() {
        // Given
        val query = "Test"
        val products = listOf(
            ProductEntity(
                id = 1L,
                code = "TEST001",
                name = "Test Product 1",
                purchasePrice = 5000.0,
                purchasePackagePrice = 0.0,
                purchasePackageQty = 0,
                sellingPrice = 7000.0,
                packagePrice = 0.0,
                packageQty = 0,
                discount = 0.0,
                stock = 10,
                createdAt = null,
                updatedAt = null
            )
        )
        `when`(productDao.searchProducts(query)).thenReturn(flowOf(products))

        // When
        val result = productRepository.searchProducts(query)

        // Then
        assertEquals(products, (result as Flow<List<ProductEntity>>).let { 
            runTest { 
                it.first() 
            } 
        })
    }
}
