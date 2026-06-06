package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShiftEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ShiftRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShiftUiState(
    val openShift: ShiftEntity? = null,
    val history: List<ShiftEntity> = emptyList(),
    val liveSales: Double = 0.0,
    val liveExpenses: Double = 0.0,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    /** Estimasi uang laci sekarang: modal awal + penjualan - pengeluaran selama shift. */
    val liveExpectedCash: Double
        get() = (openShift?.openingCash ?: 0.0) + liveSales - liveExpenses
}

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            shiftRepository.allShifts.collect { shifts ->
                val open = shifts.firstOrNull { it.status == "open" }
                _uiState.update { it.copy(openShift = open, history = shifts, isLoading = false) }
                refreshLiveTotals()
            }
        }
    }

    fun refreshLiveTotals() {
        viewModelScope.launch(Dispatchers.IO) {
            val open = _uiState.value.openShift ?: run {
                _uiState.update { it.copy(liveSales = 0.0, liveExpenses = 0.0) }
                return@launch
            }
            val now = DateUtils.nowDateTime()
            val sales = saleRepository.getTotalSalesByDateRange(open.openedAt, now)
            val expenses = expenseRepository.getTotalExpensesByDateRange(open.openedAt, now)
            _uiState.update { it.copy(liveSales = sales, liveExpenses = expenses) }
        }
    }

    fun openShift(openingCash: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            if (shiftRepository.getOpenShift() != null) {
                _uiState.update { it.copy(message = "Masih ada shift yang terbuka") }
                return@launch
            }
            shiftRepository.insert(
                ShiftEntity(
                    openedAt = DateUtils.nowDateTime(),
                    openingCash = openingCash,
                    status = "open"
                )
            )
        }
    }

    fun closeShift(countedCash: Double, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val open = shiftRepository.getOpenShift() ?: run {
                _uiState.update { it.copy(message = "Tidak ada shift terbuka") }
                return@launch
            }
            val now = DateUtils.nowDateTime()
            val totalSales = saleRepository.getTotalSalesByDateRange(open.openedAt, now)
            val totalExpenses = expenseRepository.getTotalExpensesByDateRange(open.openedAt, now)
            val expected = open.openingCash + totalSales - totalExpenses
            shiftRepository.update(
                open.copy(
                    closedAt = now,
                    countedCash = countedCash,
                    totalSales = totalSales,
                    totalExpenses = totalExpenses,
                    expectedCash = expected,
                    difference = countedCash - expected,
                    note = note.ifBlank { null },
                    status = "closed"
                )
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
