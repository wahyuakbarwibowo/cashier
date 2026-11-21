package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CategoryExpenseDto
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.STOCK_PURCHASE_CATEGORY
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
    /** Rentang kustom "yyyy-MM-dd"; kalau terisi, ini yang dipakai, bukan selectedPeriod. */
    val customStartDate: String = "",
    val customEndDate: String = "",
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

data class ExportData(
    val sales: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity>,
    val digital: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity>,
    val expenses: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity>
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
        _uiState.update { it.copy(selectedPeriod = period, customStartDate = "", customEndDate = "") }
        loadData()
    }

    fun setCustomRange(startDate: String, endDate: String) {
        _uiState.update { it.copy(customStartDate = startDate, customEndDate = endDate) }
        loadData()
    }

    fun clearCustomRange() {
        _uiState.update { it.copy(customStartDate = "", customEndDate = "", selectedPeriod = ProfitLossPeriod.ALL_TIME) }
        loadData()
    }

    /** Batas periode aktif, atau null untuk sepanjang waktu. */
    private fun activeRange(state: ProfitLossUiState): Pair<String, String>? {
        if (state.customStartDate.isNotBlank() && state.customEndDate.isNotBlank()) {
            return DateUtils.startOfDay(state.customStartDate) to DateUtils.endOfDay(state.customEndDate)
        }
        return when (state.selectedPeriod) {
            ProfitLossPeriod.TODAY -> DateUtils.startOfDay(DateUtils.nowDate()) to DateUtils.endOfDay(DateUtils.nowDate())
            ProfitLossPeriod.THIS_MONTH ->
                DateUtils.startOfDay(DateUtils.nowMonth() + "-01") to DateUtils.endOfDay(DateUtils.nowMonth() + "-31")
            ProfitLossPeriod.ALL_TIME -> null
        }
    }

    /**
     * Data mentah untuk ekspor Excel, mengikuti periode yang sedang aktif.
     * Dipakai Laporan supaya isi file sama dengan angka di layar.
     */
    suspend fun loadExportData(): ExportData {
        val range = activeRange(_uiState.value)
        val from = range?.first ?: "0000-01-01 00:00:00"
        val to = range?.second ?: "9999-12-31 23:59:59"
        return ExportData(
            sales = saleRepository.getSalesByDateRange(from, to),
            digital = phoneHistoryRepository.getPhoneHistoryByRange(from, to),
            expenses = expenseRepository.getExpensesByDateRange(from, to, Int.MAX_VALUE, 0)
        )
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val range = activeRange(_uiState.value)
                val cashierRev: Double
                val cashierProf: Double
                val digRev: Double
                val digProf: Double
                val expTot: Double
                val expCat: List<CategoryExpenseDto>

                if (range != null) {
                    val (from, to) = range
                    cashierRev = saleRepository.getTotalSalesByDateRange(from, to)
                    cashierProf = saleRepository.getTotalProfitByDateRange(from, to)
                    digRev = phoneHistoryRepository.getTotalDigitalRevenueByDateRange(from, to)
                    digProf = phoneHistoryRepository.getTotalDigitalProfitByDateRange(from, to)
                    expTot = expenseRepository.getTotalExpensesByDateRange(from, to)
                    expCat = expenseRepository.getExpensesByCategoryByDateRange(from, to)
                } else {
                    cashierRev = saleRepository.getTotalSalesAllTime()
                    cashierProf = saleRepository.getTotalProfitAllTime()
                    digRev = phoneHistoryRepository.getTotalDigitalRevenueAllTime()
                    digProf = phoneHistoryRepository.getTotalDigitalProfitAllTime()
                    expTot = expenseRepository.getTotalExpensesAllTime()
                    expCat = expenseRepository.getExpensesByCategoryAllTime()
                }

                val cashFlow = cashierRev + digRev - expTot
                // Pembelian stok dicatat juga sebagai pengeluaran (untuk arus kas), padahal
                // profit kasir sudah dikurangi harga modal barang yang terjual. Jangan potong dua kali.
                val stockPurchases = expCat.filter { it.category == STOCK_PURCHASE_CATEGORY }.sumOf { it.totalAmount }
                val netProfit = cashierProf + digProf - (expTot - stockPurchases)

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
