package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import com.wahyuakbarwibowo.aminmartkasir.data.repository.ShopProfileRepository
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PrinterUiState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val selectedDevice: BluetoothDevice? = null,
    val isConnected: Boolean = false,
    val isPrinting: Boolean = false,
    val shopProfile: ShopProfileEntity? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class PrinterViewModel(
    private val shopProfileRepository: ShopProfileRepository
) : ViewModel() {
    
    private var printerHelper: BluetoothPrinterHelper? = null
    
    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()
    
    fun initialize(context: Context) {
        printerHelper = BluetoothPrinterHelper(context)
        loadShopProfile()
    }
    
    private fun loadShopProfile() {
        viewModelScope.launch {
            shopProfileRepository.shopProfile.collect { profile ->
                _uiState.update { it.copy(shopProfile = profile) }
            }
        }
    }
    
    fun checkBluetoothPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun loadPairedDevices() {
        val devices = printerHelper?.getPairedDevices() ?: emptyList()
        _uiState.update { it.copy(pairedDevices = devices) }
    }
    
    fun connectDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            val success = printerHelper?.connect(device) ?: false
            _uiState.update { 
                it.copy(
                    isConnected = success,
                    selectedDevice = if (success) device else null,
                    error = if (!success) "Gagal terhubung ke printer" else null
                ) 
            }
        }
    }
    
    fun disconnectDevice() {
        printerHelper?.disconnect()
        _uiState.update { 
            it.copy(
                isConnected = false,
                selectedDevice = null
            ) 
        }
    }
    
    fun printReceipt(
        transactionId: String,
        date: String,
        items: List<BluetoothPrinterHelper.ReceiptItem>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paid: Double,
        change: Double,
        pointsEarned: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            
            try {
                val profile = _uiState.value.shopProfile
                
                printerHelper?.printReceipt(
                    shopName = profile?.name ?: "Aminmart Cashier",
                    shopAddress = profile?.address,
                    shopPhone = profile?.phoneNumber,
                    transactionId = transactionId,
                    date = date,
                    cashierName = profile?.cashierName,
                    items = items,
                    subtotal = subtotal,
                    discount = discount,
                    total = total,
                    paid = paid,
                    change = change,
                    pointsEarned = pointsEarned,
                    footerNote = profile?.footerNote
                )
                
                _uiState.update { 
                    it.copy(
                        isPrinting = false,
                        successMessage = "Berhasil mencetak struk"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isPrinting = false,
                        error = "Gagal mencetak: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
