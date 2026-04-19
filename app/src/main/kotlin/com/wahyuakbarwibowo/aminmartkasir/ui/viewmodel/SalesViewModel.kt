package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class SalesTransactionUiState(
    val cartItems: List<CartItem> = emptyList(),
    val allProducts: List<ProductEntity> = emptyList(), // For selector, actually paginated
    val products: List<ProductEntity> = emptyList(), // Paginated list for UI
    val customers: List<CustomerEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val selectedPaymentMethod: PaymentMethodEntity? = null,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val change: Double = 0.0,
    val pointsEarned: Int = 0,
    val pointsRedeemed: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshingProducts: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null
)

data class CartItem(
    val product: ProductEntity,
    val qty: Int = 1,
    val price: Double = 0.0,
    val subtotal: Double = 0.0
)

class SalesViewModel(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val saleRepository: SaleRepository,
    private val stockHistoryRepository: StockHistoryRepository,
    private val customerPointsHistoryRepository: CustomerPointsHistoryRepository,
    private val receivableRepository: ReceivableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesTransactionUiState())
    val uiState: StateFlow<SalesTransactionUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private var currentProductPage = 0
    private val pageSize = 20
    private var isLastProductPage = false
    private var productLoadJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                customerRepository.allCustomers,
                paymentMethodRepository.allPaymentMethods
            ) { customers, paymentMethods ->
                customers to paymentMethods
            }.collect { (customers, paymentMethods) ->
                _uiState.update { 
                    it.copy(
                        customers = customers,
                        paymentMethods = paymentMethods,
                        isLoading = false
                    ) 
                }
            }
        }
        loadInitialProducts()
    }

    private fun loadInitialProducts() {
        currentProductPage = 0
        isLastProductPage = false
        productLoadJob?.cancel()
        searchJob?.cancel()
        
        productLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(products = emptyList(), canLoadMore = true) }
            try {
                val initialProducts = productRepository.getProducts(pageSize, 0)
                if (initialProducts.size < pageSize) {
                    isLastProductPage = true
                }
                _uiState.update { 
                    it.copy(
                        products = initialProducts,
                        canLoadMore = !isLastProductPage
                    ) 
                }
                currentProductPage = 1
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadNextProductPage() {
        if (isLastProductPage || _uiState.value.isLoadMoreLoading || _uiState.value.searchQuery.isNotBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentProductPage * pageSize
                val newProducts = productRepository.getProducts(pageSize, offset)
                
                if (newProducts.size < pageSize) {
                    isLastProductPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        products = state.products + newProducts,
                        isLoadMoreLoading = false,
                        canLoadMore = !isLastProductPage
                    )
                }
                currentProductPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMoreLoading = false, error = e.message) }
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        productLoadJob?.cancel()
        
        if (query.isBlank()) {
            loadInitialProducts()
            return
        }

        searchJob = viewModelScope.launch {
            try {
                productRepository.searchProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products, canLoadMore = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingProducts = true) }
            delay(500)
            loadInitialProducts()
            _uiState.update { it.copy(isRefreshingProducts = false) }
        }
    }

    private fun calculateSubtotal(product: ProductEntity, qty: Int): Double {
        val totalDiscount = product.discount * qty
        return if (product.packageQty > 0 && product.packagePrice > 0) {
            val packages = qty / product.packageQty
            val remainder = qty % product.packageQty
            (packages * product.packagePrice) + (remainder * product.sellingPrice) - totalDiscount
        } else {
            (qty * product.sellingPrice) - totalDiscount
        }
    }

    fun addToCart(product: ProductEntity) {
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find { it.product.id == product.id }
        
        val newCartItems = if (existingItem != null) {
            currentState.cartItems.map {
                if (it.product.id == product.id) {
                    val newQty = it.qty + 1
                    it.copy(qty = newQty, subtotal = calculateSubtotal(product, newQty))
                } else {
                    it
                }
            }
        } else {
            currentState.cartItems + CartItem(
                product = product,
                qty = 1,
                price = product.sellingPrice,
                subtotal = calculateSubtotal(product, 1)
            )
        }
        
        updateCart(newCartItems)
    }

    fun updateCartItemQty(productId: Long, qty: Int) {
        val currentState = _uiState.value
        if (qty <= 0) {
            removeFromCart(productId)
            return
        }
        
        val newCartItems = currentState.cartItems.map {
            if (it.product.id == productId) {
                it.copy(qty = qty, subtotal = calculateSubtotal(it.product, qty))
            } else {
                it
            }
        }
        
        updateCart(newCartItems)
    }

    fun removeFromCart(productId: Long) {
        val currentState = _uiState.value
        val newCartItems = currentState.cartItems.filter { it.product.id != productId }
        updateCart(newCartItems)
    }

    private fun updateCart(newCartItems: List<CartItem>) {
        val subtotal = newCartItems.sumOf { it.subtotal }
        val total = (subtotal - _uiState.value.discount).coerceAtLeast(0.0)
        val paid = _uiState.value.paid
        val change = paid - total
        
        _uiState.update { 
            it.copy(
                cartItems = newCartItems,
                subtotal = subtotal,
                total = total,
                change = change
            ) 
        }
    }

    fun setDiscount(discount: Double) {
        val currentState = _uiState.value
        val total = (currentState.subtotal - discount).coerceAtLeast(0.0)
        val paid = if (currentState.paid < total) total else currentState.paid
        val change = paid - total

        _uiState.update { 
            it.copy(
                discount = discount,
                total = total,
                paid = paid,
                change = change
            ) 
        }
    }
    fun setPaid(paid: Double) {
        val currentState = _uiState.value
        val change = paid - currentState.total
        
        _uiState.update { 
            it.copy(
                paid = paid,
                change = change
            ) 
        }
    }

    fun setSelectedCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun setSelectedPaymentMethod(paymentMethod: PaymentMethodEntity?) {
        _uiState.update { it.copy(selectedPaymentMethod = paymentMethod) }
    }

    fun setPointsRedeemed(points: Int) {
        val currentState = _uiState.value
        val pointsValue = points * 100 // Asumsi 1 point = Rp 100
        val total = (currentState.subtotal - currentState.discount - pointsValue).coerceAtLeast(0.0)
        val change = currentState.paid - total
        
        _uiState.update { 
            it.copy(
                pointsRedeemed = points,
                total = total,
                change = change
            ) 
        }
    }

    fun processTransaction() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState.cartItems.isEmpty()) {
                    _uiState.update { it.copy(error = "Keranjang kosong") }
                    return@launch
                }

                val sale = SaleEntity(
                    customerId = currentState.selectedCustomer?.id,
                    paymentMethodId = currentState.selectedPaymentMethod?.id,
                    total = currentState.total,
                    paid = currentState.paid,
                    change = currentState.change,
                    pointsEarned = currentState.pointsEarned,
                    pointsRedeemed = currentState.pointsRedeemed,
                    createdAt = dateFormat.format(Date())
                )

                val saleItems = currentState.cartItems.map { item ->
                    SaleItemEntity(
                        saleId = 0,
                        productId = item.product.id,
                        productName = item.product.name,
                        qty = item.qty,
                        price = item.price,
                        subtotal = item.subtotal
                    )
                }

                // Insert sale
                val saleId = saleRepository.insertSaleWithItems(sale, saleItems)

                // Update product stock
                currentState.cartItems.forEach { item ->
                    val currentProduct = productRepository.getProductById(item.product.id)
                    productRepository.decreaseStock(item.product.id, item.qty)
                    if (currentProduct != null) {
                        val after = (currentProduct.stock - item.qty).coerceAtLeast(0)
                        stockHistoryRepository.insert(
                            StockHistoryEntity(
                                productId = currentProduct.id,
                                productName = currentProduct.name,
                                changeQty = -item.qty,
                                stockBefore = currentProduct.stock,
                                stockAfter = after,
                                reason = "Transaksi penjualan #$saleId",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // Update customer points
                if (currentState.selectedCustomer != null) {
                    if (currentState.pointsEarned > 0) {
                        customerRepository.addPoints(currentState.selectedCustomer.id, currentState.pointsEarned)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = currentState.selectedCustomer.id,
                                saleId = saleId,
                                points = currentState.pointsEarned,
                                type = "EARNED",
                                notes = "Poin dari belanja",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                    if (currentState.pointsRedeemed > 0) {
                        customerRepository.deductPoints(currentState.selectedCustomer.id, currentState.pointsRedeemed)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = currentState.selectedCustomer.id,
                                saleId = saleId,
                                points = -currentState.pointsRedeemed,
                                type = "REDEEMED",
                                notes = "Penukaran poin",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // If payment method is debt, record to receivables
                if (currentState.selectedPaymentMethod?.name?.contains("Hutang", ignoreCase = true) == true) {
                    receivableRepository.insert(
                        ReceivableEntity(
                            saleId = saleId,
                            customerId = currentState.selectedCustomer?.id,
                            amount = currentState.total,
                            paidAmount = currentState.paid,
                            status = if (currentState.paid >= currentState.total) "paid" else "pending",
                            createdAt = dateFormat.format(Date()),
                            notes = "Hutang dari transaksi #$saleId"
                        )
                    )
                }

                // Clear cart
                clearCart()
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearCart() {
        _uiState.update { 
            it.copy(
                cartItems = emptyList(),
                subtotal = 0.0,
                discount = 0.0,
                total = 0.0,
                paid = 0.0,
                change = 0.0,
                pointsEarned = 0,
                pointsRedeemed = 0,
                selectedCustomer = null,
                selectedPaymentMethod = null
            ) 
        }
    }

    fun calculatePoints() {
        val currentState = _uiState.value
        val pointsEarned = (currentState.total / 10000).toInt() // Asumsi 1 point per Rp 10.000

        _uiState.update {
            it.copy(pointsEarned = pointsEarned)
        }
    }

    fun updateTransaction(saleId: Long, onUpdateComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState.cartItems.isEmpty()) {
                    _uiState.update { it.copy(error = "Keranjang kosong") }
                    return@launch
                }

                // Get existing sale
                val existingSale = saleRepository.getSaleById(saleId)
                if (existingSale == null) {
                    _uiState.update { it.copy(error = "Transaksi tidak ditemukan") }
                    return@launch
                }

                // Get existing sale items
                val existingItems = saleRepository.getSaleItemsOnce(saleId)

                // Restore stock for old items
                existingItems.forEach { item ->
                    val product = productRepository.getProductById(item.productId)
                    if (product != null) {
                        productRepository.increaseStock(item.productId, item.qty)
                        stockHistoryRepository.insert(
                            StockHistoryEntity(
                                productId = product.id,
                                productName = product.name,
                                changeQty = item.qty,
                                stockBefore = product.stock,
                                stockAfter = product.stock + item.qty,
                                reason = "Restock dari update transaksi #$saleId",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // Reverse customer points from old transaction
                if (existingSale.customerId != null) {
                    if (existingSale.pointsEarned > 0) {
                        customerRepository.deductPoints(existingSale.customerId, existingSale.pointsEarned)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = existingSale.customerId,
                                saleId = saleId,
                                points = -existingSale.pointsEarned,
                                type = "REVERSAL",
                                notes = "Reversal poin dari update transaksi",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                    if (existingSale.pointsRedeemed > 0) {
                        customerRepository.addPoints(existingSale.customerId, existingSale.pointsRedeemed)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = existingSale.customerId,
                                saleId = saleId,
                                points = existingSale.pointsRedeemed,
                                type = "REVERSAL",
                                notes = "Reversal poin dari update transaksi",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // Update sale entity
                val updatedSale = existingSale.copy(
                    customerId = currentState.selectedCustomer?.id,
                    paymentMethodId = currentState.selectedPaymentMethod?.id,
                    total = currentState.total,
                    paid = currentState.paid,
                    change = currentState.change,
                    pointsEarned = currentState.pointsEarned,
                    pointsRedeemed = currentState.pointsRedeemed
                )

                // Update sale with new items
                val newSaleItems = currentState.cartItems.map { item ->
                    SaleItemEntity(
                        saleId = saleId,
                        productId = item.product.id,
                        qty = item.qty,
                        price = item.price,
                        subtotal = item.subtotal
                    )
                }

                saleRepository.updateSaleWithItems(updatedSale, newSaleItems)

                // Decrease stock for new items
                currentState.cartItems.forEach { item ->
                    val currentProduct = productRepository.getProductById(item.product.id)
                    if (currentProduct != null) {
                        productRepository.decreaseStock(item.product.id, item.qty)
                        val after = (currentProduct.stock - item.qty).coerceAtLeast(0)
                        stockHistoryRepository.insert(
                            StockHistoryEntity(
                                productId = currentProduct.id,
                                productName = currentProduct.name,
                                changeQty = -item.qty,
                                stockBefore = currentProduct.stock,
                                stockAfter = after,
                                reason = "Transaksi penjualan update #$saleId",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // Add new customer points
                if (currentState.selectedCustomer != null) {
                    if (currentState.pointsEarned > 0) {
                        customerRepository.addPoints(currentState.selectedCustomer.id, currentState.pointsEarned)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = currentState.selectedCustomer.id,
                                saleId = saleId,
                                points = currentState.pointsEarned,
                                type = "EARNED",
                                notes = "Poin dari update transaksi",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                    if (currentState.pointsRedeemed > 0) {
                        customerRepository.deductPoints(currentState.selectedCustomer.id, currentState.pointsRedeemed)
                        customerPointsHistoryRepository.insert(
                            CustomerPointsHistoryEntity(
                                customerId = currentState.selectedCustomer.id,
                                saleId = saleId,
                                points = -currentState.pointsRedeemed,
                                type = "REDEEMED",
                                notes = "Penukaran poin dari update transaksi",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                clearCart()
                onUpdateComplete()

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
