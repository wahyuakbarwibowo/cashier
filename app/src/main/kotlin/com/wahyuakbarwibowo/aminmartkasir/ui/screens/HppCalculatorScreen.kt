package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.HppCalculatorViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HppCalculatorScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: HppCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.savedMessage) {
        if (uiState.savedMessage != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSavedMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hitung HPP") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Lainnya")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "HPP (Harga Pokok Penjualan) = (Total Harga Beli + Biaya Tambahan) / Jumlah Unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = {
                        viewModel.setSearchQuery(it)
                        viewModel.selectProduct(null)
                        expanded = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                    label = { Text("Cari & Pilih Produk (opsional)") },
                    placeholder = { Text("Nama atau kode produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded && uiState.searchQuery.isNotBlank(),
                    onDismissRequest = { expanded = false }
                ) {
                    if (uiState.products.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Produk tidak ditemukan") },
                            onClick = { },
                            enabled = false
                        )
                    } else {
                        uiState.products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "HPP saat ini: ${formatCurrency(product.purchasePrice)}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectProduct(product)
                                    viewModel.setSearchQuery(product.name)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.totalHargaBeli,
                onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) viewModel.setTotalHargaBeli(it) },
                label = { Text("Total Harga Beli") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.jumlahUnit,
                onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) viewModel.setJumlahUnit(it) },
                label = { Text("Jumlah Unit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.biayaTambahan,
                onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) viewModel.setBiayaTambahan(it) },
                label = { Text("Biaya Tambahan (ongkir, dll)") },
                prefix = { Text("Rp ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("HPP per Unit", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatCurrency(uiState.hpp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = { viewModel.saveHppToProduct() },
                enabled = uiState.selectedProduct != null && uiState.hpp > 0 && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Menyimpan..." else "Simpan sebagai Harga Modal Produk")
            }

            uiState.savedMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
