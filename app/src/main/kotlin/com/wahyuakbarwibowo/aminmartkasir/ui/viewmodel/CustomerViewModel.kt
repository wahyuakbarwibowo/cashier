package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.CustomerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val searchQuery: String = "",
    val successMessage: String? = null,
    val error: String? = null
)

class CustomerViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        currentPage = 0
        isLastPage = false
        loadJob?.cancel()
        searchJob?.cancel()
        
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, customers = emptyList(), canLoadMore = true) }
            try {
                val initialList = customerRepository.getCustomers(pageSize, 0)
                if (initialList.size < pageSize) {
                    isLastPage = true
                }
                _uiState.update { 
                    it.copy(
                        customers = initialList,
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentPage * pageSize
                val newList = customerRepository.getCustomers(pageSize, offset)
                
                if (newList.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        customers = state.customers + newList,
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

    fun searchCustomers(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        loadJob?.cancel()
        
        if (query.isBlank()) {
            loadInitialData()
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, canLoadMore = false) }
            try {
                customerRepository.searchCustomers(query).collect { list ->
                    _uiState.update { it.copy(customers = list, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, searchQuery = "") }
            delay(500)
            loadInitialData()
        }
    }

    fun addCustomer(name: String, phone: String, address: String) {
        viewModelScope.launch {
            try {
                val customer = CustomerEntity(
                    name = name,
                    phone = phone,
                    address = address,
                    createdAt = dateFormat.format(Date())
                )
                customerRepository.insert(customer)
                _uiState.update { it.copy(successMessage = "Pelanggan $name berhasil ditambahkan") }
                loadInitialData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                customerRepository.update(customer.copy(updatedAt = dateFormat.format(Date())))
                _uiState.update { it.copy(successMessage = "Data pelanggan diperbarui") }
                loadInitialData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                customerRepository.delete(customer)
                _uiState.update { it.copy(successMessage = "Pelanggan dihapus") }
                loadInitialData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
