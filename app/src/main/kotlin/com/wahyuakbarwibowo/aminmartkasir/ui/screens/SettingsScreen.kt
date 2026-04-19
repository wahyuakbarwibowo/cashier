package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PaymentMethodEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var shopName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var footerNote by remember { mutableStateOf("") }
    var cashierName by remember { mutableStateOf("") }
    var poinEnabled by remember { mutableStateOf(false) }
    
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    // Initialize values when shopProfile is loaded
    LaunchedEffect(uiState.shopProfile) {
        uiState.shopProfile?.let {
            shopName = it.name ?: ""
            address = it.address ?: ""
            phoneNumber = it.phoneNumber ?: ""
            footerNote = it.footerNote ?: ""
            cashierName = it.cashierName ?: ""
            poinEnabled = it.poinEnabled == 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shop Profile Section
            Text("Profil Toko", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Nama Toko") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Nomor Telepon") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cashierName,
                onValueChange = { cashierName = it },
                label = { Text("Nama Kasir") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = footerNote,
                onValueChange = { footerNote = it },
                label = { Text("Catatan Kaki Struk") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contoh: Barang yang sudah dibeli tidak dapat ditukar") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Aktifkan Sistem Poin")
                Switch(checked = poinEnabled, onCheckedChange = { poinEnabled = it })
            }

            // Save Button
            Button(
                onClick = {
                    val profile = ShopProfileEntity(
                        id = uiState.shopProfile?.id ?: 0,
                        name = shopName,
                        phoneNumber = phoneNumber,
                        address = address,
                        footerNote = footerNote,
                        cashierName = cashierName,
                        poinEnabled = if (poinEnabled) 1 else 0,
                        logoPath = uiState.shopProfile?.logoPath
                    )
                    viewModel.updateShopProfile(profile)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Simpan Pengaturan")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Payment Methods Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Metode Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddPaymentDialog = true }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Text(
                "Urutkan metode pembayaran dengan menekan tombol panah",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.paymentMethods.forEachIndexed { index, method ->
                PaymentMethodItem(
                    method = method,
                    onDelete = { viewModel.deletePaymentMethod(method) },
                    onMoveUp = { if (index > 0) viewModel.movePaymentMethod(index, index - 1) },
                    onMoveDown = { if (index < uiState.paymentMethods.size - 1) viewModel.movePaymentMethod(index, index + 1) }
                )
            }
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentMethodDialog(
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { name ->
                viewModel.addPaymentMethod(PaymentMethodEntity(name = name))
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun PaymentMethodItem(
    method: PaymentMethodEntity,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(method.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            
            // Reordering controls
            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null)
            }
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null)
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddPaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Metode Pembayaran") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Metode") },
                placeholder = { Text("Contoh: QRIS, Transfer BCA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
