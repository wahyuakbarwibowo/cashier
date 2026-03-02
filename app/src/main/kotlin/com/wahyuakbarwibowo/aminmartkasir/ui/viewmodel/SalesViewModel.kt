package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SalesTransactionUiState(
    val cartItems: List<CartItem> = emptyList(),
    val allProducts: List<ProductEntity> = emptyList(),
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
    val isLoading: Boolean = true,
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
    private val customerPointsHistoryRepository: CustomerPointsHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesTransactionUiState())
    val uiState: StateFlow<SalesTransactionUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                productRepository.allProducts,
                customerRepository.allCustomers,
                paymentMethodRepository.allPaymentMethods
            ) { products, customers, paymentMethods ->
                SalesTransactionUiState(
                    allProducts = products,
                    customers = customers,
                    paymentMethods = paymentMethods,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find { it.product.id == product.id }
        
        val newCartItems = if (existingItem != null) {
            currentState.cartItems.map {
                if (it.product.id == product.id) {
                    it.copy(qty = it.qty + 1, subtotal = (it.qty + 1) * it.price)
                } else {
                    it
                }
            }
        } else {
            currentState.cartItems + CartItem(
                product = product,
                qty = 1,
                price = product.sellingPrice,
                subtotal = product.sellingPrice
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
                it.copy(qty = qty, subtotal = qty * it.price)
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
        val total = subtotal - _uiState.value.discount
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
        val total = currentState.subtotal - discount
        val change = currentState.paid - total
        
        _uiState.update { 
            it.copy(
                discount = discount,
                total = total,
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
        val total = currentState.subtotal - currentState.discount - pointsValue
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
