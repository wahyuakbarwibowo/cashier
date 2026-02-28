package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val totalExpenses: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            expenseRepository.allExpenses.collect { expenses ->
                val totalExpenses = expenseRepository.getTotalExpensesByDateRange()
                
                _uiState.update { 
                    it.copy(
                        expenses = expenses,
                        totalExpenses = totalExpenses,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    fun loadExpensesByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true,
                    startDate = startDate,
                    endDate = endDate
                ) 
            }
            
            expenseRepository.allExpenses.collect { expenses ->
                val filtered = expenses.filter { expense ->
                    expense.createdAt?.let { createdAt ->
                        isDateWithinRange(createdAt, startDate, endDate)
                    } ?: false
                }
                val totalExpenses = filtered.sumOf { it.amount }

                _uiState.update {
                    it.copy(
                        expenses = filtered,
                        totalExpenses = totalExpenses,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private fun isDateWithinRange(dateTime: String, startDate: String, endDate: String): Boolean {
        return try {
            val expenseDate = dateTimeFormat.parse(dateTime) ?: return false
            val start = dateFormat.parse(startDate) ?: return false
            val end = dateFormat.parse(endDate) ?: return false
            !expenseDate.before(start) && !expenseDate.after(endOfDay(end))
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

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            try {
                expenseRepository.insert(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            try {
                expenseRepository.update(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            try {
                expenseRepository.delete(expense)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
