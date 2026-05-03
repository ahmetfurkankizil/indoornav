package com.Vectura AI.tools.admin

import com.Vectura AI.tools.admin.model.JobStatus
import com.Vectura AI.tools.admin.service.DraftSummaryExtractor
import java.io.File
import kotlin.test.*

class DraftSummaryExtractorTest {

    private lateinit var outputDir: File
    private val extractor = DraftSummaryExtractor()

    @BeforeTest
    fun setup() {
        outputDir = File("build/test-extractor-${System.currentTimeMillis()}")
        outputDir.mkdirs()
    }

    @AfterTest
    fun cleanup() {
        outputDir.deleteRecursively()
    }

    private fun writeConfig(content: String) =
        File(outputDir, "authoring_config.generated.json").writeText(content)

    private fun writeMetadata(content: String) =
        File(outputDir, "generation_metadata.json").writeText(content)

    private fun writeStats(content: String) =
        File(outputDir, "geometry_stats.json").writeText(content)

    @Test
    fun `extracts building metadata from authoring config`() {
        writeConfig("""
        {
            "buildingId": "draft-test",
            "buildingName": "Test Building",
            "floorId": "ground",
            "nodes": [],
            "edges": [],
            "rooms": [],
            "entranceMarkers": [],
            "checkpointMarkers": []
        }
        """.trimIndent())

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertEquals("draft-test", summary.buildingId)
        assertEquals("Test Building", summary.buildingName)
        assertEquals("ground", summary.floorId)
    }

    @Test
    fun `counts nodes edges and rooms correctly`() {
        writeConfig("""
        {
            "buildingId": "x",
            "nodes": [{"id":"n01"},{"id":"n02"}],
            "edges": [{"id":"e01"}],
            "rooms": [
                {"id":"r1","displayName":"Room A","destinationNodeId":"n01","category":"room"},
                {"id":"r2","displayName":"Room B","destinationNodeId":"n02","category":"unknown"}
            ],
            "entranceMarkers": [{"id":"marker-a"}],
            "checkpointMarkers": []
        }
        """.trimIndent())

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertEquals(2, summary.counts.nodes)
        assertEquals(1, summary.counts.edges)
        assertEquals(2, summary.counts.rooms)
        assertEquals(1, summary.counts.entranceMarkers)
        assertEquals(0, summary.counts.checkpointMarkers)
        assertEquals(2, summary.rooms.size)
        assertEquals("Room A", summary.rooms[0].displayName)
        assertEquals("n01", summary.rooms[0].destinationNodeId)
    }

    @Test
    fun `warns when rooms list is empty`() {
        writeConfig("""
        {
            "buildingId": "x",
            "nodes": [{"id":"n01"}],
            "edges": [],
            "rooms": [],
            "entranceMarkers": [],
            "checkpointMarkers": []
        }
        """.trimIndent())

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertTrue(summary.warnings.any { it.contains("No rooms") })
    }

    @Test
    fun `warns when authoring config is missing`() {
        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertTrue(summary.warnings.any { it.contains("authoring_config.generated.json") })
        assertEquals(0, summary.counts.nodes)
        assertEquals(0, summary.rooms.size)
    }

    @Test
    fun `parses generation metadata`() {
        writeConfig("""{"buildingId":"x","nodes":[],"edges":[],"rooms":[],"entranceMarkers":[],"checkpointMarkers":[]}""")
        writeMetadata("""
        {
            "generatedBy": "nav-preprocessor",
            "timestamp": "2026-03-29T10:00:00Z",
            "confidence": "low",
            "editRequired": true,
            "floorY": -1.32,
            "floorConfidence": 0.16
        }
        """.trimIndent())

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertNotNull(summary.generationMetadata)
        assertEquals("nav-preprocessor", summary.generationMetadata?.generatedBy)
        assertEquals("low", summary.generationMetadata?.confidence)
        assertEquals(true, summary.generationMetadata?.editRequired)
        assertEquals(-1.32, summary.generationMetadata?.floorY)
    }

    @Test
    fun `parses geometry stats`() {
        writeConfig("""{"buildingId":"x","nodes":[],"edges":[],"rooms":[],"entranceMarkers":[],"checkpointMarkers":[]}""")
        writeStats("""
        {
            "totalVertices": 22373,
            "meshCount": 186,
            "boundingBox": {"extentX": 10.3, "extentY": 2.7, "extentZ": 8.8},
            "occupancyGrid": {"width": 44, "height": 38, "cellSize": 0.25},
            "zones": [{"id":"zone-a"},{"id":"zone-b"}]
        }
        """.trimIndent())

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertNotNull(summary.geometryStats)
        assertEquals(22373, summary.geometryStats?.totalVertices)
        assertEquals(186, summary.geometryStats?.meshCount)
        assertEquals(44, summary.geometryStats?.occupancyGridWidth)
        assertEquals(2, summary.geometryStats?.zoneCount)
    }

    @Test
    fun `reports artifact availability`() {
        writeConfig("""{"buildingId":"x","nodes":[],"edges":[],"rooms":[],"entranceMarkers":[],"checkpointMarkers":[]}""")
        writeMetadata("""{}""")
        writeStats("""{}""")
        File(outputDir, "occupancy_debug.svg").writeText("<svg/>")
        File(outputDir, "draft_graph_debug.svg").writeText("<svg/>")

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertTrue(summary.artifactAvailability.hasAuthoringConfig)
        assertTrue(summary.artifactAvailability.hasGenerationMetadata)
        assertTrue(summary.artifactAvailability.hasGeometryStats)
        assertTrue(summary.artifactAvailability.hasOccupancyPreview)
        assertTrue(summary.artifactAvailability.hasGraphPreview)
    }

    @Test
    fun `warns when SVG previews are missing`() {
        writeConfig("""{"buildingId":"x","nodes":[],"edges":[],"rooms":[],"entranceMarkers":[],"checkpointMarkers":[]}""")

        val summary = extractor.extract("job1", JobStatus.succeeded, outputDir)

        assertFalse(summary.artifactAvailability.hasOccupancyPreview)
        assertFalse(summary.artifactAvailability.hasGraphPreview)
        assertTrue(summary.warnings.any { it.contains("occupancy_debug.svg") })
        assertTrue(summary.warnings.any { it.contains("draft_graph_debug.svg") })
    }
}
