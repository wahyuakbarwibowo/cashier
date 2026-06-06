package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShiftEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ShiftViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import com.wahyuakbarwibowo.aminmartkasir.utils.RupiahVisualTransformation
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: ShiftViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Shift Kasir") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val open = uiState.openShift
                if (open != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Shift Terbuka", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Mulai: ${open.openedAt}", style = MaterialTheme.typography.labelMedium)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            ShiftRow("Modal Awal", formatCurrency(open.openingCash))
                            ShiftRow("Penjualan", formatCurrency(uiState.liveSales))
                            ShiftRow("Pengeluaran", "- ${formatCurrency(uiState.liveExpenses)}")
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            ShiftRow("Estimasi Uang Laci", formatCurrency(uiState.liveExpectedCash), bold = true)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showCloseDialog = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Tutup Shift", fontWeight = FontWeight.Bold) }
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Belum ada shift terbuka", fontWeight = FontWeight.Bold)
                            Text(
                                "Buka shift untuk mulai mencatat kas laci.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showOpenDialog = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Buka Shift", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            val closedShifts = uiState.history.filter { it.status == "closed" }
            if (closedShifts.isNotEmpty()) {
                item {
                    Text("Riwayat Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(closedShifts, key = { it.id }) { shift ->
                    ClosedShiftCard(shift)
                }
            }
        }
    }

    if (showOpenDialog) {
        CashInputDialog(
            title = "Buka Shift",
            label = "Modal Awal (uang laci)",
            confirmText = "Buka",
            noteEnabled = false,
            onConfirm = { cash, _ ->
                viewModel.openShift(cash)
                showOpenDialog = false
            },
            onDismiss = { showOpenDialog = false }
        )
    }

    if (showCloseDialog) {
        CashInputDialog(
            title = "Tutup Shift",
            label = "Uang Laci Terhitung",
            confirmText = "Tutup",
            noteEnabled = true,
            helper = "Estimasi sistem: ${formatCurrency(uiState.liveExpectedCash)}",
            onConfirm = { cash, note ->
                viewModel.closeShift(cash, note)
                showCloseDialog = false
            },
            onDismiss = { showCloseDialog = false }
        )
    }
}

@Composable
private fun ShiftRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ClosedShiftCard(shift: ShiftEntity) {
    val diff = shift.difference ?: 0.0
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${shift.openedAt} → ${shift.closedAt ?: "-"}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            ShiftRow("Modal Awal", formatCurrency(shift.openingCash))
            ShiftRow("Penjualan", formatCurrency(shift.totalSales))
            ShiftRow("Pengeluaran", "- ${formatCurrency(shift.totalExpenses)}")
            ShiftRow("Estimasi", formatCurrency(shift.expectedCash ?: 0.0))
            ShiftRow("Terhitung", formatCurrency(shift.countedCash ?: 0.0))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Selisih", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = (if (diff > 0) "+ " else if (diff < 0) "- " else "") + formatCurrency(abs(diff)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        diff < 0 -> MaterialTheme.colorScheme.error
                        diff > 0 -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            if (!shift.note.isNullOrBlank()) {
                Text("Catatan: ${shift.note}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CashInputDialog(
    title: String,
    label: String,
    confirmText: String,
    noteEnabled: Boolean,
    helper: String? = null,
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var cashText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirm(cashText.toDoubleOrNull() ?: 0.0, note) },
                enabled = cashText.isNotBlank()
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) cashText = it },
                    label = { Text(label) },
                    prefix = { Text("Rp ") },
                    visualTransformation = RupiahVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (helper != null) {
                    Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (noteEnabled) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    )
}
