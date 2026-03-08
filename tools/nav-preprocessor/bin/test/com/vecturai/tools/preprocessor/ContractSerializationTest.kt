package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for contract serialization/deserialization roundtrips.
 */
class ContractSerializationTest {

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun `AuthoringConfig roundtrip`() {
        val config = AuthoringConfig(
            buildingId = "test-1",
            buildingName = "Test Building",
            asset = AssetReference("scan.glb", "polycam", "2026-03-08"),
            tags = listOf("demo"),
            nodes = listOf(
                AuthoringNode("n1", 0.0, 0.0, 0.0, "entrance", "Entry"),
                AuthoringNode("n2", 3.0, 0.0, 4.0, "room_entry", "Room"),
            ),
            edges = listOf(AuthoringEdge("e1", "n1", "n2", 5.0, true, "corridor")),
            rooms = listOf(
                AuthoringRoom("r1", "Office", "n2", "office", listOf("desk"), listOf("Room 1")),
            ),
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n1", 0.21, 0.21, Position3D(0.0, 1.2, 0.0), "-z", 0.0, "marker_img"),
            ),
            routeRendering = AuthoringRouteRendering(1.5, 8.0, 1.5, 30.0, 0.05),
            graphMetadata = GraphMetadata("Author", "2026-03-08", "Test notes"),
        )

        val encoded = json.encodeToString(config)
        val decoded = json.decodeFromString<AuthoringConfig>(encoded)

        assertEquals(config.buildingId, decoded.buildingId)
        assertEquals(config.nodes.size, decoded.nodes.size)
        assertEquals(config.edges.size, decoded.edges.size)
        assertEquals(config.rooms.size, decoded.rooms.size)
        assertEquals(config.entranceMarkers.size, decoded.entranceMarkers.size)
        assertEquals(config.routeRendering.arrowSpacingMeters, decoded.routeRendering.arrowSpacingMeters)
    }

    @Test
    fun `PackageManifest roundtrip`() {
        val manifest = PackageManifest(
            buildingId = "b1",
            buildingName = "Test",
            floorId = "ground",
            version = 1,
            schemaVersion = 1,
            generatedAt = "2026-03-08T00:00:00Z",
            preprocessorVersion = "1.0.0",
            assetFile = "preview.glb",
            files = mapOf("nav_graph.json" to "included"),
        )
        val encoded = json.encodeToString(manifest)
        val decoded = json.decodeFromString<PackageManifest>(encoded)
        assertEquals(manifest.buildingId, decoded.buildingId)
        assertEquals(manifest.files.size, decoded.files.size)
    }

    @Test
    fun `PackageNavGraph roundtrip`() {
        val graph = PackageNavGraph(
            buildingId = "b1",
            floorId = "ground",
            schemaVersion = 1,
            nodes = listOf(PackageNode("n1", 0.0, 0.0, 0.0, "waypoint", null, null)),
            edges = listOf(PackageEdge("e1", "n1", "n1", 1.0, true)),
        )
        val encoded = json.encodeToString(graph)
        val decoded = json.decodeFromString<PackageNavGraph>(encoded)
        assertEquals(graph.nodes.size, decoded.nodes.size)
        assertEquals(graph.edges.size, decoded.edges.size)
    }

    @Test
    fun `PackageRooms roundtrip`() {
        val rooms = PackageRooms(
            buildingId = "b1",
            schemaVersion = 1,
            rooms = listOf(
                PackageRoom("r1", "Office", null, "office", listOf("n1"), listOf("desk"), listOf("Room 1"), 1.0, 2.0, "ground"),
            ),
        )
        val encoded = json.encodeToString(rooms)
        val decoded = json.decodeFromString<PackageRooms>(encoded)
        assertEquals("Office", decoded.rooms.first().name)
        assertEquals(listOf("desk"), decoded.rooms.first().keywords)
    }

    @Test
    fun `PackageMarkers roundtrip`() {
        val markers = PackageMarkers(
            buildingId = "b1",
            schemaVersion = 1,
            markers = listOf(
                PackageMarker("m1", "vecturai://test", 0.0, 1.2, 0.0, 0.0, "-z", "n1", 0.21, 0.21, "img"),
            ),
        )
        val encoded = json.encodeToString(markers)
        val decoded = json.decodeFromString<PackageMarkers>(encoded)
        assertEquals("m1", decoded.markers.first().id)
    }

    @Test
    fun `defaults applied for missing optional fields`() {
        val minimalJson = """
        {
            "buildingId": "b1",
            "buildingName": "Test",
            "asset": { "glbFile": "scan.glb" },
            "entranceMarkers": [
                { "id": "m1", "startNodeId": "n1", "physicalWidthMeters": 0.2, "physicalHeightMeters": 0.2, "position": { "x": 0, "y": 0, "z": 0 } }
            ],
            "nodes": [
                { "id": "n1", "x": 0, "y": 0 },
                { "id": "n2", "x": 1, "y": 0 }
            ],
            "edges": [
                { "id": "e1", "from": "n1", "to": "n2", "cost": 1.0 }
            ],
            "rooms": [
                { "id": "r1", "displayName": "Room", "destinationNodeId": "n2" }
            ]
        }
        """.trimIndent()

        val decoded = json.decodeFromString<AuthoringConfig>(minimalJson)
        assertEquals("ground", decoded.floorId) // default
        assertEquals("-z", decoded.entranceMarkers.first().forwardBasis) // default
        assertEquals("waypoint", decoded.nodes.first().type) // default
        assertTrue(decoded.edges.first().bidirectional) // default true
    }
}
