package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlin.test.*

/**
 * Tests for [GraphValidator] — graph integrity and connectivity checks.
 */
class GraphValidatorTest {

    private val validator = GraphValidator()

    private fun validConfig(
        nodes: List<AuthoringNode> = listOf(
            AuthoringNode("n1", 0.0, 0.0),
            AuthoringNode("n2", 3.0, 0.0),
            AuthoringNode("n3", 6.0, 0.0),
        ),
        edges: List<AuthoringEdge> = listOf(
            AuthoringEdge("e1", "n1", "n2", 3.0),
            AuthoringEdge("e2", "n2", "n3", 3.0),
        ),
        rooms: List<AuthoringRoom> = listOf(
            AuthoringRoom("r1", "Room 1", "n3"),
        ),
        markers: List<AuthoringMarker> = listOf(
            AuthoringMarker("m1", "n1", 0.2, 0.2, Position3D(0.0, 1.0, 0.0)),
        ),
    ) = AuthoringConfig(
        buildingId = "test",
        buildingName = "Test Building",
        asset = AssetReference("scan.glb"),
        nodes = nodes,
        edges = edges,
        rooms = rooms,
        entranceMarkers = markers,
    )

    @Test
    fun `valid graph passes validation`() {
        val result = validator.validate(validConfig())
        assertTrue(result.isValid, "Expected valid graph, got errors: ${result.errors}")
    }

    @Test
    fun `duplicate node IDs detected`() {
        val config = validConfig(
            nodes = listOf(
                AuthoringNode("n1", 0.0, 0.0),
                AuthoringNode("n1", 3.0, 0.0),  // duplicate
                AuthoringNode("n2", 6.0, 0.0),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "Duplicate node" in it })
    }

    @Test
    fun `duplicate edge IDs detected`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", 3.0),
                AuthoringEdge("e1", "n2", "n3", 3.0),  // duplicate
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "Duplicate edge" in it })
    }

    @Test
    fun `edge referencing non-existent node detected`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n_missing", 3.0),
                AuthoringEdge("e2", "n2", "n3", 3.0),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "n_missing" in it })
    }

    @Test
    fun `room referencing non-existent node detected`() {
        val config = validConfig(
            rooms = listOf(AuthoringRoom("r1", "Room 1", "n_missing")),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "n_missing" in it })
    }

    @Test
    fun `marker referencing non-existent node detected`() {
        val config = validConfig(
            markers = listOf(
                AuthoringMarker("m1", "n_missing", 0.2, 0.2, Position3D(0.0, 1.0, 0.0)),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "n_missing" in it })
    }

    @Test
    fun `negative edge cost detected`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", -1.0),
                AuthoringEdge("e2", "n2", "n3", 3.0),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "cost must be positive" in it })
    }

    @Test
    fun `zero edge cost detected`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", 0.0),
                AuthoringEdge("e2", "n2", "n3", 3.0),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "cost must be positive" in it })
    }

    @Test
    fun `self-loop detected`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n1", 3.0),  // self-loop
                AuthoringEdge("e2", "n1", "n2", 3.0),
                AuthoringEdge("e3", "n2", "n3", 3.0),
            ),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "self-loop" in it })
    }

    @Test
    fun `disconnected room detected`() {
        val config = validConfig(
            nodes = listOf(
                AuthoringNode("n1", 0.0, 0.0),
                AuthoringNode("n2", 3.0, 0.0),
                AuthoringNode("n3", 6.0, 0.0),  // isolated
            ),
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", 3.0),
                // no edge to n3
            ),
            rooms = listOf(AuthoringRoom("r1", "Room 1", "n3")),
        )
        val result = validator.validate(config)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "cannot reach" in it })
    }

    @Test
    fun `unidirectional edges respected in connectivity check`() {
        val config = validConfig(
            edges = listOf(
                AuthoringEdge("e1", "n1", "n2", 3.0, bidirectional = false),
                AuthoringEdge("e2", "n2", "n3", 3.0, bidirectional = false),
            ),
        )
        // n1 can reach n3, so this should be valid
        val result = validator.validate(config)
        assertTrue(result.isValid)
    }
}
