package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.*
import kotlin.test.*

/**
 * Tests for checkpoint marker validation in GraphValidator.
 */
class CheckpointMarkerValidationTest {

    private val validator = GraphValidator()

    private fun baseConfig(
        entranceMarkers: List<AuthoringMarker> = listOf(
            AuthoringMarker("m1", "n01", 0.2, 0.2, Position3D(0.0, 0.0, 0.0))
        ),
        checkpointMarkers: List<AuthoringCheckpointMarker> = emptyList(),
    ) = AuthoringConfig(
        buildingId = "test",
        buildingName = "Test Building",
        asset = AssetReference("scan.glb"),
        entranceMarkers = entranceMarkers,
        checkpointMarkers = checkpointMarkers,
        nodes = listOf(
            AuthoringNode("n01", 0.0, 0.0),
            AuthoringNode("n02", 3.0, 0.0),
            AuthoringNode("n03", 6.0, 0.0),
        ),
        edges = listOf(
            AuthoringEdge("e1", "n01", "n02", 3.0),
            AuthoringEdge("e2", "n02", "n03", 3.0),
        ),
        rooms = listOf(
            AuthoringRoom("r1", "Kitchen", "n03"),
        ),
    )

    @Test
    fun `config with no checkpoint markers passes validation`() {
        val result = validator.validate(baseConfig())
        assertTrue(result.isValid, "Config without checkpoints should pass: ${result.errors}")
    }

    @Test
    fun `config with valid checkpoint markers passes`() {
        val result = validator.validate(baseConfig(
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("cp1", "n02", 0.2, 0.2, Position3D(3.0, 0.0, 0.0)),
            ),
        ))
        assertTrue(result.isValid, "Config with valid checkpoints should pass: ${result.errors}")
    }

    @Test
    fun `duplicate checkpoint marker IDs fail`() {
        val result = validator.validate(baseConfig(
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("cp1", "n02", 0.2, 0.2, Position3D(3.0, 0.0, 0.0)),
                AuthoringCheckpointMarker("cp1", "n03", 0.2, 0.2, Position3D(6.0, 0.0, 0.0)),
            ),
        ))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "Duplicate checkpoint" in it })
    }

    @Test
    fun `checkpoint ID overlapping entrance ID fails`() {
        val result = validator.validate(baseConfig(
            entranceMarkers = listOf(
                AuthoringMarker("m1", "n01", 0.2, 0.2, Position3D(0.0, 0.0, 0.0)),
            ),
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("m1", "n02", 0.2, 0.2, Position3D(3.0, 0.0, 0.0)),
            ),
        ))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "overlap" in it.lowercase() })
    }

    @Test
    fun `checkpoint nearestNodeId must exist`() {
        val result = validator.validate(baseConfig(
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("cp1", "nonexistent", 0.2, 0.2, Position3D(3.0, 0.0, 0.0)),
            ),
        ))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "nonexistent" in it })
    }

    @Test
    fun `multiple valid checkpoints pass`() {
        val result = validator.validate(baseConfig(
            checkpointMarkers = listOf(
                AuthoringCheckpointMarker("cp1", "n02", 0.2, 0.2, Position3D(3.0, 0.0, 0.0)),
                AuthoringCheckpointMarker("cp2", "n03", 0.2, 0.2, Position3D(6.0, 0.0, 0.0)),
            ),
        ))
        assertTrue(result.isValid, "Multiple valid checkpoints should pass: ${result.errors}")
    }
}
