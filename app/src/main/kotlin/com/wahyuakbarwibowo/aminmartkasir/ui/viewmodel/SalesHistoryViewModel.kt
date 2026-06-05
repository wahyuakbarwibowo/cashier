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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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
        // For date range, we usually load all or use a different paging strategy
        // Simplified: load all for the range for now to keep it predictable
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, startDate = startDate, endDate = endDate, canLoadMore = false) }
            try {
                // This repository method needs to be checked if it supports pagination or not
                // For now we use the existing allSales flow filtered manually or a specific query
                saleRepository.allSales.first().let { allSales ->
                    val filtered = allSales.filter { sale ->
                        sale.createdAt?.let { createdAt ->
                            isDateWithinRange(createdAt, startDate, endDate)
                        } ?: false
                    }
                    _uiState.update { 
                        it.copy(
                            sales = filtered,
                            isLoading = false,
                            error = null
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>> {
        return saleRepository.getSaleItems(saleId)
    }

    private fun isDateWithinRange(dateTime: String, startDate: String, endDate: String): Boolean {
        return try {
            val saleDate = dateTimeFormat.parse(dateTime) ?: return false
            val start = dateFormat.parse(startDate) ?: return false
            val end = dateFormat.parse(endDate) ?: return false
            !saleDate.before(start) && !saleDate.after(endOfDay(end))
        } catch (_: ParseException) {
            false
        }
    }

    private fun endOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
