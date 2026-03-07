package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlinx.serialization.json.Json
import kotlin.test.*
import java.io.File

/**
 * Tests for [PackageExporter] — package generation and file output.
 */
class PackageExporterTest {

    private val exporter = PackageExporter()
    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleConfig() = AuthoringConfig(
        buildingId = "test-export",
        buildingName = "Export Test Building",
        asset = AssetReference("scan.glb"),
        nodes = listOf(
            AuthoringNode("n1", 0.0, 0.0, 0.0, "entrance"),
            AuthoringNode("n2", 3.0, 0.0, 4.0, "room_entry"),
        ),
        edges = listOf(AuthoringEdge("e1", "n1", "n2", 5.0)),
        rooms = listOf(AuthoringRoom("r1", "Office A", "n2", "office", listOf("desk"))),
        entranceMarkers = listOf(
            AuthoringMarker("m1", "n1", 0.21, 0.21, Position3D(0.0, 1.2, 0.0)),
        ),
    )

    @Test
    fun `export creates all expected files`() {
        val tmpDir = createTempDir("test-export")
        try {
            val result = exporter.export(sampleConfig(), "/dev/null", tmpDir.absolutePath, overwrite = true)
            val files = result.files.toSet()

            assertTrue("nav_graph.json" in files)
            assertTrue("rooms.json" in files)
            assertTrue("entrance_markers.json" in files)
            assertTrue("route_rendering.json" in files)
            assertTrue("manifest.json" in files)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `exported nav_graph has correct nodes and edges`() {
        val tmpDir = createTempDir("test-ng")
        try {
            exporter.export(sampleConfig(), "/dev/null", tmpDir.absolutePath, overwrite = true)
            val content = File(tmpDir, "nav_graph.json").readText()
            val graph = json.decodeFromString<PackageNavGraph>(content)
            assertEquals(2, graph.nodes.size)
            assertEquals(1, graph.edges.size)
            assertEquals("test-export", graph.buildingId)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `exported rooms have keywords`() {
        val tmpDir = createTempDir("test-rooms")
        try {
            exporter.export(sampleConfig(), "/dev/null", tmpDir.absolutePath, overwrite = true)
            val content = File(tmpDir, "rooms.json").readText()
            val rooms = json.decodeFromString<PackageRooms>(content)
            assertEquals("Office A", rooms.rooms.first().name)
            assertEquals(listOf("desk"), rooms.rooms.first().keywords)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `exported markers have generated QR payload`() {
        val tmpDir = createTempDir("test-markers")
        try {
            exporter.export(sampleConfig(), "/dev/null", tmpDir.absolutePath, overwrite = true)
            val content = File(tmpDir, "entrance_markers.json").readText()
            val markers = json.decodeFromString<PackageMarkers>(content)
            assertTrue(markers.markers.first().qrPayload.contains("test-export"))
            assertTrue(markers.markers.first().qrPayload.contains("m1"))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `export to existing dir without overwrite fails`() {
        val tmpDir = createTempDir("test-no-overwrite")
        try {
            assertFailsWith<ValidationException> {
                exporter.export(sampleConfig(), "/dev/null", tmpDir.absolutePath, overwrite = false)
            }
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    private fun createTempDir(prefix: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "$prefix-${System.nanoTime()}")
        dir.mkdirs()
        return dir
    }
}
