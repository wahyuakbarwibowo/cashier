package com.wahyuakbarwibowo.aminmartkasir.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wahyuakbarwibowo.aminmartkasir.data.local.TenantDatabaseFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TenantDatabaseGate(storeId: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ready by remember(storeId) { mutableStateOf(!TenantDatabaseFiles.needsChoice(context, storeId)) }
    var busy by remember(storeId) { mutableStateOf(false) }
    var error by remember(storeId) { mutableStateOf<String?>(null) }

    if (ready) {
        content()
    } else {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Data kasir lama ditemukan") },
            text = {
                Text(
                    "Data lokal lama belum terkait toko. Salin ke toko akun ini? " +
                        "Pilih Mulai kosong jika data lama milik toko lain. File lama tetap disimpan." +
                        (error?.let { "\n\n$it" } ?: "")
                )
            },
            confirmButton = {
                Button(onClick = {
                    busy = true
                    scope.launch {
                        error = runCatching {
                            withContext(Dispatchers.IO) { TenantDatabaseFiles.claimLegacy(context, storeId) }
                        }.exceptionOrNull()?.message
                        busy = false
                        if (error == null) ready = true
                    }
                }, enabled = !busy) {
                    if (busy) CircularProgressIndicator() else Text("Salin data lama")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    TenantDatabaseFiles.keepLegacySeparate(context, storeId)
                    ready = true
                }, enabled = !busy) { Text("Mulai kosong") }
            }
        )
    }
}
