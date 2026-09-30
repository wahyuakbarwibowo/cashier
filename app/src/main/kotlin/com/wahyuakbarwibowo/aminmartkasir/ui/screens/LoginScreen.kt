package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRegister by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text("AminMart Kasir", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (isRegister) "Daftar akun baru" else "Masuk untuk melanjutkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!SupabaseProvider.isConfigured) {
                    Text(
                        "Supabase belum dikonfigurasi. Isi supabase.properties (lihat supabase.properties.example).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isRegister) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        enabled = !isLoading && SupabaseProvider.isConfigured,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !isLoading && SupabaseProvider.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (isRegister) "Password (min. 6 karakter)" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLoading && SupabaseProvider.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                )

                infoMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        infoMessage = null
                        scope.launch {
                            if (isRegister) {
                                val result = AuthManager.signUp(fullName, email, password)
                                if (result.isSuccess) {
                                    isRegister = false
                                    password = ""
                                    infoMessage = "Pendaftaran berhasil. Akun bisa dipakai setelah disetujui admin."
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Pendaftaran gagal."
                                }
                            } else {
                                val result = AuthManager.signIn(email, password)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message
                                        ?: "Gagal masuk. Coba lagi."
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        (!isRegister || (fullName.isNotBlank() && password.length >= 6)) &&
                        SupabaseProvider.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isRegister) "Daftar" else "Masuk")
                    }
                }

                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        errorMessage = null
                        infoMessage = null
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar")
                }
            }
        }
    }
}
