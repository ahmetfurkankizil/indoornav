package com.vecturai.tools.admin.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import javax.imageio.ImageIO

class QRCodeService(private val uploadsDir: String) {

    fun buildPayload(qrToken: String): String {
        val payload = buildJsonObject {
            put("type", "VecturAI-building")
            put("token", qrToken)
            put("v", 2)
        }
        return Json.encodeToString(payload)
    }

    fun generate(qrToken: String): ByteArray {
        val content = buildPayload(qrToken)
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        val image = MatrixToImageWriter.toBufferedImage(matrix)
        return ByteArrayOutputStream().also { ImageIO.write(image, "PNG", it) }.toByteArray()
    }

    fun saveQrImage(buildingId: String, qrToken: String): String {
        val dir = Path.of(uploadsDir, "qr").toFile().also { it.mkdirs() }
        val file = dir.resolve("$buildingId.png")
        file.writeBytes(generate(qrToken))
        return file.absolutePath
    }

    fun getQrImageBytes(buildingId: String): ByteArray? {
        val file = Path.of(uploadsDir, "qr", "$buildingId.png").toFile()
        return if (file.exists()) file.readBytes() else null
    }
}
