package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PayableEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ReceivableRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PayableRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.CustomerRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SupplierRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class DebtUiState(
    val receivables: List<ReceivableEntity> = emptyList(),
    val payables: List<PayableEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val filterStatus: String = "ALL", // ALL, PENDING, PAID
    val successMessage: String? = null,
    val error: String? = null
)

class DebtViewModel(
    private val receivableRepository: ReceivableRepository,
    private val payableRepository: PayableRepository,
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    
    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                customerRepository.allCustomers,
                supplierRepository.allSuppliers
            ) { customers, suppliers ->
                customers to suppliers
            }.collect { (customers, suppliers) ->
                _uiState.update { it.copy(customers = customers, suppliers = suppliers) }
            }
        }
        loadReceivables()
    }

    fun setFilterStatus(status: String) {
        _uiState.update { it.copy(filterStatus = status) }
        loadReceivables() // Reload with new filter
    }

    fun loadReceivables() {
        currentPage = 0
        isLastPage = false
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, canLoadMore = true) }
            try {
                val status = _uiState.value.filterStatus
                val list = if (status == "ALL") {
                    receivableRepository.getReceivables(pageSize, 0)
                } else {
                    receivableRepository.getReceivablesByStatusPaginated(status.lowercase(), pageSize, 0)
                }

                if (list.size < pageSize) isLastPage = true
                
                _uiState.update { 
                    it.copy(
                        receivables = list,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !isLastPage
                    ) 
                }
                currentPage = 1
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadNextPage() {
        if (isLastPage || _uiState.value.isLoadMoreLoading || _uiState.value.isLoading) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val status = _uiState.value.filterStatus
                val offset = currentPage * pageSize
                val newList = if (status == "ALL") {
                    receivableRepository.getReceivables(pageSize, offset)
                } else {
                    receivableRepository.getReceivablesByStatusPaginated(status.lowercase(), pageSize, offset)
                }

                if (newList.size < pageSize) isLastPage = true
                
                _uiState.update { state ->
                    state.copy(
                        receivables = state.receivables + newList,
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

    fun loadPayables() {
        // Implement similar to receivables if needed, for now we focus on receivables (customer debt)
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(500)
            loadReceivables()
        }
    }

    fun recordPayment(receivable: ReceivableEntity, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newPaidAmount = receivable.paidAmount + amount
                val newStatus = if (newPaidAmount >= receivable.amount) "paid" else "pending"
                
                val updated = receivable.copy(
                    paidAmount = newPaidAmount,
                    status = newStatus
                )
                receivableRepository.update(updated)
                
                _uiState.update { it.copy(successMessage = "Pembayaran berhasil dicatat") }
                loadReceivables()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addReceivable(customerId: Long, amount: Double, dueDate: String?, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val receivable = ReceivableEntity(
                    customerId = customerId,
                    amount = amount,
                    paidAmount = 0.0,
                    dueDate = dueDate,
                    status = "pending",
                    notes = notes,
                    createdAt = DateUtils.nowDateTime()
                )
                receivableRepository.insert(receivable)
                _uiState.update { it.copy(successMessage = "Hutang baru berhasil dicatat") }
                loadReceivables()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteReceivable(receivable: ReceivableEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                receivableRepository.delete(receivable)
                _uiState.update { it.copy(successMessage = "Data hutang dihapus") }
                loadReceivables()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
