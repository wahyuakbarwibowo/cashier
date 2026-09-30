package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.remote.isPending
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.Store
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserProfile
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserRole
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.StaffViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: StaffViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var approving by remember { mutableStateOf<UserProfile?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val storeNames = remember(uiState.stores) { uiState.stores.associate { it.id to it.name } }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val pending = uiState.profiles.filter { it.isPending }
    val staff = uiState.profiles.filter { !it.isPending && it.id != viewModel.me?.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Pegawai") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Tambah Pegawai") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pending.isNotEmpty()) {
                    item { SectionTitle("Menunggu Persetujuan (${pending.size})", MaterialTheme.colorScheme.error) }
                    items(pending, key = { "p" + it.id }) { user ->
                        PendingCard(
                            user = user,
                            storeName = storeNames[user.storeId],
                            onApprove = {
                                // Admin toko langsung setujui sebagai kasir di tokonya
                                if (viewModel.isSuperAdmin) approving = user
                                else viewModel.approve(user, null, UserRole.KASIR)
                            },
                            onReject = { viewModel.setActive(user, false) }
                        )
                    }
                }
                item { SectionTitle("Pegawai (${staff.size})", MaterialTheme.colorScheme.primary) }
                if (staff.isEmpty() && !uiState.isLoading) {
                    item {
                        Text(
                            "Belum ada pegawai. Tekan \"Tambah Pegawai\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                items(staff, key = { it.id }) { user ->
                    StaffCard(
                        user = user,
                        storeName = if (viewModel.isSuperAdmin) storeNames[user.storeId] else null,
                        onToggle = { viewModel.setActive(user, !user.isActive) }
                    )
                }
            }
        }
    }

    approving?.let { user ->
        ApproveDialog(
            user = user,
            stores = uiState.stores,
            onDismiss = { approving = null },
            onConfirm = { storeId, role ->
                viewModel.approve(user, storeId, role)
                approving = null
            }
        )
    }

    if (showAdd) {
        AddStaffDialog(
            stores = if (viewModel.isSuperAdmin) uiState.stores else emptyList(),
            onDismiss = { showAdd = false },
            onConfirm = { name, email, password, storeId ->
                viewModel.createStaff(name, email, password, storeId)
                showAdd = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PendingCard(user: UserProfile, storeName: String?, onApprove: () -> Unit, onReject: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(user.displayName, fontWeight = FontWeight.Bold)
            Text(user.email, style = MaterialTheme.typography.bodySmall)
            storeName?.let { Text("Mendaftar ke: $it", style = MaterialTheme.typography.bodySmall) }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = onReject) { Text("Tolak") }
                Button(onClick = onApprove) { Text("Setujui") }
            }
        }
    }
}

@Composable
private fun StaffCard(user: UserProfile, storeName: String?, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.displayName, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    listOfNotNull(user.role.label, storeName, if (user.isActive) null else "Nonaktif").joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (user.role != UserRole.SUPER_ADMIN) {
                Switch(checked = user.isActive, onCheckedChange = { onToggle() })
            }
        }
    }
}

@Composable
private fun StorePicker(stores: List<Store>, selected: String?, onSelect: (String) -> Unit) {
    Text("Toko", style = MaterialTheme.typography.labelLarge)
    if (stores.isEmpty()) {
        Text("Belum ada toko. Tambahkan dulu di Kelola Tenant.", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
    }
    stores.forEach { store ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == store.id, onClick = { onSelect(store.id!!) })
            Text(store.name)
        }
    }
}

@Composable
private fun ApproveDialog(
    user: UserProfile,
    stores: List<Store>,
    onDismiss: () -> Unit,
    onConfirm: (String, UserRole) -> Unit
) {
    var storeId by remember { mutableStateOf(user.storeId) }
    var role by remember { mutableStateOf(UserRole.KASIR) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setujui ${user.displayName}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Peran", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(UserRole.KASIR, UserRole.ADMIN).forEach {
                        FilterChip(selected = role == it, onClick = { role = it }, label = { Text(it.label) })
                    }
                }
                StorePicker(stores, storeId) { storeId = it }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(storeId!!, role) }, enabled = storeId != null) { Text("Setujui") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun AddStaffDialog(
    stores: List<Store>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var storeId by remember { mutableStateOf<String?>(null) }
    val needStore = stores.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Pegawai (Kasir)") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama Lengkap") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Password (min. 6 karakter)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                if (needStore) StorePicker(stores, storeId) { storeId = it }
                Text("Pegawai langsung bisa login dengan email & password ini.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, email, password, storeId) },
                enabled = name.isNotBlank() && email.contains("@") && password.length >= 6 && (!needStore || storeId != null)
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
