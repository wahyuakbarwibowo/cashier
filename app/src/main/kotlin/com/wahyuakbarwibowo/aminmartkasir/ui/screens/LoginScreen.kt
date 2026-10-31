package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wahyuakbarwibowo.aminmartkasir.R
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.SupabaseProvider
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRegister by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val fieldsEnabled = !isLoading && SupabaseProvider.isConfigured
    val emailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordTooShort = isRegister && password.isNotEmpty() && password.length < 6
    val passwordMismatch = isRegister && confirmPassword.isNotEmpty() && confirmPassword != password
    val canSubmit = fieldsEnabled &&
        email.isNotBlank() && emailValid &&
        password.isNotBlank() &&
        (!isRegister || (fullName.isNotBlank() && password.length >= 6 && confirmPassword == password))

    val submit: () -> Unit = submit@{
        if (!canSubmit) return@submit
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null
        infoMessage = null
        scope.launch {
            if (isRegister) {
                val result = AuthManager.signUp(fullName, email, password)
                if (result.isSuccess) {
                    isRegister = false
                    password = ""
                    confirmPassword = ""
                    infoMessage = "Pendaftaran berhasil. Cek email Anda untuk konfirmasi, lalu tunggu persetujuan admin."
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Pendaftaran gagal."
                }
            } else {
                val result = AuthManager.signIn(email, password)
                if (result.isFailure) {
                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal masuk. Coba lagi."
                }
            }
            isLoading = false
        }
    }

    val passwordTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val visibilityToggle: @Composable () -> Unit = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"
            )
        }
    }
    val fieldText = MaterialTheme.typography.bodyLarge
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Image(
                painter = painterResource(R.drawable.aminmart_logo),
                contentDescription = "Logo Aminmart",
                modifier = Modifier.size(120.dp)
            )
            Text("Aminmart Kasir", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (isRegister) "Daftar akun baru" else "Masuk untuk melanjutkan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!SupabaseProvider.isConfigured) {
                Text(
                    "Supabase belum dikonfigurasi. Isi supabase.properties (lihat supabase.properties.example).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isRegister) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    textStyle = fieldText,
                    enabled = fieldsEnabled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = nextAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                placeholder = { Text("contoh@gmail.com") },
                singleLine = true,
                textStyle = fieldText,
                enabled = fieldsEnabled,
                isError = !emailValid,
                supportingText = if (!emailValid) {
                    { Text("Format email belum benar") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = nextAction,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                textStyle = fieldText,
                visualTransformation = passwordTransformation,
                trailingIcon = visibilityToggle,
                enabled = fieldsEnabled,
                isError = passwordTooShort,
                supportingText = if (isRegister) {
                    { Text("Minimal 6 karakter") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isRegister) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { submit() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (isRegister) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Ulangi Password") },
                    singleLine = true,
                    textStyle = fieldText,
                    visualTransformation = passwordTransformation,
                    trailingIcon = visibilityToggle,
                    enabled = fieldsEnabled,
                    isError = passwordMismatch,
                    supportingText = if (passwordMismatch) {
                        { Text("Password tidak sama") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            infoMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }

            Button(
                onClick = submit,
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isRegister) "Daftar" else "Masuk", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (!isRegister) {
                TextButton(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        if (email.isBlank() || !emailValid) {
                            errorMessage = "Isi email Anda dulu di kolom Email, lalu tekan Lupa Password lagi."
                            return@TextButton
                        }
                        isLoading = true
                        scope.launch {
                            val result = AuthManager.resetPassword(email)
                            if (result.isSuccess) {
                                infoMessage = "Link untuk membuat password baru sudah dikirim ke $email. Cek kotak masuk atau folder spam."
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Gagal mengirim email. Coba lagi."
                            }
                            isLoading = false
                        }
                    },
                    enabled = fieldsEnabled,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Lupa Password?", style = MaterialTheme.typography.titleSmall)
                }
            }

            TextButton(
                onClick = {
                    isRegister = !isRegister
                    confirmPassword = ""
                    errorMessage = null
                    infoMessage = null
                },
                enabled = !isLoading,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
