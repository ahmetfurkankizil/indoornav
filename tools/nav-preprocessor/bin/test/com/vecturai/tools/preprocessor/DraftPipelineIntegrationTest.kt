package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.glb.GlbGeometryExtractor
import com.Vectura AI.tools.preprocessor.glb.GlbParser
import com.Vectura AI.tools.preprocessor.model.AuthoringConfig
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.*

/**
 * Integration test: full draft pipeline from synthetic GLB to
 * authoring_config.generated.json + debug artifacts.
 */
class DraftPipelineIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a synthetic GLB with a simple L-shaped floor.
     * This produces a grid of floor vertices forming two connected zones.
     */
    private fun createSyntheticFloorGlb(): ByteArray {
        // Generate a grid of floor vertices at Y=0
        // Main corridor: x=[0..10], z=[0..2] (100 vertices)
        // Side room: x=[8..10], z=[2..6] (20 vertices)
        val vertices = mutableListOf<Triple<Float, Float, Float>>()

        // Corridor floor
        for (xi in 0..20) {
            for (zi in 0..4) {
                vertices.add(Triple(xi * 0.5f, 0f, zi * 0.5f))
            }
        }
        // Side room floor
        for (xi in 16..20) {
            for (zi in 5..12) {
                vertices.add(Triple(xi * 0.5f, 0f, zi * 0.5f))
            }
        }
        // Some ceiling vertices at Y=2.5
        for (xi in 0..10) {
            vertices.add(Triple(xi.toFloat(), 2.5f, 1f))
        }

        val vertexCount = vertices.size
        val binDataSize = vertexCount * 12  // 3 floats × 4 bytes

        val binBuf = ByteBuffer.allocate(binDataSize).order(ByteOrder.LITTLE_ENDIAN)
        for ((x, y, z) in vertices) {
            binBuf.putFloat(x)
            binBuf.putFloat(y)
            binBuf.putFloat(z)
        }
        val binData = binBuf.array()

        val jsonContent = """{
            "meshes":[{"name":"floor","primitives":[{"attributes":{"POSITION":0}}]}],
            "accessors":[{"bufferView":0,"componentType":5126,"count":$vertexCount,"type":"VEC3"}],
            "bufferViews":[{"buffer":0,"byteLength":$binDataSize}],
            "buffers":[{"byteLength":$binDataSize}]
        }"""

        val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
        val jsonPadded = jsonBytes.size.let { size ->
            val padded = (size + 3) and 3.inv()
            jsonBytes + ByteArray(padded - size) { 0x20 }
        }

        val totalLength = 12 + 8 + jsonPadded.size + 8 + binData.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        buf.putInt(0x46546C67) // magic
        buf.putInt(2) // version
        buf.putInt(totalLength)

        buf.putInt(jsonPadded.size)
        buf.putInt(0x4E4F534A)
        buf.put(jsonPadded)

        buf.putInt(binData.size)
        buf.putInt(0x004E4942)
        buf.put(binData)

        return buf.array()
    }

    @Test
    fun `full draft pipeline produces valid output`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "draft-integration-${System.nanoTime()}")
        tmpDir.mkdirs()

        val glbFile = File(tmpDir, "test.glb")
        glbFile.writeBytes(createSyntheticFloorGlb())

        val outputDir = File(tmpDir, "output")

        try {
            val pipeline = DraftPipeline()
            val exitCode = pipeline.execute(
                glbPath = glbFile.absolutePath,
                outputDir = outputDir.absolutePath,
            )

            assertEquals(0, exitCode, "Draft pipeline should succeed")

            // Verify authoring_config.generated.json
            val configFile = File(outputDir, "authoring_config.generated.json")
            assertTrue(configFile.exists(), "authoring_config.generated.json should exist")

            val config = json.decodeFromString<AuthoringConfig>(configFile.readText())
            assertTrue(config.nodes.isNotEmpty(), "Should have nodes")
            assertTrue(config.edges.isNotEmpty(), "Should have edges")
            assertTrue(config.entranceMarkers.isNotEmpty(), "Should have placeholder marker")
            assertTrue(config.tags.contains("draft"), "Should be tagged as draft")

            // Verify all node IDs match edge references
            val nodeIds = config.nodes.map { it.id }.toSet()
            for (edge in config.edges) {
                assertTrue(edge.from in nodeIds, "Edge ${edge.id} 'from' (${edge.from}) should reference valid node")
                assertTrue(edge.to in nodeIds, "Edge ${edge.id} 'to' (${edge.to}) should reference valid node")
            }

            // Verify debug artifacts
            assertTrue(File(outputDir, "generation_metadata.json").exists(), "generation_metadata.json should exist")
            assertTrue(File(outputDir, "occupancy_debug.svg").exists(), "occupancy_debug.svg should exist")
            assertTrue(File(outputDir, "draft_graph_debug.svg").exists(), "draft_graph_debug.svg should exist")
            assertTrue(File(outputDir, "geometry_stats.json").exists(), "geometry_stats.json should exist")

            // Verify SVG is valid XML
            val svg = File(outputDir, "occupancy_debug.svg").readText()
            assertTrue(svg.startsWith("<?xml"))
            assertTrue(svg.contains("<svg"))

        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `draft pipeline fails gracefully on header-only GLB`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "draft-fail-${System.nanoTime()}")
        tmpDir.mkdirs()

        // GLB with valid header but no geometry
        val glbFile = File(tmpDir, "empty.glb")
        val jsonContent = """{"meshes":[],"accessors":[],"bufferViews":[],"buffers":[]}"""
        val jsonBytes = jsonContent.toByteArray(Charsets.UTF_8)
        val jsonPadded = jsonBytes.size.let { size ->
            val padded = (size + 3) and 3.inv()
            jsonBytes + ByteArray(padded - size) { 0x20 }
        }
        val totalLength = 12 + 8 + jsonPadded.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46546C67); buf.putInt(2); buf.putInt(totalLength)
        buf.putInt(jsonPadded.size); buf.putInt(0x4E4F534A); buf.put(jsonPadded)
        glbFile.writeBytes(buf.array())

        try {
            val pipeline = DraftPipeline()
            val exitCode = pipeline.execute(
                glbPath = glbFile.absolutePath,
                outputDir = File(tmpDir, "output").absolutePath,
            )
            assertEquals(1, exitCode, "Should fail with validation error for empty geometry")
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
