package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalManagementScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DigitalTransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Produk", "Kategori")
    
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<DigitalProductEntity?>(null) }
    
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Produk Digital") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == 0) {
                    editingProduct = null
                    showProductDialog = true
                } else {
                    categoryNameInput = ""
                    showCategoryDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> ProductListSection(
                        products = uiState.allProducts,
                        onEdit = {
                            editingProduct = it
                            showProductDialog = true
                        },
                        onDelete = { viewModel.deleteProduct(it) },
                        onReorder = { from, to -> viewModel.moveProduct(from, to) }
                    )
                    1 -> CategoryListSection(
                        categories = uiState.categories,
                        onDelete = { viewModel.deleteCategory(it) },
                        onReorder = { from, to -> viewModel.moveCategory(from, to) }
                    )
                }
            }
        }
    }

    if (showProductDialog) {
        DigitalProductFormDialog(
            product = editingProduct,
            categories = uiState.categories,
            onDismiss = { showProductDialog = false },
            onConfirm = { product ->
                if (editingProduct == null) {
                    viewModel.addProduct(product)
                } else {
                    viewModel.updateProduct(product)
                }
                showProductDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Tambah Kategori Baru") },
            text = {
                OutlinedTextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (categoryNameInput.isNotBlank()) {
                            viewModel.addCategory(categoryNameInput)
                            showCategoryDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListSection(
    products: List<DigitalProductEntity>,
    onEdit: (DigitalProductEntity) -> Unit,
    onDelete: (DigitalProductEntity) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada produk digital")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            itemsIndexed(products, key = { _, product -> product.id }) { index, product ->
                val isDragging = draggedItemIndex == index
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 1.dp,
                    animationSpec = tween(150),
                    label = "elevation"
                )

                DraggableProductCard(
                    product = product,
                    index = index,
                    isDragging = isDragging,
                    elevation = elevation,
                    onDragStart = { draggedItemIndex = index },
                    onDrag = { delta ->
                        dragOffset += delta
                        if (dragOffset > 100f && index < products.lastIndex) {
                            onReorder(index, index + 1)
                            dragOffset = 0f
                        } else if (dragOffset < -100f && index > 0) {
                            onReorder(index, index - 1)
                            dragOffset = 0f
                        }
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                        dragOffset = 0f
                    },
                    onEdit = { onEdit(product) },
                    onDelete = { onDelete(product) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraggableProductCard(
    product: DigitalProductEntity,
    index: Int,
    isDragging: Boolean,
    elevation: Dp,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = if (isDragging) 0.9f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(isDragging) {
                if (!isDragging) return@pointerInput
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${product.provider} | ${product.category}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Jual: ${formatCurrency(product.sellingPrice)} | Modal: ${formatCurrency(product.costPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                }
                            )
                        }
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Seret untuk urutkan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListSection(
    categories: List<DigitalCategoryEntity>,
    onDelete: (DigitalCategoryEntity) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    if (categories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada kategori digital")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                val isDragging = draggedItemIndex == index
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 1.dp,
                    animationSpec = tween(150),
                    label = "elevation"
                )

                DraggableCategoryCard(
                    category = category,
                    index = index,
                    isDragging = isDragging,
                    elevation = elevation,
                    onDragStart = { draggedItemIndex = index },
                    onDrag = { delta ->
                        dragOffset += delta
                        if (dragOffset > 100f && index < categories.lastIndex) {
                            onReorder(index, index + 1)
                            dragOffset = 0f
                        } else if (dragOffset < -100f && index > 0) {
                            onReorder(index, index - 1)
                            dragOffset = 0f
                        }
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                        dragOffset = 0f
                    },
                    onDelete = { onDelete(category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraggableCategoryCard(
    category: DigitalCategoryEntity,
    index: Int,
    isDragging: Boolean,
    elevation: Dp,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = if (isDragging) 0.9f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(isDragging) {
                if (!isDragging) return@pointerInput
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                }
                            )
                        }
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = "Seret untuk urutkan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalProductFormDialog(
    product: DigitalProductEntity?,
    categories: List<DigitalCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (DigitalProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: categories.firstOrNull()?.name ?: "") }
    var provider by remember { mutableStateOf(product?.provider ?: "") }
    var nominal by remember { mutableStateOf(product?.nominal?.toString() ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Tambah Produk Digital" else "Edit Produk Digital") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk (misal: Pulsa 10rb)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider (Provider: Telkomsel, PLN, dll)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = nominal,
                    onValueChange = { nominal = it },
                    label = { Text("Nominal (Angka)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Harga Modal (HPP)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Harga Jual") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && category.isNotBlank()) {
                        onConfirm(
                            DigitalProductEntity(
                                id = product?.id ?: 0L,
                                category = category,
                                provider = provider,
                                name = name,
                                nominal = nominal.toDoubleOrNull() ?: 0.0,
                                costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                                sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                                createdAt = product?.createdAt ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            )
                        )
                    }
                }
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
