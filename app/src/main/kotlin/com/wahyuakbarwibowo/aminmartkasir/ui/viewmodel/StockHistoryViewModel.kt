package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StockHistoryUiState(
    val history: List<StockHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

class StockHistoryViewModel(
    private val stockHistoryRepository: StockHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockHistoryUiState())
    val uiState: StateFlow<StockHistoryUiState> = _uiState.asStateFlow()
    
    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        currentPage = 0
        isLastPage = false
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, history = emptyList(), canLoadMore = true) }
            try {
                val initialHistory = stockHistoryRepository.getStockHistory(pageSize, 0)
                if (initialHistory.size < pageSize) {
                    isLastPage = true
                }
                _uiState.update { 
                    it.copy(
                        history = initialHistory,
                        isLoading = false,
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
                val offset = currentPage * pageSize
                val newHistory = stockHistoryRepository.getStockHistory(pageSize, offset)
                
                if (newHistory.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        history = state.history + newHistory,
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

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(500)
            loadInitialData()
        }
    }
}
