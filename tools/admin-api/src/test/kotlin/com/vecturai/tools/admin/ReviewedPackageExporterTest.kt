package com.Vectura AI.tools.admin

import com.Vectura AI.tools.admin.model.RoomOverride
import com.Vectura AI.tools.admin.model.RoomOverrides
import com.Vectura AI.tools.admin.service.ReviewedPackageExporter
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.*

class ReviewedPackageExporterTest {

    private lateinit var jobDir: File
    private lateinit var outputDir: File
    private lateinit var exporter: ReviewedPackageExporter
    private val jobId = "exportjob1"

    @BeforeTest
    fun setup() {
        val base = File("build/test-exporter-${System.nanoTime()}")
        jobDir = File(base, jobId)
        outputDir = File(jobDir, "output")
        outputDir.mkdirs()
        exporter = ReviewedPackageExporter(base.absolutePath)
    }

    @AfterTest
    fun cleanup() {
        jobDir.parentFile.deleteRecursively()
    }

    private fun writeMinimalConfig(rooms: List<Map<String, String>> = emptyList()) {
        val roomsJson = rooms.joinToString(",\n") { r ->
            """{"id":"${r["id"]}","displayName":"${r["displayName"]}","destinationNodeId":"${r["node"]}","category":"${r["cat"]}","description":"${r["desc"]}"}"""
        }
        File(outputDir, "authoring_config.generated.json").writeText("""
        {
            "buildingId": "draft-test",
            "buildingName": "Test Building",
            "floorId": "ground",
            "nodes": [{"id":"n01","x":0,"y":0,"z":0,"type":"junction","label":"A"}],
            "edges": [{"id":"e01","from":"n01","to":"n01","cost":1,"bidirectional":true}],
            "rooms": [$roomsJson],
            "entranceMarkers": [{"id":"marker-draft","startNodeId":"n01","physicalWidthMeters":0.21,"physicalHeightMeters":0.21,"position":{"x":0,"y":0,"z":0},"forwardBasis":"-z","rotationYDegrees":0}],
            "checkpointMarkers": [],
            "routeRendering": {"arrowSpacingMeters":1.5,"lookaheadDistanceMeters":8.0,"destinationThresholdMeters":1.5,"turnMarkerThresholdDegrees":30,"arrowHeightOffsetMeters":0.05}
        }
        """.trimIndent())
    }

    @Test
    fun `export fails when config missing`() {
        val result = exporter.export(jobId, RoomOverrides())
        assertEquals("failed", result.status)
        assertTrue(result.warnings.any { it.contains("authoring_config") })
    }

    @Test
    fun `export produces all 5 required files`() {
        writeMinimalConfig()
        val result = exporter.export(jobId, RoomOverrides())
        assertEquals("succeeded", result.status)
        val expected = setOf("manifest.json", "rooms.json", "nav_graph.json", "entrance_markers.json", "route_rendering.json")
        assertEquals(expected, result.files.toSet())

        val exportDir = File(jobDir, "reviewed-package")
        expected.forEach { f -> assertTrue(File(exportDir, f).exists(), "$f should exist") }
    }

    @Test
    fun `exported manifest has correct buildingId and status`() {
        writeMinimalConfig()
        exporter.export(jobId, RoomOverrides())
        val manifest = Json.parseToJsonElement(File(jobDir, "reviewed-package/manifest.json").readText()).jsonObject
        assertEquals("draft-test", manifest["buildingId"]?.jsonPrimitive?.content)
        assertEquals("exported-from-admin", manifest["reviewStatus"]?.jsonPrimitive?.content)
    }

    @Test
    fun `exported rooms use override displayName and category`() {
        writeMinimalConfig(listOf(
            mapOf("id" to "zone-a", "displayName" to "Zone A", "node" to "n01", "cat" to "unknown", "desc" to "")
        ))
        val overrides = RoomOverrides(
            overrides = mapOf("zone-a" to RoomOverride(displayName = "Kitchen", category = "kitchen"))
        )
        exporter.export(jobId, overrides)
        val rooms = Json.parseToJsonElement(File(jobDir, "reviewed-package/rooms.json").readText())
            .jsonObject["rooms"]!!.jsonArray
        assertEquals(1, rooms.size)
        assertEquals("Kitchen", rooms[0].jsonObject["displayName"]?.jsonPrimitive?.content)
        assertEquals("kitchen", rooms[0].jsonObject["category"]?.jsonPrimitive?.content)
    }

    @Test
    fun `exported rooms without overrides use draft values`() {
        writeMinimalConfig(listOf(
            mapOf("id" to "zone-b", "displayName" to "Zone B", "node" to "n01", "cat" to "unknown", "desc" to "original")
        ))
        exporter.export(jobId, RoomOverrides())
        val rooms = Json.parseToJsonElement(File(jobDir, "reviewed-package/rooms.json").readText())
            .jsonObject["rooms"]!!.jsonArray
        assertEquals("Zone B", rooms[0].jsonObject["displayName"]?.jsonPrimitive?.content)
        assertEquals("original", rooms[0].jsonObject["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun `exported nav_graph contains nodes and edges`() {
        writeMinimalConfig()
        exporter.export(jobId, RoomOverrides())
        val graph = Json.parseToJsonElement(File(jobDir, "reviewed-package/nav_graph.json").readText()).jsonObject
        assertEquals(1, graph["nodes"]!!.jsonArray.size)
        assertEquals(1, graph["edges"]!!.jsonArray.size)
    }

    @Test
    fun `exported route_rendering matches draft config`() {
        writeMinimalConfig()
        exporter.export(jobId, RoomOverrides())
        val rr = Json.parseToJsonElement(File(jobDir, "reviewed-package/route_rendering.json").readText()).jsonObject
        assertEquals(1.5, rr["arrowSpacingMeters"]?.jsonPrimitive?.double)
        assertEquals(8.0, rr["lookaheadDistanceMeters"]?.jsonPrimitive?.double)
    }

    @Test
    fun `export can be re-run and overwrites previous export`() {
        writeMinimalConfig(listOf(
            mapOf("id" to "zone-a", "displayName" to "Zone A", "node" to "n01", "cat" to "unknown", "desc" to "")
        ))
        exporter.export(jobId, RoomOverrides())
        val overrides = RoomOverrides(overrides = mapOf("zone-a" to RoomOverride(displayName = "Renamed")))
        exporter.export(jobId, overrides)

        val rooms = Json.parseToJsonElement(File(jobDir, "reviewed-package/rooms.json").readText())
            .jsonObject["rooms"]!!.jsonArray
        assertEquals("Renamed", rooms[0].jsonObject["displayName"]?.jsonPrimitive?.content)
    }
}
