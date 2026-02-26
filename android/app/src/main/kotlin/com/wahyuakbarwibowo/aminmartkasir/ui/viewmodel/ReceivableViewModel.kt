package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.CustomerRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ReceivableRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReceivableUiState(
    val receivables: List<ReceivableEntity> = emptyList(),
    val customers: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ReceivableViewModel(
    private val receivableRepository: ReceivableRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceivableUiState())
    val uiState: StateFlow<ReceivableUiState> = _uiState.asStateFlow()

    init {
        loadReceivables()
        loadCustomers()
    }

    private fun loadReceivables() {
        viewModelScope.launch {
            receivableRepository.allReceivables.collect { receivables ->
                _uiState.update { 
                    it.copy(
                        receivables = receivables,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            customerRepository.allCustomers.collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun markAsPaid(id: Long) {
        viewModelScope.launch {
            try {
                receivableRepository.updateStatus(id, "paid")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteReceivable(receivable: ReceivableEntity) {
        viewModelScope.launch {
            try {
                receivableRepository.delete(receivable)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
