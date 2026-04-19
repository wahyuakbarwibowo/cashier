package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ExpenseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.StockHistoryRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PurchaseRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.SupplierRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class PurchaseUiState(
    val suppliers: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val allProducts: List<ProductEntity> = emptyList(),
    val cartItems: List<PurchaseCartItem> = emptyList(),
    val selectedSupplier: com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity? = null,
    val total: Double = 0.0,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val successMessage: String? = null,
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
    private val productRepository: ProductRepository,
    private val stockHistoryRepository: StockHistoryRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            combine(
                supplierRepository.allSuppliers,
                productRepository.allProducts,
                _searchQuery
            ) { suppliers, allProducts, query ->
                val filteredProducts = if (query.isBlank()) {
                    allProducts
                } else {
                    allProducts.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.code?.contains(query, ignoreCase = true) == true 
                    }
                }
                Triple(suppliers, allProducts, filteredProducts)
            }.collect { (suppliers, allProducts, filteredProducts) ->
                _uiState.update { current ->
                    val selectedSupplier = current.selectedSupplier?.let { selected ->
                        suppliers.find { it.id == selected.id }
                    }
                    current.copy(
                        suppliers = suppliers,
                        allProducts = allProducts,
                        products = filteredProducts,
                        searchQuery = _searchQuery.value,
                        selectedSupplier = selectedSupplier,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            delay(500)
            loadInitialData()
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

    fun addSupplier(name: String, phoneNumber: String, address: String) {
        viewModelScope.launch {
            try {
                val supplier = com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity(
                    name = name,
                    phone = phoneNumber,
                    address = address
                )
                val id = supplierRepository.insert(supplier)
                val newSupplier = supplier.copy(id = id)
                _uiState.update { it.copy(selectedSupplier = newSupplier, successMessage = "Supplier $name berhasil ditambahkan") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
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

                // Add to StockHistory and record Expense
                currentState.cartItems.forEach { item ->
                    val currentProduct = productRepository.getProductById(item.product.id)
                    if (currentProduct != null) {
                        // The stock was just increased by the repository, so the stock before was currentProduct.stock - item.qty
                        val stockAfter = currentProduct.stock
                        val stockBefore = (stockAfter - item.qty).coerceAtLeast(0)
                        
                        stockHistoryRepository.insert(
                            StockHistoryEntity(
                                productId = currentProduct.id,
                                productName = currentProduct.name,
                                changeQty = item.qty,
                                stockBefore = stockBefore,
                                stockAfter = stockAfter,
                                reason = "Pembelian dari supplier",
                                createdAt = dateFormat.format(Date())
                            )
                        )
                    }
                }

                // Record the total purchase cost as an Expense
                expenseRepository.insert(
                    ExpenseEntity(
                        category = "Pembelian Stok",
                        amount = currentState.total,
                        notes = "Pembelian barang ke supplier ${currentState.selectedSupplier?.name ?: "Tanpa Nama"}",
                        createdAt = dateFormat.format(Date())
                    )
                )

                // Clear cart
                clearCart()
                _uiState.update {
                    it.copy(
                        successMessage = "Pembelian berhasil disimpan"
                    )
                }
                
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

    fun clearMessages() {
        _uiState.update {
            it.copy(
                successMessage = null,
                error = null
            )
        }
    }
}
