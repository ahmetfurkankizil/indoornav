package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*
import java.io.File

/**
 * Integration test: full pipeline from authoring config to package output.
 */
class PipelineIntegrationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun `full pipeline with sample config produces valid package`() {
        val config = AuthoringConfig(
            buildingId = "integration-test",
            buildingName = "Integration Test Building",
            asset = AssetReference("scan.glb"),
            nodes = listOf(
                AuthoringNode("n1", 0.0, 0.0, 0.0, "entrance", "Entry"),
                AuthoringNode("n2", 3.0, 0.0, 0.0, "junction", "Hall"),
                AuthoringNode("n3", 6.0, 0.0, 0.0, "room_entry", "Office Entry"),
                AuthoringNode("n4", 3.0, 0.0, 3.0, "room_entry", "Kitchen Entry"),
            ),
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", 3.0),
                AuthoringEdge("e2", "n2", "n3", 3.0),
                AuthoringEdge("e3", "n2", "n4", 3.0),
            ),
            rooms = listOf(
                AuthoringRoom("r1", "Office", "n3", "office"),
                AuthoringRoom("r2", "Kitchen", "n4", "facility"),
            ),
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n1", 0.2, 0.2, Position3D(0.0, 1.0, 0.0)),
            ),
        )

        // Write config to temp file
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "pipeline-test-${System.nanoTime()}")
        tmpDir.mkdirs()
        val configFile = File(tmpDir, "authoring_config.json")
        configFile.writeText(json.encodeToString(config))

        // Create minimal .glb stub (valid GLB magic)
        val glbFile = File(tmpDir, "scan.glb")
        glbFile.writeBytes(
            byteArrayOf(
                0x67, 0x6C, 0x54, 0x46,  // magic: "glTF"
                0x02, 0x00, 0x00, 0x00,  // version: 2
                0x0C, 0x00, 0x00, 0x00,  // length: 12
            )
        )

        val outputDir = File(tmpDir, "output")

        try {
            val pipeline = Pipeline()
            val exitCode = pipeline.execute(
                glbPath = glbFile.absolutePath,
                configPath = configFile.absolutePath,
                outputDir = outputDir.absolutePath,
                overwrite = true,
            )

            assertEquals(0, exitCode, "Pipeline should succeed")
            assertTrue(File(outputDir, "manifest.json").exists())
            assertTrue(File(outputDir, "nav_graph.json").exists())
            assertTrue(File(outputDir, "rooms.json").exists())
            assertTrue(File(outputDir, "entrance_markers.json").exists())
            assertTrue(File(outputDir, "route_rendering.json").exists())
            assertTrue(File(outputDir, "preview.glb").exists())
            assertTrue(File(outputDir, "graph_debug.json").exists())
            assertTrue(File(outputDir, "plan_view_debug.svg").exists())

            // Verify nav_graph content
            val navGraphContent = File(outputDir, "nav_graph.json").readText()
            val navGraph = json.decodeFromString<PackageNavGraph>(navGraphContent)
            assertEquals(4, navGraph.nodes.size)
            assertEquals(3, navGraph.edges.size)

            // Verify SVG is valid XML-ish
            val svg = File(outputDir, "plan_view_debug.svg").readText()
            assertTrue(svg.startsWith("<?xml"))
            assertTrue(svg.contains("<svg"))
            assertTrue(svg.contains("Office Entry"))

        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `pipeline fails with missing glb file`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "pipeline-fail-${System.nanoTime()}")
        tmpDir.mkdirs()

        val configFile = File(tmpDir, "config.json")
        configFile.writeText(json.encodeToString(AuthoringConfig(
            buildingId = "fail-test",
            buildingName = "Fail",
            asset = AssetReference("scan.glb"),
            nodes = listOf(AuthoringNode("n1", 0.0, 0.0), AuthoringNode("n2", 1.0, 0.0)),
            edges = listOf(AuthoringEdge("e1", "n1", "n2", 1.0)),
            rooms = listOf(AuthoringRoom("r1", "R", "n2")),
            entranceMarkers = listOf(AuthoringMarker("m1", "n1", 0.2, 0.2, Position3D(0.0, 0.0, 0.0))),
        )))

        try {
            val pipeline = Pipeline()
            val exitCode = pipeline.execute(
                glbPath = "/nonexistent/scan.glb",
                configPath = configFile.absolutePath,
                outputDir = File(tmpDir, "out").absolutePath,
            )
            assertEquals(1, exitCode, "Pipeline should fail with validation error")
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
