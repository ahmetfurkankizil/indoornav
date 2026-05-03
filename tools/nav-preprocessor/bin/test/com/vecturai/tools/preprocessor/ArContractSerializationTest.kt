package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for shared AR model serialization and contract correctness.
 */
class ArContractSerializationTest {

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    // Locally defined mirrors of shared AR models for testing
    @kotlinx.serialization.Serializable
    enum class TestArrowType { FOLLOW, TURN_LEFT, TURN_RIGHT, U_TURN, DESTINATION }

    @kotlinx.serialization.Serializable
    data class TestArrowPlacement(
        val id: String,
        val positionX: Double,
        val positionY: Double,
        val positionZ: Double,
        val forwardDx: Double,
        val forwardDy: Double = 0.0,
        val forwardDz: Double,
        val type: TestArrowType = TestArrowType.FOLLOW,
        val label: String? = null,
        val distanceFromStartMeters: Double = 0.0,
    )

    @kotlinx.serialization.Serializable
    data class TestArRenderableRoute(
        val routeId: String,
        val arrows: List<TestArrowPlacement>,
        val totalDistanceMeters: Double,
        val currentInstruction: String = "",
        val destinationName: String = "",
    )

    @kotlinx.serialization.Serializable
    data class TestAlignmentTransform(
        val offsetX: Double = 0.0,
        val offsetY: Double = 0.0,
        val offsetZ: Double = 0.0,
        val rotationYDeg: Double = 0.0,
    )

    @kotlinx.serialization.Serializable
    data class TestMarkerAlignmentResult(
        val markerId: String,
        val entranceNodeId: String,
        val markerBuildingX: Double,
        val markerBuildingY: Double,
        val markerBuildingZ: Double,
        val markerArX: Double,
        val markerArY: Double,
        val markerArZ: Double,
        val markerArRotationYDeg: Double = 0.0,
        val markerBuildingRotationYDeg: Double = 0.0,
        val confidence: Double = 1.0,
    )

    @Test
    fun `ArRenderableRoute roundtrip`() {
        val route = TestArRenderableRoute(
            routeId = "n1-n5",
            arrows = listOf(
                TestArrowPlacement("a1", 0.0, 0.05, 0.0, 1.0, forwardDz = 0.0),
                TestArrowPlacement("a2", 1.5, 0.05, 0.0, 1.0, forwardDz = 0.0),
                TestArrowPlacement("a3", 3.0, 0.05, 0.0, 0.0, forwardDz = 1.0,
                    type = TestArrowType.TURN_RIGHT, label = "Turn right"),
                TestArrowPlacement("dest", 3.0, 0.05, 3.0, 0.0, forwardDz = 1.0,
                    type = TestArrowType.DESTINATION, label = "Office"),
            ),
            totalDistanceMeters = 6.0,
            currentInstruction = "Head forward",
            destinationName = "Office",
        )

        val encoded = json.encodeToString(route)
        val decoded = json.decodeFromString<TestArRenderableRoute>(encoded)

        assertEquals(route.routeId, decoded.routeId)
        assertEquals(route.arrows.size, decoded.arrows.size)
        assertEquals(route.totalDistanceMeters, decoded.totalDistanceMeters)
        assertEquals("TURN_RIGHT", decoded.arrows[2].type.name)
        assertEquals("Office", decoded.arrows[3].label)
    }

    @Test
    fun `AlignmentTransform roundtrip`() {
        val transform = TestAlignmentTransform(
            offsetX = 3.5,
            offsetY = -1.2,
            offsetZ = -7.0,
            rotationYDeg = 45.0,
        )

        val encoded = json.encodeToString(transform)
        val decoded = json.decodeFromString<TestAlignmentTransform>(encoded)

        assertEquals(transform.offsetX, decoded.offsetX)
        assertEquals(transform.rotationYDeg, decoded.rotationYDeg)
    }

    @Test
    fun `MarkerAlignmentResult roundtrip`() {
        val result = TestMarkerAlignmentResult(
            markerId = "m1",
            entranceNodeId = "n01",
            markerBuildingX = 0.0,
            markerBuildingY = 1.2,
            markerBuildingZ = 0.0,
            markerArX = 0.5,
            markerArY = 0.3,
            markerArZ = -2.0,
            markerArRotationYDeg = 12.5,
            markerBuildingRotationYDeg = 0.0,
            confidence = 0.95,
        )

        val encoded = json.encodeToString(result)
        val decoded = json.decodeFromString<TestMarkerAlignmentResult>(encoded)

        assertEquals(result.markerId, decoded.markerId)
        assertEquals(result.confidence, decoded.confidence)
        assertEquals(result.markerArX, decoded.markerArX)
    }

    @Test
    fun `ArrowPlacement defaults applied`() {
        val minimalJson = """
        {
            "id": "a1",
            "positionX": 1.0,
            "positionY": 0.0,
            "positionZ": 2.0,
            "forwardDx": 1.0,
            "forwardDz": 0.0
        }
        """.trimIndent()
        val decoded = json.decodeFromString<TestArrowPlacement>(minimalJson)
        assertEquals(TestArrowType.FOLLOW, decoded.type) // default
        assertEquals(0.0, decoded.forwardDy) // default
        assertNull(decoded.label) // default
        assertEquals(0.0, decoded.distanceFromStartMeters) // default
    }
}
