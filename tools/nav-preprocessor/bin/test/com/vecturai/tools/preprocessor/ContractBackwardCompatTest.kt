package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.*

/**
 * Tests for backward compatibility of contract serialization.
 *
 * Ensures that:
 * - Existing JSON without checkpointMarkers deserializes correctly
 * - checkpointMarkers defaults to empty list
 * - New JSON with checkpointMarkers round-trips correctly
 */
class ContractBackwardCompatTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `authoring config without checkpointMarkers deserializes with empty list`() {
        val jsonStr = """
        {
            "buildingId": "test",
            "buildingName": "Test Building",
            "floorId": "ground",
            "asset": { "glbFile": "scan.glb" },
            "entranceMarkers": [{
                "id": "m1",
                "startNodeId": "n01",
                "physicalWidthMeters": 0.2,
                "physicalHeightMeters": 0.2,
                "position": { "x": 0.0, "y": 0.0, "z": 0.0 }
            }],
            "nodes": [
                { "id": "n01", "x": 0.0, "y": 0.0 },
                { "id": "n02", "x": 3.0, "y": 0.0 }
            ],
            "edges": [
                { "id": "e1", "from": "n01", "to": "n02", "cost": 3.0 }
            ],
            "rooms": [
                { "id": "r1", "displayName": "Kitchen", "destinationNodeId": "n02" }
            ]
        }
        """.trimIndent()

        val config = json.decodeFromString<AuthoringConfig>(jsonStr)
        assertEquals("test", config.buildingId)
        assertTrue(config.checkpointMarkers.isEmpty(), "checkpointMarkers should default to empty")
        assertEquals(1, config.entranceMarkers.size)
    }

    @Test
    fun `authoring config with checkpointMarkers round-trips`() {
        val config = AuthoringConfig(
            buildingId = "test",
            buildingName = "Test",
            asset = AssetReference("scan.glb"),
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n01", 0.2, 0.2, Position3D(0.0, 0.0, 0.0)),
            ),
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("cp1", "n02", 0.15, 0.15, Position3D(3.0, 0.0, 0.0)),
            ),
            nodes = listOf(
                AuthoringNode("n01", 0.0, 0.0),
                AuthoringNode("n02", 3.0, 0.0),
            ),
            edges = listOf(
                AuthoringEdge("e1", "n01", "n02", 3.0),
            ),
            rooms = listOf(
                AuthoringRoom("r1", "Kitchen", "n02"),
            ),
        )

        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<AuthoringConfig>(serialized)

        assertEquals(1, deserialized.checkpointMarkers.size)
        assertEquals("cp1", deserialized.checkpointMarkers[0].id)
        assertEquals("n02", deserialized.checkpointMarkers[0].nearestNodeId)
        assertEquals(3.0, deserialized.checkpointMarkers[0].position.x, 0.001)
    }

    @Test
    fun `existing single entrance marker config validates`() {
        val config = AuthoringConfig(
            buildingId = "test",
            buildingName = "Test Building",
            asset = AssetReference("scan.glb"),
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n01", 0.2, 0.2, Position3D(0.0, 0.0, 0.0)),
            ),
            nodes = listOf(
                AuthoringNode("n01", 0.0, 0.0),
                AuthoringNode("n02", 3.0, 0.0),
            ),
            edges = listOf(
                AuthoringEdge("e1", "n01", "n02", 3.0),
            ),
            rooms = listOf(
                AuthoringRoom("r1", "Kitchen", "n02"),
            ),
        )

        val validator = GraphValidator()
        val result = validator.validate(config)
        assertTrue(result.isValid, "Existing single-marker config should still validate: ${result.errors}")
    }

    @Test
    fun `checkpoint marker serialization includes all fields`() {
        val cp = AuthoringCheckpointMarker(
            id = "cp1",
            nearestNodeId = "n02",
            physicalWidthMeters = 0.15,
            physicalHeightMeters = 0.15,
            position = Position3D(3.0, 0.0, 0.5),
            rotationYDegrees = 90.0,
            referenceImageName = "cp1_ref",
            notes = "Placed on hallway wall",
        )

        val serialized = json.encodeToString(cp)
        assertTrue("cp1" in serialized)
        assertTrue("n02" in serialized)
        assertTrue("cp1_ref" in serialized)
        assertTrue("hallway" in serialized)

        val deserialized = json.decodeFromString<AuthoringCheckpointMarker>(serialized)
        assertEquals(cp.id, deserialized.id)
        assertEquals(cp.rotationYDegrees, deserialized.rotationYDegrees, 0.001)
        assertEquals(cp.notes, deserialized.notes)
    }
}
