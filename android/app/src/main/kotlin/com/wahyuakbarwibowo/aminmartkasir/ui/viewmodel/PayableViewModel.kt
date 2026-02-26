package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PayableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PayableRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PayableUiState(
    val payables: List<PayableEntity> = emptyList(),
    val suppliers: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class PayableViewModel(
    private val payableRepository: PayableRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayableUiState())
    val uiState: StateFlow<PayableUiState> = _uiState.asStateFlow()

    init {
        loadPayables()
        loadSuppliers()
    }

    private fun loadPayables() {
        viewModelScope.launch {
            payableRepository.allPayables.collect { payables ->
                _uiState.update { 
                    it.copy(
                        payables = payables,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierRepository.allSuppliers.collect { suppliers ->
                _uiState.update { it.copy(suppliers = suppliers) }
            }
        }
    }

    fun markAsPaid(id: Long) {
        viewModelScope.launch {
            try {
                payableRepository.updateStatus(id, "paid")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePayable(payable: PayableEntity) {
        viewModelScope.launch {
            try {
                payableRepository.delete(payable)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
