package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PhoneHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DigitalReportDetailUiState(
    val history: PhoneHistoryEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class DigitalReportDetailViewModel(
    private val phoneHistoryRepository: PhoneHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalReportDetailUiState())
    val uiState: StateFlow<DigitalReportDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(reportId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val detail = phoneHistoryRepository.getPhoneHistoryById(reportId)
                if (detail != null) {
                    _uiState.update { 
                        it.copy(
                            history = detail,
                            isLoading = false,
                            error = null
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Detail transaksi tidak ditemukan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
