package com.example.vecturai.ui

import android.content.Context

fun hasValidGlbAsset(context: Context, assetPath: String): Boolean = runCatching {
    context.assets.open(assetPath).use { input ->
        val header = ByteArray(GLB_HEADER_LENGTH_BYTES)
        if (input.read(header) != GLB_HEADER_LENGTH_BYTES) return@runCatching false

        val magicOk = header[0] == 0x67.toByte() &&
            header[1] == 0x6C.toByte() &&
            header[2] == 0x54.toByte() &&
            header[3] == 0x46.toByte()
        val version = readLittleEndianUInt(header, 4)
        val declaredLength = readLittleEndianUInt(header, 8)
        val assetSize = runCatching {
            context.assets.openFd(assetPath).use { it.length }
        }.getOrNull()

        magicOk &&
            version == GLB_VERSION_2 &&
            declaredLength >= GLB_HEADER_LENGTH_BYTES.toLong() &&
            (assetSize == null || declaredLength <= assetSize)
    }
}.getOrDefault(false)

private const val GLB_HEADER_LENGTH_BYTES = 12
private const val GLB_VERSION_2 = 2L

private fun readLittleEndianUInt(bytes: ByteArray, offset: Int): Long =
    (bytes[offset].toLong() and 0xffL) or
        ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
        ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
        ((bytes[offset + 3].toLong() and 0xffL) shl 24)
