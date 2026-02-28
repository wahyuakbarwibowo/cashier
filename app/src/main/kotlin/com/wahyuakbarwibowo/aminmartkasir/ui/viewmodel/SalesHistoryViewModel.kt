package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class SalesHistoryUiState(
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: Map<Long, List<SaleItemEntity>> = emptyMap(),
    val startDate: String = "",
    val endDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class SalesHistoryViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState())
    val uiState: StateFlow<SalesHistoryUiState> = _uiState.asStateFlow()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadSales()
    }

    private fun loadSales() {
        viewModelScope.launch {
            saleRepository.allSales.collect { sales ->
                _uiState.update { 
                    it.copy(
                        sales = sales,
                        isLoading = false,
                        error = null
                    ) 
                }
                
                // Load items for each sale
                sales.forEach { sale ->
                    saleRepository.getSaleItems(sale.id).collect { items ->
                        _uiState.update { state ->
                            state.copy(
                                saleItems = state.saleItems + (sale.id to items)
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadSalesByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, startDate = startDate, endDate = endDate) }
            
            saleRepository.allSales.collect { sales ->
                val filtered = sales.filter { sale ->
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
