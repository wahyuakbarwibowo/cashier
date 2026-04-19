package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PaymentMethodEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.PaymentMethodRepository
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ShopProfileRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val shopProfile: ShopProfileEntity? = null,
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class SettingsViewModel(
    private val shopProfileRepository: ShopProfileRepository,
    private val paymentMethodRepository: PaymentMethodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadShopProfile()
        loadPaymentMethods()
    }

    private fun loadShopProfile() {
        viewModelScope.launch {
            shopProfileRepository.shopProfile.collect { profile ->
                _uiState.update { 
                    it.copy(
                        shopProfile = profile,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            paymentMethodRepository.allPaymentMethods.collect { paymentMethods ->
                _uiState.update { it.copy(paymentMethods = paymentMethods) }
            }
        }
    }

    fun updateShopProfile(profile: ShopProfileEntity) {
        viewModelScope.launch {
            try {
                val existingProfile = shopProfileRepository.getShopProfileOnce()
                if (existingProfile != null) {
                    shopProfileRepository.update(profile.copy(id = existingProfile.id))
                } else {
                    shopProfileRepository.insert(profile)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addPaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch {
            try {
                val currentMax = _uiState.value.paymentMethods.maxOfOrNull { it.sortOrder } ?: -1
                paymentMethodRepository.insert(paymentMethod.copy(sortOrder = currentMax + 1))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun movePaymentMethod(fromIndex: Int, toIndex: Int) {
        val methods = _uiState.value.paymentMethods
        if (fromIndex !in methods.indices || toIndex !in methods.indices || fromIndex == toIndex) return

        val mutable = methods.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)

        // Optimistic UI update
        _uiState.update { it.copy(paymentMethods = mutable) }

        // Persist to database
        viewModelScope.launch {
            mutable.forEachIndexed { index, method ->
                if (method.sortOrder != index) {
                    paymentMethodRepository.update(method.copy(sortOrder = index))
                }
            }
        }
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch {
            try {
                paymentMethodRepository.update(paymentMethod)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deletePaymentMethod(paymentMethod: PaymentMethodEntity) {
        viewModelScope.launch {
            try {
                paymentMethodRepository.delete(paymentMethod)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
