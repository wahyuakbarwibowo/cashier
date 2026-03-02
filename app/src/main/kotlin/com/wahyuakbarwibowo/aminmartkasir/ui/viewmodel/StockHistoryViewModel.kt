package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockHistoryUiState(
    val history: List<StockHistoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class StockHistoryViewModel(
    private val stockHistoryRepository: StockHistoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StockHistoryUiState())
    val uiState: StateFlow<StockHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            stockHistoryRepository.allStockHistory.collect { data ->
                _uiState.update {
                    it.copy(
                        history = data,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}

