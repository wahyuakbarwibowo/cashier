package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProductUiState(
    val products: List<ProductEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val editingProduct: ProductEntity? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val stockHistoryRepository: StockHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var searchJob: Job? = null

    init {
        loadProducts()
        loadLowStockProducts()

        viewModelScope.launch {
            _searchQuery.collect { query ->
                searchJob?.cancel()
                if (query.isNotBlank()) {
                    searchJob = viewModelScope.launch {
                        productRepository.searchProducts(query).collect { products ->
                            _uiState.update { it.copy(products = products, searchQuery = query) }
                        }
                    }
                } else {
                    searchJob = viewModelScope.launch {
                        productRepository.allProducts.collect { products ->
                            _uiState.update {
                                it.copy(
                                    products = products,
                                    searchQuery = query,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadProducts() {
        // Only load if search query is empty to avoid overriding search results
        if (_searchQuery.value.isBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                productRepository.allProducts.collect { products ->
                    _uiState.update { 
                        it.copy(
                            products = products,
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
            }
        }
    }
    private fun loadLowStockProducts() {
        viewModelScope.launch {
            productRepository.getLowStockProducts().collect { products ->
                _uiState.update { it.copy(lowStockProducts = products) }
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                val insertedId = productRepository.insert(product)
                if (product.stock > 0) {
                    stockHistoryRepository.insert(
                        StockHistoryEntity(
                            productId = insertedId,
                            productName = product.name,
                            changeQty = product.stock,
                            stockBefore = 0,
                            stockAfter = product.stock,
                            reason = "Tambah produk baru",
                            createdAt = dateFormat.format(Date())
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                val previous = productRepository.getProductById(product.id)
                productRepository.update(product)

                if (previous != null && previous.stock != product.stock) {
                    val delta = product.stock - previous.stock
                    val reason = if (delta > 0) "Penambahan stok manual" else "Pengurangan stok manual"
                    stockHistoryRepository.insert(
                        StockHistoryEntity(
                            productId = product.id,
                            productName = product.name,
                            changeQty = delta,
                            stockBefore = previous.stock,
                            stockAfter = product.stock,
                            reason = reason,
                            createdAt = dateFormat.format(Date())
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadProductById(productId: Long) {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                _uiState.update { it.copy(editingProduct = product, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                productRepository.delete(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
