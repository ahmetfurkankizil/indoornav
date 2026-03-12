package com.vecturai.tools.preprocessor.glb

import com.vecturai.tools.preprocessor.ValidationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses a GLB 2.0 binary file into its JSON and BIN chunks.
 *
 * GLB binary layout (little-endian):
 * ```
 * ┌────────────────── 12-byte header ──────────────────┐
 * │ magic (4 bytes) │ version (4 bytes) │ length (4 bytes) │
 * ├────────────────── chunk 0 (JSON) ──────────────────┤
 * │ chunkLength (4) │ chunkType (4) │ chunkData (...)  │
 * ├────────────────── chunk 1 (BIN) ───────────────────┤
 * │ chunkLength (4) │ chunkType (4) │ chunkData (...)  │
 * └────────────────────────────────────────────────────┘
 * ```
 */
class GlbParser {

    companion object {
        const val GLB_MAGIC = 0x46546C67       // "glTF" in little-endian
        const val GLB_VERSION = 2
        const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
        const val CHUNK_TYPE_BIN = 0x004E4942  // "BIN\0"
        const val GLB_HEADER_SIZE = 12
        const val CHUNK_HEADER_SIZE = 8
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse a .glb file and return structured data.
     *
     * @throws ValidationException on invalid or unreadable files
     */
    fun parse(glbPath: String): GlbData {
        val file = File(glbPath)
        if (!file.exists()) throw ValidationException("GLB file not found: $glbPath")
        if (!file.canRead()) throw ValidationException("GLB file not readable: $glbPath")
        if (file.length() < GLB_HEADER_SIZE) {
            throw ValidationException("GLB file too small (${file.length()} bytes)")
        }

        RandomAccessFile(file, "r").use { raf ->
            // ── Read 12-byte header ──
            val headerBytes = ByteArray(GLB_HEADER_SIZE)
            raf.readFully(headerBytes)
            val header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

            val magic = header.getInt()
            if (magic != GLB_MAGIC) {
                throw ValidationException("Invalid GLB magic bytes (expected glTF header)")
            }

            val version = header.getInt()
            if (version != GLB_VERSION) {
                throw ValidationException("Unsupported GLB version $version (only v2 supported)")
            }

            val totalLength = header.getInt().toLong() and 0xFFFFFFFFL

            // ── Read chunks ──
            var jsonChunkData: ByteArray? = null
            var binChunkData: ByteArray? = null
            var offset = GLB_HEADER_SIZE.toLong()

            while (offset + CHUNK_HEADER_SIZE <= totalLength && offset + CHUNK_HEADER_SIZE <= file.length()) {
                raf.seek(offset)
                val chunkHeaderBytes = ByteArray(CHUNK_HEADER_SIZE)
                raf.readFully(chunkHeaderBytes)
                val chunkHeader = ByteBuffer.wrap(chunkHeaderBytes).order(ByteOrder.LITTLE_ENDIAN)

                val chunkLength = chunkHeader.getInt().toLong() and 0xFFFFFFFFL
                val chunkType = chunkHeader.getInt()

                if (chunkLength > file.length()) {
                    throw ValidationException("Chunk length ($chunkLength) exceeds file size")
                }

                val chunkData = ByteArray(chunkLength.toInt())
                raf.readFully(chunkData)

                when (chunkType) {
                    CHUNK_TYPE_JSON -> jsonChunkData = chunkData
                    CHUNK_TYPE_BIN -> binChunkData = chunkData
                }

                offset += CHUNK_HEADER_SIZE + chunkLength
            }

            if (jsonChunkData == null) {
                throw ValidationException("GLB file missing JSON chunk")
            }

            // Parse the JSON chunk into our model
            val jsonString = jsonChunkData.toString(Charsets.UTF_8).trimEnd('\u0000', ' ')
            val gltfJson = try {
                json.decodeFromString<GltfJson>(jsonString)
            } catch (e: Exception) {
                throw ValidationException("Failed to parse glTF JSON chunk: ${e.message}")
            }

            return GlbData(
                json = gltfJson,
                binChunk = binChunkData ?: ByteArray(0),
            )
        }
    }
}
