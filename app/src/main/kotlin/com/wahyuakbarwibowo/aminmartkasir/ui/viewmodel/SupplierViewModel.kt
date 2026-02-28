package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SupplierUiState(
    val suppliers: List<SupplierEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class SupplierViewModel(
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierRepository.allSuppliers.collect { suppliers ->
                _uiState.update { 
                    it.copy(
                        suppliers = suppliers,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    fun addSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            try {
                supplierRepository.insert(supplier)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            try {
                supplierRepository.update(supplier)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            try {
                supplierRepository.delete(supplier)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
