package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.BackupViewModel
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onOpenDrawer: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var jsonToSave by remember { mutableStateOf<String?>(null) }
    var confirmCloudSave by remember { mutableStateOf(false) }
    var confirmCloudRestore by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            jsonToSave?.let { json ->
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                jsonToSave = null
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(it)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
                viewModel.importData(stringBuilder.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Lainnya")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Default.Backup,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "Amankan Data Anda",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Ekspor data Anda ke file JSON untuk cadangan, atau impor kembali data dari file cadangan sebelumnya. Perhatian: Proses impor akan menghapus semua data saat ini dan menggantinya dengan data dari file cadangan.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Cadangan Cloud", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cadangan disimpan untuk toko pada akun yang sedang login. Pemulihan mengganti seluruh data lokal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { confirmCloudSave = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simpan ke Cloud")
                    }
                    OutlinedButton(
                        onClick = { confirmCloudRestore = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pulihkan dari Cloud")
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
                Text(uiState.message ?: "Mohon tunggu...", style = MaterialTheme.typography.bodySmall)
            }

            // Using empty Spacer as spacer, weight(1f) is removed because it's inside verticalScroll
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.exportData { json ->
                                jsonToSave = json
                                createDocumentLauncher.launch("aminmart_backup_${System.currentTimeMillis()}.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ekspor Data (Backup)")
                    }

                    OutlinedButton(
                        onClick = {
                            // ponytail: */* karena banyak file manager melaporkan .json sebagai
                            // octet-stream/text-plain, sehingga file jadi tidak bisa dipilih
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Impor Data (Restore)")
                    }
                }
            }
        }
    }

    // Show success/error dialog
    if (uiState.message != null && !uiState.isLoading) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text(if (uiState.isSuccess) "Berhasil" else "Perhatian") },
            text = { Text(uiState.message!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    if (confirmCloudRestore) {
        AlertDialog(
            onDismissRequest = { confirmCloudRestore = false },
            title = { Text("Pulihkan dari cloud?") },
            text = { Text("Semua data lokal akan diganti dengan cadangan cloud toko ini.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloudRestore = false
                    viewModel.restoreCloudBackup()
                }) { Text("Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloudRestore = false }) { Text("Batal") }
            }
        )
    }

    if (confirmCloudSave) {
        AlertDialog(
            onDismissRequest = { confirmCloudSave = false },
            title = { Text("Simpan cadangan ke cloud?") },
            text = {
                Text("Seluruh data lokal akan dikaitkan ke toko akun ini dan mengganti cadangan cloud toko tersebut.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloudSave = false
                    viewModel.saveCloudBackup()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCloudSave = false }) { Text("Batal") }
            }
        )
    }
}
