package com.wahyuakbarwibowo.aminmartkasir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.TenantDatabaseFiles
import com.wahyuakbarwibowo.aminmartkasir.ui.LoginGate
import com.wahyuakbarwibowo.aminmartkasir.ui.MainAppContainer
import com.wahyuakbarwibowo.aminmartkasir.ui.TenantDatabaseGate
import com.wahyuakbarwibowo.aminmartkasir.ui.theme.AminmartKasirTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AminmartKasirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginGate(
                        onSignedIn = { profile ->
                            val storeId = profile.storeId ?: TenantDatabaseFiles.platformStore(profile.id)
                            key(storeId) {
                                val tenantOwner = remember {
                                    object : ViewModelStoreOwner {
                                        override val viewModelStore = ViewModelStore()
                                    }
                                }
                                DisposableEffect(tenantOwner) {
                                    onDispose { tenantOwner.viewModelStore.clear() }
                                }
                                CompositionLocalProvider(LocalViewModelStoreOwner provides tenantOwner) {
                                    TenantDatabaseGate(storeId) {
                                        val viewModelFactory = remember(storeId) {
                                            AppDatabase.getViewModelFactory(applicationContext, storeId)
                                        }
                                        MainAppContainer(viewModelFactory = viewModelFactory)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
