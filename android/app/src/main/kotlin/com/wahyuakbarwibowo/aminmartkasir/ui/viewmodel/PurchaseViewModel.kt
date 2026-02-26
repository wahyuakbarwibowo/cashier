package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PurchaseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class PurchaseUiState(
    val suppliers: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val cartItems: List<PurchaseCartItem> = emptyList(),
    val selectedSupplier: com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity? = null,
    val total: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PurchaseCartItem(
    val product: ProductEntity,
    val qty: Int = 1,
    val price: Double = 0.0,
    val subtotal: Double = 0.0
)

class PurchaseViewModel(
    private val supplierRepository: SupplierRepository,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                supplierRepository.allSuppliers,
                productRepository.allProducts
            ) { suppliers, products ->
                PurchaseUiState(
                    suppliers = suppliers,
                    products = products,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addToCart(product: ProductEntity, qty: Int, price: Double) {
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find { it.product.id == product.id }
        
        val newCartItems = if (existingItem != null) {
            currentState.cartItems.map {
                if (it.product.id == product.id) {
                    it.copy(qty = it.qty + qty, subtotal = (it.qty + qty) * it.price)
                } else {
                    it
                }
            }
        } else {
            currentState.cartItems + PurchaseCartItem(
                product = product,
                qty = qty,
                price = price,
                subtotal = qty * price
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

    private fun updateCart(newCartItems: List<PurchaseCartItem>) {
        val total = newCartItems.sumOf { it.subtotal }
        
        _uiState.update { 
            it.copy(
                cartItems = newCartItems,
                total = total
            ) 
        }
    }

    fun setSelectedSupplier(supplier: com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity?) {
        _uiState.update { it.copy(selectedSupplier = supplier) }
    }

    fun processPurchase() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState.cartItems.isEmpty()) {
                    _uiState.update { it.copy(error = "Keranjang kosong") }
                    return@launch
                }

                val purchase = PurchaseEntity(
                    supplierId = currentState.selectedSupplier?.id,
                    supplier = currentState.selectedSupplier?.name,
                    total = currentState.total,
                    createdAt = dateFormat.format(Date())
                )

                val purchaseItems = currentState.cartItems.map { item ->
                    PurchaseItemEntity(
                        purchaseId = 0,
                        productId = item.product.id,
                        qty = item.qty,
                        price = item.price,
                        subtotal = item.subtotal
                    )
                }

                // Insert purchase and update stock
                purchaseRepository.insertPurchaseWithItems(purchase, purchaseItems)

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
                total = 0.0,
                selectedSupplier = null
            ) 
        }
    }
}
