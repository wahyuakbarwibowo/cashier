package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SaleDetailUiState(
    val sale: SaleEntity? = null,
    val items: List<SaleItemEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class SaleDetailViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleDetailUiState())
    val uiState: StateFlow<SaleDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadSaleDetail(saleId: Long) {
        // Dipanggil lagi setiap kembali dari layar Edit; collector lama akan menulis header
        // sale yang basi setiap kali item berubah, jadi batalkan dulu.
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sale = saleRepository.getSaleById(saleId)
                if (sale != null) {
                    // Collect items as a flow so it updates if they change (unlikely but safer)
                    saleRepository.getSaleItems(saleId).collect { items ->
                        _uiState.update {
                            it.copy(
                                sale = sale,
                                items = items,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Transaksi tidak ditemukan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
