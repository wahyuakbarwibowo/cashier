package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val totalProducts: Int = 0,
    val totalCustomers: Int = 0,
    val totalSales: Int = 0,
    val todaySales: Double = 0.0,
    val monthSales: Double = 0.0,
    val lowStockCount: Int = 0,
    val weeklySales: List<Pair<String, Double>> = emptyList(),
    val topProducts: List<Pair<String, Int>> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DashboardViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
                _uiState.update { 
                    it.copy(
                        totalProducts = products,
                        totalCustomers = customers,
                        totalSales = sales,
                        lowStockCount = lowStock,
                        isLoading = false
                    )
                }
            }.collect()
        }

        loadSalesData()
        loadAnalyticsData()
    }

    private fun loadSalesData() {
        viewModelScope.launch {
            try {
                val todaySales = saleRepository.getTotalSalesByDateRange()
                _uiState.update { it.copy(todaySales = todaySales) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            try {
                // Load weekly sales (last 7 days)
                val allSales = saleRepository.allSales.first()
                val calendar = Calendar.getInstance()
                val weeklyTrend = mutableListOf<Pair<String, Double>>()
                
                val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = dateFormat.format(cal.time)
                    val label = dayFormat.format(cal.time)
                    
                    val dailyTotal = allSales.filter { it.createdAt?.startsWith(dateStr) == true }.sumOf { it.total }
                    weeklyTrend.add(label to dailyTotal)
                }
                
                // Load Top 5 Products
                // Note: In real app, this should be a specialized DAO query
                val allItems = mutableListOf<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity>()
                allSales.take(50).forEach { sale ->
                    allItems.addAll(saleRepository.getSaleItemsOnce(sale.id))
                }
                
                val topProds = allItems.groupBy { it.productName }
                    .mapValues { it.value.sumOf { item -> item.qty } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                _uiState.update { 
                    it.copy(
                        weeklySales = weeklyTrend,
                        topProducts = topProds
                    ) 
                }
            } catch (e: Exception) {
                // Ignore analytics errors for stability
            }
        }
    }

    private fun _uiState_update_error(message: String?) {
        // Placeholder for internal update if needed
    }

    fun refresh() {
        loadDashboardData()
    }
}

// Extension to allow state update with error
private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}
