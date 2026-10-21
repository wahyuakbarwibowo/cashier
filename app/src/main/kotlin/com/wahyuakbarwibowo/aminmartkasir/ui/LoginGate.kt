package com.wahyuakbarwibowo.aminmartkasir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserProfile
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.LoginScreen

@Composable
fun LoginGate(onSignedIn: @Composable (UserProfile) -> Unit) {
    val authState by AuthManager.state.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthManager.UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AuthManager.UiState.SignedOut -> LoginScreen()

        is AuthManager.UiState.Authenticated -> Column(modifier = Modifier.fillMaxSize()) {
            if (state.isOffline) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "Mode offline — data tersimpan di perangkat; sinkronkan saat online.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) { onSignedIn(state.profile) }
        }
    }
}
