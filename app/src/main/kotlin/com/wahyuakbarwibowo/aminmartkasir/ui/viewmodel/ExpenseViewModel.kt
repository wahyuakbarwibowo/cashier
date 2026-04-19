package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalExpense: Double = 0.0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()
    
    private var currentPage = 0
    private val pageSize = 20
    private var isLastPage = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInitialExpenses()
    }

    private fun loadInitialExpenses() {
        currentPage = 0
        isLastPage = false
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, expenses = emptyList(), canLoadMore = true) }
            try {
                val initialExpenses = expenseRepository.getExpenses(pageSize, 0)
                val total = expenseRepository.getTotalExpensesByDateRange()
                
                if (initialExpenses.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { 
                    it.copy(
                        expenses = initialExpenses,
                        totalExpense = total,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !isLastPage
                    ) 
                }
                currentPage = 1
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun loadNextPage() {
        if (isLastPage || _uiState.value.isLoadMoreLoading || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentPage * pageSize
                val newExpenses = expenseRepository.getExpenses(pageSize, offset)
                
                if (newExpenses.size < pageSize) {
                    isLastPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        expenses = state.expenses + newExpenses,
                        isLoadMoreLoading = false,
                        canLoadMore = !isLastPage
                    )
                }
                currentPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMoreLoading = false, error = e.message) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(500)
            loadInitialExpenses()
        }
    }

    fun addExpense(category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            try {
                val expense = ExpenseEntity(
                    category = category,
                    amount = amount,
                    notes = notes,
                    createdAt = dateFormat.format(Date())
                )
                expenseRepository.insert(expense)
                loadInitialExpenses()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            try {
                expenseRepository.delete(expense)
                loadInitialExpenses()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
