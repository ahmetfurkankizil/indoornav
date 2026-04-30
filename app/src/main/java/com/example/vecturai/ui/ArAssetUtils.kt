package com.example.vecturai.ui

import android.content.Context

fun hasValidGlbAsset(context: Context, assetPath: String): Boolean = runCatching {
    context.assets.open(assetPath).use { input ->
        val header = ByteArray(4)
        input.read(header) == 4 &&
            header[0] == 0x67.toByte() &&
            header[1] == 0x6C.toByte() &&
            header[2] == 0x54.toByte() &&
            header[3] == 0x46.toByte()
    }
}.getOrDefault(false)
