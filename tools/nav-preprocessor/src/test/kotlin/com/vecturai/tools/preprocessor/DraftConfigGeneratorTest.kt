package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.analysis.FloorPlaneEstimator
import com.Vectura AI.tools.preprocessor.analysis.ZoneSuggester
import com.Vectura AI.tools.preprocessor.draft.DraftConfigGenerator
import com.Vectura AI.tools.preprocessor.draft.NavigationGraphDrafter
import com.Vectura AI.tools.preprocessor.glb.BoundingBox3D
import com.Vectura AI.tools.preprocessor.glb.Vec3
import com.Vectura AI.tools.preprocessor.model.AuthoringConfig
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.*

/**
 * Tests for [DraftConfigGenerator] — draft authoring config generation.
 */
class DraftConfigGeneratorTest {

    private val generator = DraftConfigGenerator()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `generates valid authoring config JSON`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "draft-test-${System.nanoTime()}")
        try {
            val draftGraph = NavigationGraphDrafter.DraftNavGraph(
                nodes = listOf(
                    NavigationGraphDrafter.DraftNode("n01", 0.0, 0.0, 0.0, "junction", "Zone A", "zone-a"),
                    NavigationGraphDrafter.DraftNode("n02", 5.0, 0.0, 3.0, "room_entry", "Zone B", "zone-b"),
                ),
                edges = listOf(
                    NavigationGraphDrafter.DraftEdge("e01", "n01", "n02", 5.83),
                ),
            )

            val zones = listOf(
                ZoneSuggester.Zone("zone-a", "Zone A", 50, 0.0, 0.0, listOf()),
                ZoneSuggester.Zone("zone-b", "Zone B", 20, 5.0, 3.0, listOf()),
            )

            val floorEstimate = FloorPlaneEstimator.FloorEstimate(
                floorY = 0.0, confidence = 0.75, floorVertexCount = 1000,
                totalVertexCount = 1500, floorVertices = emptyList(),
            )

            val configPath = generator.generate(
                glbFileName = "scan.glb",
                draftGraph = draftGraph,
                zones = zones,
                floorEstimate = floorEstimate,
                boundingBox = BoundingBox3D(-5f, 0f, -5f, 10f, 3f, 10f),
                outputDir = tmpDir.absolutePath,
            )

            // Verify file exists
            val configFile = File(configPath)
            assertTrue(configFile.exists())

            // Verify it parses as valid AuthoringConfig
            val content = configFile.readText()
            val config = json.decodeFromString<AuthoringConfig>(content)

            assertEquals(2, config.nodes.size)
            assertEquals(1, config.edges.size)
            assertEquals(1, config.rooms.size) // Zone B → room (Zone A is largest → corridor)
            assertTrue(config.entranceMarkers.isNotEmpty())
            assertTrue(config.tags.contains("draft"))
            assertTrue(config.tags.contains("auto-generated"))

            // Verify metadata file exists
            assertTrue(File(tmpDir, "generation_metadata.json").exists())

        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `generated rooms use neutral labels`() {
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "draft-test2-${System.nanoTime()}")
        try {
            val draftGraph = NavigationGraphDrafter.DraftNavGraph(
                nodes = listOf(
                    NavigationGraphDrafter.DraftNode("n01", 0.0, 0.0, 0.0, "junction", "Zone A", "zone-a"),
                    NavigationGraphDrafter.DraftNode("n02", 3.0, 0.0, 0.0, "room_entry", "Zone B", "zone-b"),
                    NavigationGraphDrafter.DraftNode("n03", 6.0, 0.0, 0.0, "room_entry", "Zone C", "zone-c"),
                ),
                edges = listOf(
                    NavigationGraphDrafter.DraftEdge("e01", "n01", "n02", 3.0),
                    NavigationGraphDrafter.DraftEdge("e02", "n01", "n03", 6.0),
                ),
            )

            val zones = listOf(
                ZoneSuggester.Zone("zone-a", "Zone A", 50, 0.0, 0.0, listOf()),
                ZoneSuggester.Zone("zone-b", "Zone B", 20, 3.0, 0.0, listOf()),
                ZoneSuggester.Zone("zone-c", "Zone C", 15, 6.0, 0.0, listOf()),
            )

            val floorEstimate = FloorPlaneEstimator.FloorEstimate(
                floorY = 0.0, confidence = 0.6, floorVertexCount = 500,
                totalVertexCount = 800, floorVertices = emptyList(),
            )

            val configPath = generator.generate(
                glbFileName = "scan.glb",
                draftGraph = draftGraph,
                zones = zones,
                floorEstimate = floorEstimate,
                boundingBox = null,
                outputDir = tmpDir.absolutePath,
            )

            val config = json.decodeFromString<AuthoringConfig>(File(configPath).readText())

            // Rooms should have neutral labels
            assertEquals(2, config.rooms.size)
            assertTrue(config.rooms.all { it.displayName.startsWith("Zone") })
            assertTrue(config.rooms.all { it.category == "unknown" })

        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
