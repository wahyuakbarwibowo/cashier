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
                val todayStart = dateFormat.format(Date()) + " 00:00:00"
                val todayEnd = dateFormat.format(Date()) + " 23:59:59"
                val todaySales = saleRepository.getTotalSalesByDateRange(todayStart, todayEnd)
                _uiState.update { it.copy(todaySales = todaySales) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            try {
                // 1. Weekly Sales (Load only the last 7 days from DB, highly optimized!)
                val calMin = Calendar.getInstance()
                calMin.add(Calendar.DAY_OF_YEAR, -6)
                calMin.set(Calendar.HOUR_OF_DAY, 0)
                calMin.set(Calendar.MINUTE, 0)
                calMin.set(Calendar.SECOND, 0)
                val minDateStr = dateFormat.format(calMin.time) + " 00:00:00"

                val recentSales = saleRepository.getSalesSince(minDateStr)
                val weeklyTrend = mutableListOf<Pair<String, Double>>()
                val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                
                for (i in 6 downTo 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = dateFormat.format(cal.time)
                    val label = dayFormat.format(cal.time)
                    
                    val dailyTotal = recentSales.filter { it.createdAt?.startsWith(dateStr) == true }.sumOf { it.total }
                    weeklyTrend.add(label to dailyTotal)
                }
                
                // 2. Resolve Top Products via SQLite GROUP BY (Eliminating N+1 queries!)
                val topProductsDto = saleRepository.getTopSellingProductsOnce(5)
                val resolvedTopProducts = topProductsDto.map { dto ->
                    dto.productName to dto.totalQty
                }

                _uiState.update { 
                    it.copy(
                        weeklySales = weeklyTrend,
                        topProducts = resolvedTopProducts
                    ) 
                }
            } catch (e: Exception) {
                // Ignore analytics errors
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}

private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}
