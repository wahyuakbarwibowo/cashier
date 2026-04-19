package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.DigitalCategoryRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.DigitalProductRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PhoneHistoryRepository
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
    val selectedCategory: String? = null,
    val selectedProvider: String? = null,
    val targetNumber: String = "",
    val transactionNote: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isProcessing: Boolean = false,
    val paidAmount: String = "",
    val lastProcessedTransaction: PhoneHistoryEntity? = null,
    val successMessage: String? = null,
    val error: String? = null
)

class DigitalTransactionViewModel(
    private val digitalProductRepository: DigitalProductRepository,
    private val digitalCategoryRepository: DigitalCategoryRepository,
    private val phoneHistoryRepository: PhoneHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalTransactionUiState())
    val uiState: StateFlow<DigitalTransactionUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var searchJob: Job? = null
    private var loadDataJob: Job? = null

    init {
        loadData()
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
        loadDataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Seed defaults if empty
            val currentCats = digitalCategoryRepository.allDigitalCategories.first()
            if (currentCats.isEmpty()) {
                seedInitialData()
            }

            combine(
                digitalCategoryRepository.allDigitalCategories,
                phoneHistoryRepository.allPhoneHistory,
                digitalProductRepository.allDigitalProducts
            ) { cats, history, allProds ->
                Triple(cats, history, allProds)
            }.collect { (cats, history, allProds) ->
                _uiState.update { state ->
                    state.copy(
                        categories = cats,
                        phoneHistory = history,
                        allProducts = allProds,
                        isLoading = false,
                        isRefreshing = false,
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

    private suspend fun seedInitialData() {
        val cats = listOf("PULSA", "PLN", "E-WALLET", "PAKET DATA")
        cats.forEachIndexed { index, name ->
            digitalCategoryRepository.insert(DigitalCategoryEntity(name = name, sortOrder = index))
        }
        
        // Add some example products
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Telkomsel", name = "Pulsa 10k", nominal = 10000.0, costPrice = 10200.0, sellingPrice = 12000.0, sortOrder = 0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Telkomsel", name = "Pulsa 20k", nominal = 20000.0, costPrice = 20200.0, sellingPrice = 22000.0, sortOrder = 1))
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Indosat", name = "Pulsa 10k", nominal = 10000.0, costPrice = 10100.0, sellingPrice = 12000.0, sortOrder = 2))
        digitalProductRepository.insert(DigitalProductEntity(category = "PLN", provider = "TOKEN", name = "PLN Token 20k", nominal = 20000.0, costPrice = 20000.0, sellingPrice = 22000.0, sortOrder = 0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PLN", provider = "TOKEN", name = "PLN Token 50k", nominal = 50000.0, costPrice = 50000.0, sellingPrice = 52000.0, sortOrder = 1))
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

    fun processTransaction(product: DigitalProductEntity, paid: Double) {
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
                    customerName = null,
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
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false, 
                        lastProcessedTransaction = insertedTransaction,
                        successMessage = "Transaksi ${product.name} ke $phoneNumber Berhasil!"
                    )
                }
                resetTransactionForm()
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
                paidAmount = ""
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
