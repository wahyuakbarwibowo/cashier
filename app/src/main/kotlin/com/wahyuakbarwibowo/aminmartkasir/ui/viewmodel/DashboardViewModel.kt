package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalProducts: Int = 0,
    val totalCustomers: Int = 0,
    val totalSales: Int = 0,
    val todaySales: Double = 0.0,
    val monthSales: Double = 0.0,
    val lowStockCount: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                productRepository.productCount,
                customerRepository.customerCount,
                saleRepository.saleCount,
                productRepository.getLowStockProducts().map { it.size }
            ) { products, customers, sales, lowStock ->
                DashboardUiState(
                    totalProducts = products,
                    totalCustomers = customers,
                    totalSales = sales,
                    lowStockCount = lowStock,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        loadSalesData()
    }

    private fun loadSalesData() {
        viewModelScope.launch {
            try {
                val todaySales = saleRepository.getTotalSalesByDateRange()

                _uiState.update { currentState ->
                    currentState.copy(
                        todaySales = todaySales,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}
