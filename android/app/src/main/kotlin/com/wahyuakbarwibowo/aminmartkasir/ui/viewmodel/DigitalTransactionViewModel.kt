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
    val phoneHistory: List<PhoneHistoryEntity> = emptyList(),
    val selectedCategory: String = "PULSA",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
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
        loadCategories()
        loadProducts()
        loadPhoneHistory()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            digitalCategoryRepository.allDigitalCategories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            digitalProductRepository.allDigitalProducts.collect { products ->
                _uiState.update { 
                    it.copy(
                        products = products,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    private fun loadPhoneHistory() {
        viewModelScope.launch {
            phoneHistoryRepository.allPhoneHistory.collect { history ->
                _uiState.update { it.copy(phoneHistory = history) }
            }
        }
    }

    fun setSelectedCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        
        viewModelScope.launch {
            digitalProductRepository.getDigitalProductsByCategory(category).collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        viewModelScope.launch {
            if (query.isNotBlank()) {
                digitalProductRepository.searchDigitalProducts(query).collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            } else {
                loadProducts()
            }
        }
    }

    fun addCategory(category: DigitalCategoryEntity) {
        viewModelScope.launch {
            try {
                digitalCategoryRepository.insert(category)
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

    fun processTransaction(phoneNumber: String, product: DigitalProductEntity, customerName: String?) {
        viewModelScope.launch {
            try {
                val phoneHistory = PhoneHistoryEntity(
                    category = product.category,
                    phoneNumber = phoneNumber,
                    customerName = customerName,
                    provider = product.provider,
                    amount = product.sellingPrice,
                    costPrice = product.costPrice,
                    sellingPrice = product.sellingPrice,
                    profit = product.sellingPrice - product.costPrice,
                    notes = "",
                    paid = product.sellingPrice,
                    createdAt = dateFormat.format(Date())
                )
                
                phoneHistoryRepository.insert(phoneHistory)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
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
