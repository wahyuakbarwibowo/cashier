package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.repository.BackupRepository
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.InputStream
import java.io.OutputStream

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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, message = "Sedang mengimpor data...") }
            try {
                backupRepository.importData(json)
                _uiState.update { it.copy(isLoading = false, message = "Impor berhasil! Data telah diperbarui.", isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Gagal impor: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun exportExcel(openOutput: () -> OutputStream?, template: Boolean = false) = runFileAction(
        if (template) "Menyiapkan template Excel..." else "Mengekspor data Excel...",
        if (template) "Template Excel berhasil disimpan." else "Data Excel berhasil disimpan."
    ) {
        backupRepository.exportExcel(requireNotNull(openOutput()) { "File tujuan tidak dapat dibuka." }, template)
    }

    fun importExcel(openInput: () -> InputStream?) = runFileAction(
        "Mengimpor data Excel...", "Data Excel berhasil diimpor."
    ) {
        backupRepository.importExcel(requireNotNull(openInput()) { "File sumber tidak dapat dibuka." })
    }

    fun exportPdf(openOutput: () -> OutputStream?) = runFileAction(
        "Membuat PDF...", "PDF berhasil disimpan."
    ) {
        backupRepository.exportPdf(requireNotNull(openOutput()) { "File tujuan tidak dapat dibuka." })
    }

    private fun runFileAction(loading: String, success: String, action: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, message = loading) }
            try {
                action()
                _uiState.update { it.copy(isLoading = false, message = success, isSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Gagal: ${e.message}", isSuccess = false) }
            }
        }
    }

    fun saveCloudBackup() = runCloudBackup("Menyimpan cadangan ke cloud...") { storeId ->
        backupRepository.saveCloudBackup(storeId)
        "Cadangan berhasil disimpan ke cloud."
    }

    fun restoreCloudBackup() = runCloudBackup("Memulihkan data dari cloud...") { storeId ->
        backupRepository.restoreCloudBackup(storeId)
        "Data berhasil dipulihkan dari cloud."
    }

    private fun runCloudBackup(message: String, action: suspend (String) -> String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, message = message) }
            try {
                val profile = (AuthManager.state.value as? AuthManager.UiState.Authenticated)?.profile
                    ?: throw IllegalStateException("Silakan login untuk menggunakan cadangan cloud.")
                val storeId = profile.storeId
                    ?: throw IllegalStateException("Akun belum terhubung ke toko.")
                val result = action(storeId)
                _uiState.update { it.copy(isLoading = false, message = result, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = "Gagal: ${e.message}", isSuccess = false)
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
