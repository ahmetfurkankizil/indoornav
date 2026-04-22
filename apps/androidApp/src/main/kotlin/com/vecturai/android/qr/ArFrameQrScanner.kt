package com.vecturai.android.qr

import android.media.Image
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class ArFrameQrScanner(
    private val onCodeScanned: (String) -> Boolean,
) {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    private val processing = AtomicBoolean(false)
    private val locked = AtomicBoolean(false)
    private var lastScanAttemptMs = 0L

    fun scan(frame: Frame, rotationDegrees: Int) {
        val now = System.currentTimeMillis()
        if (locked.get() || now - lastScanAttemptMs < SCAN_INTERVAL_MS) return
        if (!processing.compareAndSet(false, true)) return
        lastScanAttemptMs = now

        val image = try {
            frame.acquireCameraImage()
        } catch (_: NotYetAvailableException) {
            processing.set(false)
            return
        } catch (_: Throwable) {
            processing.set(false)
            return
        }

        processImage(image, rotationDegrees)
    }

    fun reset() {
        locked.set(false)
        processing.set(false)
        lastScanAttemptMs = 0L
    }

    fun close() {
        scanner.close()
    }

    private fun processImage(image: Image, rotationDegrees: Int) {
        try {
            val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val rawValue = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                    if (rawValue != null && onCodeScanned(rawValue)) {
                        locked.set(true)
                    }
                }
                .addOnCompleteListener {
                    image.close()
                    processing.set(false)
                }
        } catch (_: Throwable) {
            image.close()
            processing.set(false)
        }
    }

    private companion object {
        private const val SCAN_INTERVAL_MS = 166L
    }
}
