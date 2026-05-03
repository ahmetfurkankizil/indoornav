package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.model.*
import kotlin.test.*

/**
 * Tests for [AuthoringConfigLoader] — structural validation.
 */
class AuthoringConfigLoaderTest {

    private val loader = AuthoringConfigLoader()

    private fun validConfig() = AuthoringConfig(
        buildingId = "test",
        buildingName = "Test Building",
        asset = AssetReference("scan.glb"),
        nodes = listOf(
            AuthoringNode("n1", 0.0, 0.0),
            AuthoringNode("n2", 3.0, 0.0),
        ),
        edges = listOf(AuthoringEdge("e1", "n1", "n2", 3.0)),
        rooms = listOf(AuthoringRoom("r1", "Room 1", "n2")),
        entranceMarkers = listOf(
            AuthoringMarker("m1", "n1", 0.2, 0.2, Position3D(0.0, 1.0, 0.0)),
        ),
    )

    @Test
    fun `valid config passes structural validation`() {
        val errors = loader.validateStructure(validConfig())
        assertTrue(errors.isEmpty(), "Expected no errors, got: $errors")
    }

    @Test
    fun `blank buildingId fails`() {
        val errors = loader.validateStructure(validConfig().copy(buildingId = ""))
        assertTrue(errors.any { "buildingId" in it })
    }

    @Test
    fun `blank buildingName fails`() {
        val errors = loader.validateStructure(validConfig().copy(buildingName = ""))
        assertTrue(errors.any { "buildingName" in it })
    }

    @Test
    fun `fewer than 2 nodes fails`() {
        val errors = loader.validateStructure(validConfig().copy(
            nodes = listOf(AuthoringNode("n1", 0.0, 0.0)),
        ))
        assertTrue(errors.any { "2 nodes" in it })
    }

    @Test
    fun `no edges fails`() {
        val errors = loader.validateStructure(validConfig().copy(edges = emptyList()))
        assertTrue(errors.any { "edge" in it.lowercase() })
    }

    @Test
    fun `no rooms fails`() {
        val errors = loader.validateStructure(validConfig().copy(rooms = emptyList()))
        assertTrue(errors.any { "room" in it.lowercase() })
    }

    @Test
    fun `no entrance markers fails`() {
        val errors = loader.validateStructure(validConfig().copy(entranceMarkers = emptyList()))
        assertTrue(errors.any { "entrance" in it.lowercase() || "marker" in it.lowercase() })
    }

    @Test
    fun `node with blank id fails`() {
        val errors = loader.validateStructure(validConfig().copy(
            nodes = listOf(
                AuthoringNode("", 0.0, 0.0),
                AuthoringNode("n2", 3.0, 0.0),
            ),
        ))
        assertTrue(errors.any { "blank id" in it.lowercase() })
    }

    @Test
    fun `marker with zero width fails`() {
        val errors = loader.validateStructure(validConfig().copy(
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n1", 0.0, 0.2, Position3D(0.0, 1.0, 0.0)),
            ),
        ))
        assertTrue(errors.any { "physicalWidthMeters" in it })
    }

    @Test
    fun `marker with invalid forwardBasis fails`() {
        val errors = loader.validateStructure(validConfig().copy(
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n1", 0.2, 0.2, Position3D(0.0, 1.0, 0.0), forwardBasis = "+y"),
            ),
        ))
        assertTrue(errors.any { "forwardBasis" in it })
    }

    @Test
    fun `rendering config with too small arrowSpacing fails`() {
        val errors = loader.validateStructure(validConfig().copy(
            routeRendering = AuthoringRouteRendering(arrowSpacingMeters = 0.1),
        ))
        assertTrue(errors.any { "arrowSpacingMeters" in it })
    }

    @Test
    fun `load from missing file throws`() {
        assertFailsWith<ValidationException> {
            loader.load("/nonexistent/file.json")
        }
    }
}
