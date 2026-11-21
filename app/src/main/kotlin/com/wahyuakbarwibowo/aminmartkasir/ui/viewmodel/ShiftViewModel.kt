package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PaymentMethodEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShiftEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PaymentMethodRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PhoneHistoryRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SaleRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ShiftRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** Estimasi uang laci sekarang: modal awal + uang tunai diterima - pengeluaran selama shift. */
    val liveExpectedCash: Double
        get() = (openShift?.openingCash ?: 0.0) + liveSales - liveExpenses
}

// ponytail: metode bayar dibuat bebas oleh user, jadi non-tunai dikenali dari nama.
// Tambah flag "tunai" di payment_methods kalau nama-nama ini tidak cukup.
private val NON_CASH_KEYWORDS = listOf("qris", "transfer", "debit", "kartu", "card", "wallet", "ovo", "gopay", "dana", "shopee", "edc")

internal fun isCashMethod(method: PaymentMethodEntity?): Boolean =
    method == null || NON_CASH_KEYWORDS.none { method.name.contains(it, ignoreCase = true) }

/**
 * Uang tunai yang benar-benar masuk laci: transaksi Hutang hanya menyumbang uang mukanya,
 * kembalian tidak dihitung (min(paid, total)), metode non-tunai diabaikan. PPOB tidak
 * menyimpan metode bayar, jadi dianggap tunai.
 */
internal fun cashReceived(
    sales: List<SaleEntity>,
    digital: List<PhoneHistoryEntity>,
    methods: List<PaymentMethodEntity>
): Double {
    val byId = methods.associateBy { it.id }
    val retail = sales
        .filter { isCashMethod(it.paymentMethodId?.let(byId::get)) }
        .sumOf { minOf(it.paid, it.total).coerceAtLeast(0.0) }
    val ppob = digital.sumOf { minOf(it.paid, it.sellingPrice).coerceAtLeast(0.0) }
    return retail + ppob
}

class ShiftViewModel(
    private val shiftRepository: ShiftRepository,
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val phoneHistoryRepository: PhoneHistoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository
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
            val sales = cashReceivedBetween(open.openedAt, now)
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
            val totalSales = cashReceivedBetween(open.openedAt, now)
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

    private suspend fun cashReceivedBetween(from: String, to: String): Double = cashReceived(
        sales = saleRepository.getSalesByDateRange(from, to),
        digital = phoneHistoryRepository.getPhoneHistoryByRange(from, to),
        methods = paymentMethodRepository.allPaymentMethods.first()
    )

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
