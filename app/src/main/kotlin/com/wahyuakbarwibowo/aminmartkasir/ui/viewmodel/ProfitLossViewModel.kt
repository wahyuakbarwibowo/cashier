package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CategoryExpenseDto
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PhoneHistoryRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ProfitLossPeriod(val label: String) {
    TODAY("Hari Ini"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua")
}

data class ProfitLossUiState(
    val selectedPeriod: ProfitLossPeriod = ProfitLossPeriod.THIS_MONTH,
    val cashierRevenue: Double = 0.0,
    val cashierProfit: Double = 0.0,
    val digitalRevenue: Double = 0.0,
    val digitalProfit: Double = 0.0,
    val totalExpense: Double = 0.0,
    val estimatedCashFlow: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val expenseByCategory: List<CategoryExpenseDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfitLossViewModel(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val phoneHistoryRepository: PhoneHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfitLossUiState())
    val uiState: StateFlow<ProfitLossUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setPeriod(period: ProfitLossPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val period = _uiState.value.selectedPeriod
                val cashierRev: Double
                val cashierProf: Double
                val digRev: Double
                val digProf: Double
                val expTot: Double
                val expCat: List<CategoryExpenseDto>

                when (period) {
                    ProfitLossPeriod.TODAY -> {
                        val todayStart = DateUtils.nowDate() + " 00:00:00"
                        val todayEnd = DateUtils.nowDate() + " 23:59:59"
                        
                        cashierRev = saleRepository.getTotalSalesByDateRange(todayStart, todayEnd)
                        cashierProf = saleRepository.getTotalProfitByDateRange(todayStart, todayEnd)
                        digRev = phoneHistoryRepository.getTotalDigitalRevenueByDateRange(todayStart, todayEnd)
                        digProf = phoneHistoryRepository.getTotalDigitalProfitByDateRange(todayStart, todayEnd)
                        expTot = expenseRepository.getTotalExpensesByDateRange(todayStart, todayEnd)
                        expCat = expenseRepository.getExpensesByCategoryByDateRange(todayStart, todayEnd)
                    }
                    ProfitLossPeriod.THIS_MONTH -> {
                        val monthStart = DateUtils.nowMonth() + "-01 00:00:00"
                        val monthEnd = DateUtils.nowMonth() + "-31 23:59:59"
                        
                        cashierRev = saleRepository.getTotalSalesByDateRange(monthStart, monthEnd)
                        cashierProf = saleRepository.getTotalProfitByDateRange(monthStart, monthEnd)
                        digRev = phoneHistoryRepository.getTotalDigitalRevenueByDateRange(monthStart, monthEnd)
                        digProf = phoneHistoryRepository.getTotalDigitalProfitByDateRange(monthStart, monthEnd)
                        expTot = expenseRepository.getTotalExpensesByDateRange(monthStart, monthEnd)
                        expCat = expenseRepository.getExpensesByCategoryByDateRange(monthStart, monthEnd)
                    }
                    ProfitLossPeriod.ALL_TIME -> {
                        cashierRev = saleRepository.getTotalSalesAllTime()
                        cashierProf = saleRepository.getTotalProfitAllTime()
                        digRev = phoneHistoryRepository.getTotalDigitalRevenueAllTime()
                        digProf = phoneHistoryRepository.getTotalDigitalProfitAllTime()
                        expTot = expenseRepository.getTotalExpensesAllTime()
                        expCat = expenseRepository.getExpensesByCategoryAllTime()
                    }
                }

                val cashFlow = cashierRev + digRev - expTot
                val netProfit = cashierProf + digProf - expTot

                _uiState.update {
                    it.copy(
                        cashierRevenue = cashierRev,
                        cashierProfit = cashierProf,
                        digitalRevenue = digRev,
                        digitalProfit = digProf,
                        totalExpense = expTot,
                        estimatedCashFlow = cashFlow,
                        estimatedProfit = netProfit,
                        expenseByCategory = expCat,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
