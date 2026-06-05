package com.wahyuakbarwibowo.aminmartkasir.ui.scanner

import com.google.zxing.DecodeHintType
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.camera.CameraSettings

class BarcodeCaptureActivity : CaptureActivity() {

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(com.wahyuakbarwibowo.aminmartkasir.R.layout.activity_barcode_capture)
        val dbv = findViewById<DecoratedBarcodeView>(com.wahyuakbarwibowo.aminmartkasir.R.id.zxing_barcode_scanner)

        dbv.barcodeView.apply {
            // TRY_HARDER: retry decode aggressively for small/damaged barcodes
            decoderFactory = DefaultDecoderFactory(
                null,
                mapOf(DecodeHintType.TRY_HARDER to true),
                null,
                0
            )
            // Continuous autofocus keeps small barcodes sharp
            cameraSettings.focusMode = CameraSettings.FocusMode.CONTINUOUS
        }

        return dbv
    }
}
