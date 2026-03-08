package com.vecturai.tools.preprocessor

import java.io.File

/**
 * Inspects the .glb asset file for presence and basic metadata.
 *
 * Does NOT parse the full GLB binary format — that would require a
 * dedicated glTF library. Instead, extracts what's safely available:
 * file size, magic bytes validation, and basic GLB header info.
 */
class AssetInspector {

    data class AssetInfo(
        val filePath: String,
        val fileSizeBytes: Long,
        val isValidGlb: Boolean,
        val glbVersion: Int?,
        val totalLengthBytes: Long?,
        val warnings: List<String>,
    )

    /**
     * Inspect a .glb file and return metadata.
     *
     * @throws ValidationException if the file is missing or unreadable
     */
    fun inspect(glbPath: String): AssetInfo {
        val file = File(glbPath)
        if (!file.exists()) {
            throw ValidationException("Asset file not found: $glbPath")
        }
        if (!file.canRead()) {
            throw ValidationException("Asset file not readable: $glbPath")
        }
        if (!file.name.endsWith(".glb", ignoreCase = true)) {
            throw ValidationException("Expected .glb file, got: ${file.name}")
        }

        val warnings = mutableListOf<String>()
        var isValidGlb = false
        var glbVersion: Int? = null
        var totalLength: Long? = null

        // GLB binary format: 12-byte header
        // Bytes 0-3: magic 0x46546C67 ("glTF")
        // Bytes 4-7: version (uint32, little-endian)
        // Bytes 8-11: total length (uint32, little-endian)
        if (file.length() >= 12) {
            try {
                file.inputStream().use { stream ->
                    val header = ByteArray(12)
                    stream.read(header)

                    val magic = (header[0].toInt() and 0xFF) or
                        ((header[1].toInt() and 0xFF) shl 8) or
                        ((header[2].toInt() and 0xFF) shl 16) or
                        ((header[3].toInt() and 0xFF) shl 24)

                    if (magic == 0x46546C67) { // "glTF" in little-endian
                        isValidGlb = true
                        glbVersion = (header[4].toInt() and 0xFF) or
                            ((header[5].toInt() and 0xFF) shl 8) or
                            ((header[6].toInt() and 0xFF) shl 16) or
                            ((header[7].toInt() and 0xFF) shl 24)
                        totalLength = ((header[8].toInt() and 0xFF).toLong()) or
                            ((header[9].toInt() and 0xFF).toLong() shl 8) or
                            ((header[10].toInt() and 0xFF).toLong() shl 16) or
                            ((header[11].toInt() and 0xFF).toLong() shl 24)

                        if (glbVersion != 2) {
                            warnings.add("GLB version is $glbVersion (expected 2)")
                        }
                    } else {
                        warnings.add("File does not have valid GLB magic bytes")
                    }
                }
            } catch (e: Exception) {
                warnings.add("Could not read GLB header: ${e.message}")
            }
        } else {
            warnings.add("File too small for valid GLB (${file.length()} bytes)")
        }

        if (file.length() > 500_000_000) {
            warnings.add("Asset file is very large (${file.length() / 1_000_000} MB)")
        }

        return AssetInfo(
            filePath = file.absolutePath,
            fileSizeBytes = file.length(),
            isValidGlb = isValidGlb,
            glbVersion = glbVersion,
            totalLengthBytes = totalLength,
            warnings = warnings,
        )
    }
}
