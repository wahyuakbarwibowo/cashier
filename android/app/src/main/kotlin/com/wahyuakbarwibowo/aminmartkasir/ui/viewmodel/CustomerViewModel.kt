package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.CustomerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class CustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadCustomers()
        
        viewModelScope.launch {
            _searchQuery.collect { query ->
                if (query.isNotBlank()) {
                    customerRepository.searchCustomers(query).collect { customers ->
                        _uiState.update { it.copy(customers = customers, searchQuery = query) }
                    }
                } else {
                    loadCustomers()
                }
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            customerRepository.allCustomers.collect { customers ->
                _uiState.update { 
                    it.copy(
                        customers = customers,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun addCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                customerRepository.insert(customer)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                customerRepository.update(customer)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                customerRepository.delete(customer)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
