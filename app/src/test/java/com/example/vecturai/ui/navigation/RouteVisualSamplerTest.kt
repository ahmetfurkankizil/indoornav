package com.example.vecturai.ui.navigation

import com.example.vecturai.ar.Vec3
import com.example.vecturai.graph.EdgeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteVisualSamplerTest {
    @Test
    fun straightHallwayProducesEvenlySpacedArrows() {
        val nodes = listOf(
            node("a", z = 0f),
            node("b", z = 10f)
        )
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = listOf(edge("a", "b")),
            userPosition = Vec3(0f, 1.4f, 0f),
            currentFloor = 0
        )

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = listOf(edge("a", "b")),
            projection = projection!!,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, 0f, 1f)
        )

        assertEquals(FLOOR_ARROW_COUNT, visual.arrows.size)
        assertEquals(0.8f, visual.arrows[0].position.z, 0.001f)
        assertEquals(1.8f, visual.arrows[1].position.z, 0.001f)
        assertEquals(0f, visual.arrows[0].yawDegrees, 0.001f)
        assertTrue(visual.arrows.last().distanceAheadMeters <= FLOOR_ARROW_MAX_DISTANCE_M)
    }

    @Test
    fun shortFinalSegmentClampsFirstArrowAtDestination() {
        val nodes = listOf(
            node("a", z = 0f),
            node("b", z = 0.5f)
        )
        val edges = listOf(edge("a", "b"))
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.4f, 0f),
            currentFloor = 0
        )

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = edges,
            projection = projection!!,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, 0f, 1f)
        )

        assertEquals(listOf(0), visual.arrows.map { it.sampleIndex })
        assertEquals(0.5f, visual.arrows.single().position.z, 0.001f)
    }

    @Test
    fun sampleIndicesStayStableWhenFewArrowsAreVisible() {
        val nodes = listOf(
            node("a", z = 0f),
            node("b", z = 2f)
        )
        val edges = listOf(edge("a", "b"))
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.4f, 0f),
            currentFloor = 0
        )

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = edges,
            projection = projection!!,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, 0f, 1f)
        )

        assertEquals(listOf(0, 1), visual.arrows.map { it.sampleIndex })
    }

    @Test
    fun distancesComeFromProjectionCumulativeModel() {
        val nodes = listOf(
            node("a", z = 0f),
            node("b", z = 4f),
            node("c", z = 7f)
        )
        val edges = listOf(edge("a", "b"), edge("b", "c"))
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.4f, 1f),
            currentFloor = 0
        )

        val distances = RouteVisualSampler.distances(
            nodes = nodes,
            edges = edges,
            projection = projection!!,
            currentFloor = 0
        )

        assertEquals(3f, distances.distanceToNextMeters!!, 0.001f)
        assertEquals(6f, distances.distanceToDestinationMeters!!, 0.001f)
    }

    private fun node(
        id: String,
        x: Float = 0f,
        y: Float = 1.35f,
        z: Float = 0f,
        floor: Int = 0
    ) = RouteSampleNode(
        id = id,
        position = Vec3(x, y, z),
        floor = floor
    )

    private fun edge(
        fromId: String,
        toId: String,
        kind: EdgeKind = EdgeKind.CORRIDOR
    ) = RouteSampleEdge(fromId, toId, kind)
}
