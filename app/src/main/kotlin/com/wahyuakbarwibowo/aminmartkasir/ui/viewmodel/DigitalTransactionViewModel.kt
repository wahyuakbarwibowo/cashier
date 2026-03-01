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
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
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

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
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
                        // Select first category by default if none selected
                        selectedCategory = state.selectedCategory ?: cats.firstOrNull()?.name
                    )
                }
                // Refresh products for initial category
                _uiState.value.selectedCategory?.let { loadProductsByCategory(it) }
            }
        }
    }

    private suspend fun seedInitialData() {
        val cats = listOf("PULSA", "PLN", "E-WALLET", "PAKET DATA")
        cats.forEach { digitalCategoryRepository.insert(DigitalCategoryEntity(name = it)) }
        
        // Add some example products
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Telkomsel", name = "Pulsa 10k", nominal = 10000.0, costPrice = 10200.0, sellingPrice = 12000.0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Telkomsel", name = "Pulsa 20k", nominal = 20000.0, costPrice = 20200.0, sellingPrice = 22000.0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PULSA", provider = "Indosat", name = "Pulsa 10k", nominal = 10000.0, costPrice = 10100.0, sellingPrice = 12000.0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PLN", provider = "TOKEN", name = "PLN Token 20k", nominal = 20000.0, costPrice = 20000.0, sellingPrice = 22000.0))
        digitalProductRepository.insert(DigitalProductEntity(category = "PLN", provider = "TOKEN", name = "PLN Token 50k", nominal = 50000.0, costPrice = 50000.0, sellingPrice = 52000.0))
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, selectedProvider = null, products = emptyList()) }
        loadProductsByCategory(category)
    }

    private fun loadProductsByCategory(category: String) {
        viewModelScope.launch {
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

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isNotBlank()) {
                digitalProductRepository.searchDigitalProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            } else {
                _uiState.value.selectedCategory?.let { loadProductsByCategory(it) }
            }
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                digitalCategoryRepository.insert(DigitalCategoryEntity(name = categoryName))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addProduct(product: DigitalProductEntity) {
        viewModelScope.launch {
            try {
                digitalProductRepository.insert(product)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun processTransaction(product: DigitalProductEntity) {
        val phoneNumber = _uiState.value.targetNumber
        if (phoneNumber.isBlank()) {
            _uiState.update { it.copy(error = "Nomor tujuan harus diisi") }
            return
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
                    notes = "TRX Digital: ${product.name}",
                    paid = product.sellingPrice,
                    createdAt = dateFormat.format(Date())
                )
                
                val id = phoneHistoryRepository.insert(phoneHistory)
                val insertedTransaction = phoneHistory.copy(id = id)
                
                _uiState.update { 
                    it.copy(
                        isProcessing = false, 
                        lastProcessedTransaction = insertedTransaction,
                        successMessage = "Transaksi ${product.name} ke $phoneNumber Berhasil!",
                        targetNumber = "" 
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
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
}
