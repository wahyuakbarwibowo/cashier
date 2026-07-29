package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HppCalculatorUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedProduct: ProductEntity? = null,
    val totalHargaBeli: String = "",
    val jumlahUnit: String = "",
    val biayaTambahan: String = "",
    val isSaving: Boolean = false,
    val savedMessage: String? = null
) {
    val hpp: Double
        get() {
            val total = totalHargaBeli.toDoubleOrNull() ?: 0.0
            val qty = jumlahUnit.toIntOrNull() ?: 0
            val biaya = biayaTambahan.toDoubleOrNull() ?: 0.0
            return if (qty > 0) (total + biaya) / qty else 0.0
        }
}

class HppCalculatorViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HppCalculatorUiState())
    val uiState: StateFlow<HppCalculatorUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(productRepository.allProducts, _searchQuery) { products, query ->
                if (query.isBlank()) products else products.filter {
                    it.name.contains(query, ignoreCase = true) || it.code?.contains(query, ignoreCase = true) == true
                }
            }.collect { filtered ->
                _uiState.update { it.copy(products = filtered) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectProduct(product: ProductEntity?) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun setTotalHargaBeli(value: String) {
        _uiState.update { it.copy(totalHargaBeli = value) }
    }

    fun setJumlahUnit(value: String) {
        _uiState.update { it.copy(jumlahUnit = value) }
    }

    fun setBiayaTambahan(value: String) {
        _uiState.update { it.copy(biayaTambahan = value) }
    }

    fun saveHppToProduct() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        if (state.hpp <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true) }
            productRepository.update(product.copy(purchasePrice = state.hpp))
            _uiState.update {
                it.copy(
                    isSaving = false,
                    savedMessage = "HPP ${product.name} disimpan: Rp ${state.hpp.toLong()}"
                )
            }
        }
    }

    fun clearSavedMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}
