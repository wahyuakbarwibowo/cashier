package com.wahyuakbarwibowo.aminmartkasir.ui.scanner

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Minta izin kamera lalu buka scanner barcode. Kembalikan fungsi untuk memulai scan;
 * [onScanned] hanya dipanggil untuk hasil yang tidak kosong.
 */
@Composable
fun rememberBarcodeScanner(
    prompt: String = "Scan barcode produk",
    onPermissionDenied: (() -> Unit)? = null,
    onScanned: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnDenied by rememberUpdatedState(onPermissionDenied)
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.takeIf { it.isNotBlank() }?.let { currentOnScanned(it) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scanLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                    setPrompt(prompt)
                    setBeepEnabled(true)
                    setOrientationLocked(true)
                    setCaptureActivity(BarcodeCaptureActivity::class.java)
                    addExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true)
                }
            )
        } else {
            currentOnDenied?.invoke()
                ?: Toast.makeText(context, "Izin kamera diperlukan untuk scan barcode", Toast.LENGTH_SHORT).show()
        }
    }
    return { permissionLauncher.launch(Manifest.permission.CAMERA) }
}
