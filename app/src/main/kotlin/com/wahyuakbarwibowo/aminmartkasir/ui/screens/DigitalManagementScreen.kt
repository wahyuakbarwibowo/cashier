package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalManagementScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    viewModel: DigitalTransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Produk Digital") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Categories Management (Draggable)
                SectionHeader(
                    title = "Kategori", 
                    onAdd = { showAddCategoryDialog = true }
                )
                
                CategoryList(
                    categories = uiState.categories,
                    onMove = { from, to -> viewModel.moveCategory(from, to) },
                    onDelete = { viewModel.deleteCategory(it) }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 2. Products Management (Draggable within category)
                SectionHeader(
                    title = "Produk (${uiState.selectedCategory ?: "Pilih Kategori"})", 
                    onAdd = { 
                        if (uiState.selectedCategory != null) {
                            showAddProductDialog = true 
                        }
                    }
                )

                if (uiState.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.categories) { cat ->
                            FilterChip(
                                selected = uiState.selectedCategory == cat.name,
                                onClick = { viewModel.setSelectedCategory(cat.name) },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                if (uiState.products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada produk di kategori ini")
                    }
                } else {
                    ProductList(
                        modifier = Modifier.weight(1f),
                        products = uiState.products,
                        onMove = { from, to -> viewModel.moveProduct(from, to) },
                        onDelete = { viewModel.deleteProduct(it) }
                    )
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { 
                viewModel.addCategory(it)
                showAddCategoryDialog = false
            }
        )
    }

    if (showAddProductDialog) {
        AddDigitalProductDialog(
            category = uiState.selectedCategory!!,
            onDismiss = { showAddProductDialog = false },
            onConfirm = { 
                viewModel.addProduct(it)
                showAddProductDialog = false
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.AddCircle, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryList(
    categories: List<DigitalCategoryEntity>,
    onMove: (Int, Int) -> Unit,
    onDelete: (DigitalCategoryEntity) -> Unit
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(categories, key = { _, cat -> cat.id }) { index, cat ->
            CategoryItem(
                category = cat,
                isDragged = draggedIndex == index,
                onDelete = { onDelete(cat) }
                // Dragging in LazyRow is complex, keeping simple for now
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: DigitalCategoryEntity,
    isDragged: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .clip(MaterialTheme.shapes.medium),
        color = if (isDragged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = if (isDragged) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun ProductList(
    modifier: Modifier = Modifier,
    products: List<DigitalProductEntity>,
    onMove: (Int, Int) -> Unit,
    onDelete: (DigitalProductEntity) -> Unit
) {
    // Basic list for management
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductManagementCard(product, onDelete = { onDelete(product) })
        }
    }
}

@Composable
fun ProductManagementCard(product: DigitalProductEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("${product.provider} | Modal: ${formatCurrency(product.costPrice)}", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatCurrency(product.sellingPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kategori") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Kategori") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDigitalProductDialog(
    category: String,
    onDismiss: () -> Unit,
    onConfirm: (DigitalProductEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Produk Digital") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kategori: $category", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Produk") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider (Telkomsel/PLN/dll)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = costPrice, 
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) costPrice = it }, 
                    label = { Text("Harga Modal") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") }
                )
                OutlinedTextField(
                    value = sellingPrice, 
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) sellingPrice = it }, 
                    label = { Text("Harga Jual") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(DigitalProductEntity(
                        name = name,
                        category = category,
                        provider = provider,
                        costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        nominal = 0.0,
                        sortOrder = 0
                    ))
                },
                enabled = name.isNotBlank() && costPrice.isNotBlank() && sellingPrice.isNotBlank()
            ) {
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
