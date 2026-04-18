package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.PrinterViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterDialog(
    onDismiss: () -> Unit,
    onDeviceConnected: () -> Unit,
    transactionData: LastTransactionData? = null,
    digitalTransaction: PhoneHistoryEntity? = null,
    viewModelFactory: Factory? = null,
    viewModel: PrinterViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (viewModel.isBluetoothEnabled()) {
            if (viewModel.checkBluetoothPermission(context)) {
                viewModel.loadPairedDevices()
            }
        } else {
            onDismiss() // Close if user cancels enabling bluetooth
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            if (!viewModel.isBluetoothEnabled()) {
                bluetoothEnableLauncher.launch(
                    android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            } else {
                viewModel.loadPairedDevices()
            }
        } else {
            showPermissionRationale = true
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        
        // 1. Check permissions first (Required for ACTION_REQUEST_ENABLE on API 31+)
        if (!viewModel.checkBluetoothPermission(context)) {
            val neededPermissions = mutableListOf<String>()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                neededPermissions.add(Manifest.permission.BLUETOOTH)
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permissionLauncher.launch(neededPermissions.toTypedArray())
        } 
        // 2. If permissions granted but bluetooth is OFF, request enable
        else if (!viewModel.isBluetoothEnabled()) {
            bluetoothEnableLauncher.launch(
                android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        } 
        // 3. Everything OK, load devices
        else {
            viewModel.loadPairedDevices()
        }
    }
    
    // Auto-print when connected and transaction data is provided
    LaunchedEffect(uiState.isConnected, transactionData, digitalTransaction) {
        if (uiState.isConnected && !uiState.isPrinting) {
            val transactionDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            
            if (transactionData != null) {
                viewModel.printReceipt(
                    transactionId = transactionData.transactionId,
                    date = transactionDate,
                    items = transactionData.items,
                    subtotal = transactionData.subtotal,
                    discount = transactionData.discount,
                    total = transactionData.total,
                    paid = transactionData.paid,
                    change = transactionData.change,
                    pointsEarned = transactionData.pointsEarned
                )
            } else if (digitalTransaction != null) {
                val parsedProductName = parseDigitalProductName(digitalTransaction.notes)
                val parsedNote = parseDigitalNote(digitalTransaction.notes)
                viewModel.printDigitalReceipt(
                    transactionId = "TRX-DIG-${digitalTransaction.id}",
                    date = transactionDate,
                    category = digitalTransaction.category,
                    provider = digitalTransaction.provider ?: "-",
                    targetNumber = digitalTransaction.phoneNumber ?: "-",
                    productName = parsedProductName,
                    sellingPrice = digitalTransaction.sellingPrice,
                    notes = parsedNote,
                    paid = digitalTransaction.paid,
                    change = digitalTransaction.paid - digitalTransaction.sellingPrice
                )
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Printer Bluetooth")
                if (uiState.isConnected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Terhubung",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showPermissionRationale) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Izin Bluetooth diperlukan untuk terhubung ke printer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                if (uiState.pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada perangkat Bluetooth yang dipasangkan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.pairedDevices, key = { it.address }) { device ->
                            Card(
                                onClick = {
                                    viewModel.connectDevice(device)
                                    onDeviceConnected()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (uiState.selectedDevice?.address == device.address) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Devices,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = device.name ?: "Unknown Device",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = device.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (uiState.selectedDevice?.address == device.address && uiState.isConnected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isConnected) {
                    Button(
                        onClick = {
                            // Print test receipt
                            viewModel.printReceipt(
                                transactionId = "TEST-001",
                                date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                items = listOf(
                                    BluetoothPrinterHelper.ReceiptItem("TEST PRINT", 1, 1000.0, 1000.0)
                                ),
                                subtotal = 1000.0,
                                discount = 0.0,
                                total = 1000.0,
                                paid = 1000.0,
                                change = 0.0
                            )
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Test Print")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        },
        dismissButton = {
            if (uiState.isConnected) {
                TextButton(
                    onClick = {
                        viewModel.disconnectDevice()
                    }
                ) {
                    Text("Disconnect")
                }
            }
        }
    )
}

private fun parseDigitalProductName(rawNotes: String?): String {
    if (rawNotes.isNullOrBlank()) return "Produk Digital"
    return rawNotes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("TRX Digital: ") }
        ?.removePrefix("TRX Digital: ")
        ?.ifBlank { "Produk Digital" }
        ?: "Produk Digital"
}

private fun parseDigitalNote(rawNotes: String?): String? {
    if (rawNotes.isNullOrBlank()) return null
    return rawNotes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("NOTE: ") }
        ?.removePrefix("NOTE: ")
        ?.trim()
        ?.ifBlank { null }
}
