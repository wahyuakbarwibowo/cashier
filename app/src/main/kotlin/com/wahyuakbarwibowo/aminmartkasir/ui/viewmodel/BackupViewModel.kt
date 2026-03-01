package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

class BackupViewModel(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Sedang mengekspor data...") }
            try {
                val json = backupRepository.exportData()
                onDataReady(json)
                _uiState.update { it.copy(isLoading = false, message = "Ekspor berhasil!", isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Gagal ekspor: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Sedang mengimpor data...") }
            try {
                backupRepository.importData(json)
                _uiState.update { it.copy(isLoading = false, message = "Impor berhasil! Data telah diperbarui.", isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Gagal impor: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
