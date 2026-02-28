package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductUiState(
    val products: List<ProductEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProductViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    
    init {
        loadProducts()
        loadLowStockProducts()
        
        viewModelScope.launch {
            _searchQuery.collect { query ->
                if (query.isNotBlank()) {
                    productRepository.searchProducts(query).collect { products ->
                        _uiState.update { it.copy(products = products, searchQuery = query) }
                    }
                } else {
                    loadProducts()
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
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
                productRepository.insert(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                productRepository.update(product)
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
