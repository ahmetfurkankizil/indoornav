package com.vecturai.tools.preprocessor.importmap

import com.vecturai.tools.preprocessor.ValidationException
import com.vecturai.tools.preprocessor.glb.Vec3
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses PLY (Polygon File Format / Stanford Triangle Format) files.
 *
 * Supports:
 * - ASCII format
 * - Binary little-endian format
 * - Binary big-endian format
 *
 * Extracts only vertex x/y/z positions; all other properties (normals,
 * colors, faces, etc.) are skipped.
 */
class PlyParser {

    enum class PlyFormat { ASCII, BINARY_LE, BINARY_BE }

    data class PlyHeader(
        val format: PlyFormat,
        val vertexCount: Int,
        val xIndex: Int,
        val yIndex: Int,
        val zIndex: Int,
        val propertyCount: Int,
        /** Property sizes in bytes (for binary parsing). */
        val propertySizes: List<Int>,
        /** Byte offset where data starts (after "end_header\n"). */
        val headerByteLength: Int,
    )

    /**
     * Parse a PLY file and return vertex positions.
     *
     * @throws ValidationException on invalid or unreadable files
     */
    fun parse(path: String): List<Vec3> {
        val file = File(path)
        if (!file.exists()) throw ValidationException("PLY file not found: $path")
        if (!file.canRead()) throw ValidationException("PLY file not readable: $path")

        val header = parseHeader(file)
        if (header.vertexCount == 0) return emptyList()

        return when (header.format) {
            PlyFormat.ASCII -> parseAscii(file, header)
            PlyFormat.BINARY_LE -> parseBinary(file, header, ByteOrder.LITTLE_ENDIAN)
            PlyFormat.BINARY_BE -> parseBinary(file, header, ByteOrder.BIG_ENDIAN)
        }
    }

    internal fun parseHeader(file: File): PlyHeader {
        val lines = mutableListOf<String>()
        var headerByteLength = 0

        // Read header lines byte-by-byte to get exact byte offset
        file.inputStream().bufferedReader(Charsets.US_ASCII).use { reader ->
            while (true) {
                val line = reader.readLine()
                    ?: throw ValidationException("PLY header incomplete: unexpected end of file")
                headerByteLength += line.toByteArray(Charsets.US_ASCII).size + 1 // +1 for \n
                // Handle \r\n
                lines.add(line.trimEnd('\r'))
                if (line.trimEnd('\r') == "end_header") break
            }
        }

        if (lines.isEmpty() || lines[0] != "ply") {
            throw ValidationException("Not a valid PLY file (missing 'ply' magic)")
        }

        var format: PlyFormat? = null
        var vertexCount = 0
        var inVertexElement = false
        val vertexProperties = mutableListOf<Pair<String, String>>() // name -> type
        var xIndex = -1; var yIndex = -1; var zIndex = -1

        for (line in lines) {
            val parts = line.trim().split("\\s+".toRegex())
            when {
                parts[0] == "format" -> {
                    format = when (parts.getOrNull(1)) {
                        "ascii" -> PlyFormat.ASCII
                        "binary_little_endian" -> PlyFormat.BINARY_LE
                        "binary_big_endian" -> PlyFormat.BINARY_BE
                        else -> throw ValidationException("Unsupported PLY format: ${parts.getOrNull(1)}")
                    }
                }
                parts[0] == "element" && parts.getOrNull(1) == "vertex" -> {
                    inVertexElement = true
                    vertexCount = parts.getOrNull(2)?.toIntOrNull()
                        ?: throw ValidationException("Invalid vertex count in PLY header")
                }
                parts[0] == "element" && parts.getOrNull(1) != "vertex" -> {
                    inVertexElement = false
                }
                parts[0] == "property" && inVertexElement -> {
                    val propType = parts.getOrNull(1) ?: continue
                    val propName = parts.getOrNull(2) ?: continue
                    vertexProperties.add(propName to propType)
                }
            }
        }

        if (format == null) throw ValidationException("PLY header missing 'format' declaration")

        // Find x, y, z indices
        vertexProperties.forEachIndexed { index, (name, _) ->
            when (name.lowercase()) {
                "x" -> xIndex = index
                "y" -> yIndex = index
                "z" -> zIndex = index
            }
        }

        if (xIndex < 0 || yIndex < 0 || zIndex < 0) {
            throw ValidationException("PLY vertex element missing x/y/z properties")
        }

        val propertySizes = vertexProperties.map { (_, type) -> plyTypeSize(type) }

        return PlyHeader(
            format = format,
            vertexCount = vertexCount,
            xIndex = xIndex,
            yIndex = yIndex,
            zIndex = zIndex,
            propertyCount = vertexProperties.size,
            propertySizes = propertySizes,
            headerByteLength = headerByteLength,
        )
    }

    private fun parseAscii(file: File, header: PlyHeader): List<Vec3> {
        val vertices = mutableListOf<Vec3>()
        val allLines = file.readLines()

        // Find end_header line index
        val headerEndIndex = allLines.indexOfFirst { it.trim() == "end_header" }
        if (headerEndIndex < 0) throw ValidationException("PLY missing end_header")

        val dataLines = allLines.subList(headerEndIndex + 1, allLines.size)
        val count = minOf(header.vertexCount, dataLines.size)

        for (i in 0 until count) {
            val parts = dataLines[i].trim().split("\\s+".toRegex())
            if (parts.size < header.propertyCount) continue

            val x = parts[header.xIndex].toFloatOrNull() ?: continue
            val y = parts[header.yIndex].toFloatOrNull() ?: continue
            val z = parts[header.zIndex].toFloatOrNull() ?: continue
            vertices.add(Vec3(x, y, z))
        }

        return vertices
    }

    private fun parseBinary(file: File, header: PlyHeader, order: ByteOrder): List<Vec3> {
        val vertices = mutableListOf<Vec3>()
        val vertexStride = header.propertySizes.sum()

        // Compute byte offsets of x, y, z within each vertex record
        val offsets = IntArray(header.propertyCount)
        var accum = 0
        for (i in header.propertySizes.indices) {
            offsets[i] = accum
            accum += header.propertySizes[i]
        }

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(header.headerByteLength.toLong())

            val vertexBlock = ByteArray(header.vertexCount * vertexStride)
            val bytesRead = raf.read(vertexBlock)
            if (bytesRead < vertexBlock.size) {
                throw ValidationException(
                    "PLY binary data truncated: expected ${vertexBlock.size} bytes, got $bytesRead"
                )
            }

            val buf = ByteBuffer.wrap(vertexBlock).order(order)

            for (i in 0 until header.vertexCount) {
                val base = i * vertexStride
                val x = buf.getFloat(base + offsets[header.xIndex])
                val y = buf.getFloat(base + offsets[header.yIndex])
                val z = buf.getFloat(base + offsets[header.zIndex])
                vertices.add(Vec3(x, y, z))
            }
        }

        return vertices
    }

    private fun plyTypeSize(type: String): Int = when (type.lowercase()) {
        "char", "int8", "uchar", "uint8" -> 1
        "short", "int16", "ushort", "uint16" -> 2
        "int", "int32", "uint", "uint32", "float", "float32" -> 4
        "double", "float64" -> 8
        else -> 4 // default to float
    }
}
