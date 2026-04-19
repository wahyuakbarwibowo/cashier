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

data class DigitalTransactionUiState(
    val categories: List<DigitalCategoryEntity> = emptyList(),
    val products: List<DigitalProductEntity> = emptyList(),
    val allProducts: List<DigitalProductEntity> = emptyList(),
    val phoneHistory: List<PhoneHistoryEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val selectedCategory: String? = null,
    val selectedProvider: String? = null,
    val selectedCustomer: CustomerEntity? = null,
    val selectedPaymentMethod: PaymentMethodEntity? = null,
    val targetNumber: String = "",
    val transactionNote: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val canLoadMore: Boolean = true,
    val isProcessing: Boolean = false,
    val paidAmount: String = "",
    val lastProcessedTransaction: PhoneHistoryEntity? = null,
    val successMessage: String? = null,
    val error: String? = null
)

class DigitalTransactionViewModel(
    private val digitalProductRepository: DigitalProductRepository,
    private val digitalCategoryRepository: DigitalCategoryRepository,
    private val phoneHistoryRepository: PhoneHistoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val customerRepository: CustomerRepository,
    private val receivableRepository: ReceivableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalTransactionUiState())
    val uiState: StateFlow<DigitalTransactionUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var searchJob: Job? = null
    private var loadDataJob: Job? = null
    
    private var currentHistoryPage = 0
    private val pageSize = 20
    private var isLastHistoryPage = false

    init {
        loadData()
        loadPaymentAndCustomers()
    }

    private fun loadPaymentAndCustomers() {
        viewModelScope.launch {
            combine(
                paymentMethodRepository.allPaymentMethods,
                customerRepository.allCustomers
            ) { methods, customers ->
                methods to customers
            }.collect { (methods, customers) ->
                _uiState.update { it.copy(paymentMethods = methods, customers = customers) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, searchQuery = "", selectedCategory = null) }
            delay(500)
            loadData()
        }
    }

    private fun loadData() {
        loadDataJob?.cancel()
        currentHistoryPage = 0
        isLastHistoryPage = false
        
        loadDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Seed defaults if empty
            val currentCats = digitalCategoryRepository.allDigitalCategories.first()
            if (currentCats.isEmpty()) {
                seedInitialData()
            }

            // Load history first page
            val initialHistory = phoneHistoryRepository.getPhoneHistory(pageSize, 0)
            if (initialHistory.size < pageSize) {
                isLastHistoryPage = true
            }
            currentHistoryPage = 1

            combine(
                digitalCategoryRepository.allDigitalCategories,
                digitalProductRepository.allDigitalProducts
            ) { cats, allProds ->
                cats to allProds
            }.collect { (cats, allProds) ->
                _uiState.update { state ->
                    state.copy(
                        categories = cats,
                        phoneHistory = initialHistory,
                        allProducts = allProds,
                        isLoading = false,
                        isRefreshing = false,
                        canLoadMore = !isLastHistoryPage,
                        selectedCategory = when {
                            state.selectedCategory != null && cats.any { it.name == state.selectedCategory } -> state.selectedCategory
                            else -> cats.firstOrNull()?.name
                        }
                    )
                }
                // Refresh products for initial category
                _uiState.value.selectedCategory?.let { loadProductsByCategory(it) }
            }
        }
    }

    fun loadNextHistoryPage() {
        if (isLastHistoryPage || _uiState.value.isLoadMoreLoading || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            try {
                val offset = currentHistoryPage * pageSize
                val newHistory = phoneHistoryRepository.getPhoneHistory(pageSize, offset)
                
                if (newHistory.size < pageSize) {
                    isLastHistoryPage = true
                }
                
                _uiState.update { state ->
                    state.copy(
                        phoneHistory = state.phoneHistory + newHistory,
                        isLoadMoreLoading = false,
                        canLoadMore = !isLastHistoryPage
                    )
                }
                currentHistoryPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMoreLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun seedInitialData() {
        val cats = listOf("PULSA", "PLN", "E-WALLET", "PAKET DATA")
        cats.forEachIndexed { index, name ->
            digitalCategoryRepository.insert(DigitalCategoryEntity(name = name, sortOrder = index))
        }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, selectedProvider = null, products = emptyList()) }
        loadProductsByCategory(category)
    }

    private fun loadProductsByCategory(category: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            digitalProductRepository.getDigitalProductsByCategory(category).collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun setSelectedProvider(provider: String?) {
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun setTargetNumber(number: String) {
        _uiState.update { it.copy(targetNumber = number) }
    }

    fun setTransactionNote(note: String) {
        _uiState.update { it.copy(transactionNote = note) }
    }

    fun setPaidAmount(amount: String) {
        _uiState.update { it.copy(paidAmount = amount) }
    }

    fun setSelectedCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun setSelectedPaymentMethod(method: PaymentMethodEntity?) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                digitalProductRepository.searchDigitalProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            }
        } else {
            _uiState.value.selectedCategory?.let { loadProductsByCategory(it) }
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                val currentMaxOrder = _uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1
                digitalCategoryRepository.insert(DigitalCategoryEntity(name = categoryName, sortOrder = currentMaxOrder + 1))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addProduct(product: DigitalProductEntity) {
        viewModelScope.launch {
            try {
                val currentMaxOrder = _uiState.value.allProducts
                    .filter { it.category == product.category }
                    .maxOfOrNull { it.sortOrder } ?: -1
                digitalProductRepository.insert(product.copy(sortOrder = currentMaxOrder + 1))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun processTransaction(product: DigitalProductEntity, paid: Double, method: PaymentMethodEntity?, customer: CustomerEntity?) {
        val currentState = _uiState.value
        val phoneNumber = currentState.targetNumber
        if (phoneNumber.isBlank()) {
            _uiState.update { it.copy(error = "Nomor tujuan harus diisi") }
            return
        }
        val inputNote = currentState.transactionNote.trim().ifBlank { null }
        val storedNotes = buildString {
            append("TRX Digital: ${product.name}")
            if (!inputNote.isNullOrBlank()) {
                append("\nNOTE: $inputNote")
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val phoneHistory = PhoneHistoryEntity(
                    category = product.category,
                    phoneNumber = phoneNumber,
                    customerName = customer?.name,
                    provider = product.provider,
                    amount = product.sellingPrice,
                    costPrice = product.costPrice,
                    sellingPrice = product.sellingPrice,
                    profit = product.sellingPrice - product.costPrice,
                    notes = storedNotes,
                    paid = paid,
                    createdAt = dateFormat.format(Date())
                )
                
                val id = phoneHistoryRepository.insert(phoneHistory)
                val insertedTransaction = phoneHistory.copy(id = id)
                
                // If payment method is debt, record to receivables
                if (method?.name?.contains("Hutang", ignoreCase = true) == true) {
                    receivableRepository.insert(
                        ReceivableEntity(
                            saleId = null, // digital doesn't use retail saleId
                            customerId = customer?.id,
                            amount = product.sellingPrice,
                            paidAmount = paid,
                            status = if (paid >= product.sellingPrice) "paid" else "pending",
                            createdAt = dateFormat.format(Date()),
                            notes = "Hutang dari Transaksi Digital ${product.name} ke $phoneNumber"
                        )
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false, 
                        lastProcessedTransaction = insertedTransaction,
                        successMessage = "Transaksi ${product.name} ke $phoneNumber Berhasil!"
                    )
                }
                resetTransactionForm()
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    private fun resetTransactionForm() {
        _uiState.update {
            it.copy(
                targetNumber = "",
                transactionNote = "",
                paidAmount = "",
                selectedCustomer = null,
                selectedPaymentMethod = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null, lastProcessedTransaction = null) }
    }

    fun deleteProduct(product: DigitalProductEntity) {
        viewModelScope.launch {
            try {
                digitalProductRepository.delete(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateProduct(product: DigitalProductEntity) {
        viewModelScope.launch {
            try {
                digitalProductRepository.update(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCategory(category: DigitalCategoryEntity) {
        viewModelScope.launch {
            try {
                digitalCategoryRepository.delete(category)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val categories = _uiState.value.categories
        if (fromIndex !in categories.indices || toIndex !in categories.indices || fromIndex == toIndex) return

        val mutable = categories.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        
        // Optimistic UI update
        _uiState.update { it.copy(categories = mutable) }

        // Persist to database
        viewModelScope.launch {
            mutable.forEachIndexed { index, category ->
                if (category.sortOrder != index) {
                    digitalCategoryRepository.update(category.copy(sortOrder = index))
                }
            }
        }
    }

    fun moveProduct(fromIndex: Int, toIndex: Int) {
        val products = _uiState.value.allProducts
        if (fromIndex !in products.indices || toIndex !in products.indices || fromIndex == toIndex) return

        val mutable = products.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)

        // Optimistic UI update
        val selectedCategory = _uiState.value.selectedCategory
        val selectedProducts = if (selectedCategory.isNullOrBlank()) {
            emptyList()
        } else {
            mutable.filter { it.category == selectedCategory }
        }

        _uiState.update { state ->
            state.copy(
                allProducts = mutable,
                products = if (selectedCategory.isNullOrBlank()) state.products else selectedProducts
            )
        }

        // Persist to database
        viewModelScope.launch {
            mutable.forEachIndexed { index, product ->
                if (product.sortOrder != index) {
                    digitalProductRepository.update(product.copy(sortOrder = index))
                }
            }
        }
    }
}
