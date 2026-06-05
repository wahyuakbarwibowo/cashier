package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductVariantEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductVariantRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ProductSortOption(val label: String, val repositorySort: ProductRepository.ProductSort) {
    NEWEST("Terbaru", ProductRepository.ProductSort.NEWEST),
    NAME_ASC("Nama A-Z", ProductRepository.ProductSort.NAME_ASC),
    NAME_DESC("Nama Z-A", ProductRepository.ProductSort.NAME_DESC),
    STOCK_ASC("Stok Rendah", ProductRepository.ProductSort.STOCK_ASC),
    STOCK_DESC("Stok Tinggi", ProductRepository.ProductSort.STOCK_DESC)
}

data class ProductUiState(
    val products: List<ProductEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val editingProduct: ProductEntity? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val sortOption: ProductSortOption = ProductSortOption.NEWEST,
    val searchQuery: String = "",
    val successMessage: String? = null,
    val error: String? = null
)

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val stockHistoryRepository: StockHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    init {
        loadInitialProducts()
        loadLowStockProducts()
    }

    private fun loadLowStockProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getLowStockProducts().collect { lowStock ->
                _uiState.update { it.copy(lowStockProducts = lowStock) }
            }
        }
    }

    private fun loadInitialProducts() {
        currentPage = 0
        isLastPage = false
        loadJob?.cancel()
        searchJob?.cancel()
        
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, products = emptyList(), canLoadMore = true) }
            try {
                val initialProducts = productRepository.getProductsSorted(
                    limit = pageSize,
                    offset = 0,
                    sort = _uiState.value.sortOption.repositorySort
                )
                if (initialProducts.size < pageSize) {
                    isLastPage = true
                }
                _uiState.update { 
                    it.copy(
                        products = initialProducts,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !isLastPage
                    ) 
                }
                currentPage = 1
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun loadNextPage() {
        if (isLastPage || _uiState.value.isLoadMoreLoading || _uiState.value.isLoading || _uiState.value.searchQuery.isNotBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentPage * pageSize
                val newProducts = productRepository.getProductsSorted(
                    limit = pageSize,
                    offset = offset,
                    sort = _uiState.value.sortOption.repositorySort
                )
                
                if (newProducts.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        products = state.products + newProducts,
                        isLoadMoreLoading = false,
                        canLoadMore = !isLastPage
                    )
                }
                currentPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMoreLoading = false, error = e.message) }
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        loadJob?.cancel()
        
        if (query.isBlank()) {
            loadInitialProducts()
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, canLoadMore = false) }
            try {
                productRepository.searchProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setSortOption(sortOption: ProductSortOption) {
        if (_uiState.value.sortOption == sortOption) return
        _uiState.update {
            it.copy(
                sortOption = sortOption,
                searchQuery = ""
            )
        }
        loadInitialProducts()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true, searchQuery = "") }
            delay(500)
            loadInitialProducts()
        }
    }

    fun loadProductById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val product = productRepository.getProductById(id)
                _uiState.update { it.copy(editingProduct = product, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = productRepository.insert(product)
                
                // Record stock history
                if (product.stock != 0) {
                    stockHistoryRepository.insert(
                        StockHistoryEntity(
                            productId = id,
                            productName = product.name,
                            changeQty = product.stock,
                            stockBefore = 0,
                            stockAfter = product.stock,
                            reason = "Stok awal produk baru",
                            createdAt = DateUtils.nowDateTime()
                        )
                    )
                }
                
                _uiState.update { it.copy(successMessage = "Produk berhasil ditambahkan") }
                loadInitialProducts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val oldProduct = productRepository.getProductById(product.id)
                productRepository.update(product)
                
                // Record stock history if stock changed manually
                if (oldProduct != null && oldProduct.stock != product.stock) {
                    stockHistoryRepository.insert(
                        StockHistoryEntity(
                            productId = product.id,
                            productName = product.name,
                            changeQty = product.stock - oldProduct.stock,
                            stockBefore = oldProduct.stock,
                            stockAfter = product.stock,
                            reason = "Update stok manual",
                            createdAt = DateUtils.nowDateTime()
                        )
                    )
                }
                
                _uiState.update { it.copy(successMessage = "Produk berhasil diperbarui") }
                loadInitialProducts()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                productRepository.delete(product)
                _uiState.update { it.copy(successMessage = "Produk berhasil dihapus") }
                if (_uiState.value.searchQuery.isBlank()) {
                    loadInitialProducts()
                } else {
                    searchProducts(_uiState.value.searchQuery)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun adjustStock(productId: Long, changeQty: Int, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val product = productRepository.getProductById(productId)
                if (product == null) {
                    _uiState.update { it.copy(error = "Produk tidak ditemukan") }
                    return@launch
                }

                val newStock = product.stock + changeQty
                
                if (changeQty > 0) {
                    productRepository.increaseStock(productId, changeQty)
                } else {
                    productRepository.decreaseStock(productId, -changeQty)
                }

                stockHistoryRepository.insert(
                    StockHistoryEntity(
                        productId = productId,
                        productName = product.name,
                        changeQty = changeQty,
                        stockBefore = product.stock,
                        stockAfter = newStock,
                        reason = reason,
                        createdAt = DateUtils.nowDateTime()
                    )
                )

                _uiState.update { it.copy(successMessage = "Stok berhasil disesuaikan") }
                if (_uiState.value.searchQuery.isBlank()) {
                    loadInitialProducts()
                } else {
                    searchProducts(_uiState.value.searchQuery)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }

    suspend fun getVariantsByProductId(productId: Long): List<ProductVariantEntity> {
        return productVariantRepository.getByProductIdOnce(productId)
    }

    suspend fun replaceVariants(productId: Long, variants: List<ProductVariantEntity>) {
        productVariantRepository.deleteByProductId(productId)
        variants.forEach { variant ->
            productVariantRepository.insert(variant.copy(id = 0, productId = productId))
        }
    }
}
