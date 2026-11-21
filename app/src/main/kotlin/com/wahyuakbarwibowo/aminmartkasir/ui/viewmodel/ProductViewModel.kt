package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductVariantEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductVariantRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductSyncRepository
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val error: String? = null,
    val isSyncing: Boolean = false
)

class ProductViewModel(
    private val productRepository: ProductRepository,
    private val productVariantRepository: ProductVariantRepository,
    private val stockHistoryRepository: StockHistoryRepository,
    private val productSyncRepository: ProductSyncRepository,
    private val onManualSyncSucceeded: (String) -> Unit = {}
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
        reloadOnProductChanges()
    }

    /**
     * Form produk memakai instance ViewModel sendiri, jadi daftar di layar Produk perlu
     * tahu saat tabel berubah. Room memancarkan ulang kueri ini di setiap perubahan tabel.
     */
    private fun reloadOnProductChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.productCount.drop(1).collect {
                val query = _uiState.value.searchQuery
                if (query.isBlank()) loadInitialProducts() else searchProducts(query)
            }
        }
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
            delay(500)
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

    fun syncProducts() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            val profile = (AuthManager.state.value as? AuthManager.UiState.Authenticated)?.profile
            val storeId = profile?.storeId
            if (storeId.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Akun belum terhubung ke toko.") }
                return@launch
            }
            _uiState.update { it.copy(isSyncing = true, error = null) }
            try {
                val count = productSyncRepository.sync(storeId)
                onManualSyncSucceeded(storeId)
                _uiState.update { it.copy(isSyncing = false, successMessage = "Sinkronisasi selesai ($count produk dikirim)") }
                loadInitialProducts()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, error = e.message ?: "Sinkronisasi gagal.") }
            }
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

    /**
     * Simpan produk dari form, lalu panggil [onSaved] di Main. Layar baru boleh ditutup
     * setelah ini: VM terikat ke back stack form, jadi menutup layar lebih dulu bisa
     * membatalkan penyimpanan di tengah jalan. [variants] null = varian tidak diubah.
     */
    fun saveProduct(product: ProductEntity, variants: List<ProductVariantEntity>?, onSaved: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (product.id == 0L) {
                    productRepository.insert(product)
                    _uiState.update { it.copy(successMessage = "Produk berhasil ditambahkan") }
                } else {
                    val loadedStock = _uiState.value.editingProduct?.takeIf { it.id == product.id }?.stock ?: product.stock
                    productRepository.update(product, stockDelta = product.stock - loadedStock)
                    variants?.let { productVariantRepository.syncForProduct(product.id, it) }
                    _uiState.update { it.copy(successMessage = "Produk berhasil diperbarui") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                withContext(Dispatchers.Main) { onSaved() }
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
                if (!productRepository.adjustStock(productId, changeQty, reason)) {
                    _uiState.update { it.copy(error = "Produk tidak ditemukan") }
                    return@launch
                }

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
}
