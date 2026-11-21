package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class SalesHistoryUiState(
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: Map<Long, List<SaleItemEntity>> = emptyMap(),
    val startDate: String = "",
    val endDate: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

class SalesHistoryViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState: StateFlow<SalesHistoryUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false
    private var loadJob: Job? = null

    init {
        loadInitialSales()
    }

    private fun loadInitialSales() {
        currentPage = 0
        isLastPage = false
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, sales = emptyList(), canLoadMore = true) }
            try {
                val sales = saleRepository.getSales(pageSize, 0)
                if (sales.size < pageSize) {
                    isLastPage = true
                }
                _uiState.update { 
                    it.copy(
                        sales = sales,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !isLastPage,
                        error = null
                    ) 
                }
                currentPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun loadNextPage() {
        if (isLastPage || _uiState.value.isLoadMoreLoading || _uiState.value.isLoading) return

        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentPage * pageSize
                val newSales = saleRepository.getSales(pageSize, offset)
                
                if (newSales.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        sales = state.sales + newSales,
                        isLoadMoreLoading = false,
                        canLoadMore = !isLastPage,
                        error = null
                    )
                }
                currentPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMoreLoading = false, error = e.message) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true, startDate = "", endDate = "") }
            delay(500)
            loadInitialSales()
        }
    }

    fun loadSalesByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, startDate = startDate, endDate = endDate, canLoadMore = false) }
            try {
                val filtered = saleRepository.getSalesByDateRange(
                    DateUtils.startOfDay(startDate),
                    DateUtils.endOfDay(endDate)
                )
                _uiState.update {
                    it.copy(
                        sales = filtered,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearDateFilter() {
        _uiState.update { it.copy(startDate = "", endDate = "") }
        loadInitialSales()
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>> {
        return saleRepository.getSaleItems(saleId)
    }
}
