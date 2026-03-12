package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.glb.GlbParser
import com.vecturai.tools.preprocessor.glb.GlbGeometryExtractor
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

/**
 * Tests for [GlbParser] — GLB binary format parsing.
 */
class GlbParserTest {

    private val parser = GlbParser()

    private fun createMinimalGlb(): ByteArray {
        // Minimal valid GLB: header + JSON chunk with empty meshes
        val jsonContent = """{"meshes":[],"accessors":[],"bufferViews":[],"buffers":[]}"""
        val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
        // Pad JSON to 4-byte alignment
        val jsonPadded = jsonBytes.size.let { size ->
            val padded = (size + 3) and 3.inv()
            jsonBytes + ByteArray(padded - size) { 0x20 }
        }

        val totalLength = 12 + 8 + jsonPadded.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        // Header
        buf.putInt(0x46546C67) // magic "glTF"
        buf.putInt(2) // version
        buf.putInt(totalLength)

        // JSON chunk
        buf.putInt(jsonPadded.size)
        buf.putInt(0x4E4F534A) // "JSON"
        buf.put(jsonPadded)

        return buf.array()
    }

    private fun createGlbWithGeometry(): ByteArray {
        // 3 vertices forming a triangle at Y=0 (floor)
        val vertexData = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN)
        vertexData.putFloat(0f); vertexData.putFloat(0f); vertexData.putFloat(0f)
        vertexData.putFloat(1f); vertexData.putFloat(0f); vertexData.putFloat(0f)
        vertexData.putFloat(0f); vertexData.putFloat(0f); vertexData.putFloat(1f)
        val binData = vertexData.array()

        val jsonContent = """{
            "meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}],
            "accessors":[{"bufferView":0,"componentType":5126,"count":3,"type":"VEC3"}],
            "bufferViews":[{"buffer":0,"byteLength":36}],
            "buffers":[{"byteLength":36}]
        }""".trimIndent()
        val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
        val jsonPadded = jsonBytes.size.let { size ->
            val padded = (size + 3) and 3.inv()
            jsonBytes + ByteArray(padded - size) { 0x20 }
        }

        val totalLength = 12 + 8 + jsonPadded.size + 8 + binData.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        // Header
        buf.putInt(0x46546C67)
        buf.putInt(2)
        buf.putInt(totalLength)

        // JSON chunk
        buf.putInt(jsonPadded.size)
        buf.putInt(0x4E4F534A)
        buf.put(jsonPadded)

        // BIN chunk
        buf.putInt(binData.size)
        buf.putInt(0x004E4942)
        buf.put(binData)

        return buf.array()
    }

    @Test
    fun `parse minimal valid GLB`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            tmpFile.writeBytes(createMinimalGlb())
            val result = parser.parse(tmpFile.absolutePath)
            assertTrue(result.json.meshes.isEmpty())
            assertTrue(result.binChunk.isEmpty())
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun `parse GLB with geometry`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            tmpFile.writeBytes(createGlbWithGeometry())
            val result = parser.parse(tmpFile.absolutePath)
            assertEquals(1, result.json.meshes.size)
            assertEquals(1, result.json.accessors.size)
            assertEquals(36, result.binChunk.size)
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun `parse GLB with geometry extracts vertices`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            tmpFile.writeBytes(createGlbWithGeometry())
            val glbData = parser.parse(tmpFile.absolutePath)
            val extractor = GlbGeometryExtractor()
            val result = extractor.extract(glbData)
            assertEquals(3, result.vertices.size)
            assertEquals(0f, result.vertices[0].x)
            assertEquals(0f, result.vertices[0].y)
            assertEquals(0f, result.vertices[0].z)
            assertEquals(1f, result.vertices[1].x)
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun `reject missing file`() {
        assertFailsWith<ValidationException> {
            parser.parse("/nonexistent/file.glb")
        }
    }

    @Test
    fun `reject too small file`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            tmpFile.writeBytes(byteArrayOf(1, 2, 3))
            assertFailsWith<ValidationException> {
                parser.parse(tmpFile.absolutePath)
            }
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun `reject invalid magic bytes`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            val bad = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            bad.putInt(0x12345678) // wrong magic
            bad.putInt(2)
            bad.putInt(12)
            tmpFile.writeBytes(bad.array())
            assertFailsWith<ValidationException> {
                parser.parse(tmpFile.absolutePath)
            }
        } finally {
            tmpFile.delete()
        }
    }

    @Test
    fun `reject unsupported GLB version`() {
        val tmpFile = File.createTempFile("test", ".glb")
        try {
            val bad = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            bad.putInt(0x46546C67) // valid magic
            bad.putInt(1) // version 1, not supported
            bad.putInt(12)
            tmpFile.writeBytes(bad.array())
            assertFailsWith<ValidationException> {
                parser.parse(tmpFile.absolutePath)
            }
        } finally {
            tmpFile.delete()
        }
    }
}
