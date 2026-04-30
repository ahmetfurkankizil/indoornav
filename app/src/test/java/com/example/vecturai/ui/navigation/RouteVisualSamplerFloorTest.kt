package com.example.vecturai.ui.navigation

import com.example.vecturai.ar.Vec3
import com.example.vecturai.graph.EdgeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteVisualSamplerFloorTest {
    @Test
    fun samplesStopBeforeVerticalTransition() {
        val nodes = listOf(
            node("a", z = 0f, floor = 0),
            node("b", z = 2f, floor = 0),
            node("c", z = 2f, floor = 1, y = 4f)
        )
        val edges = listOf(
            edge("a", "b"),
            edge("b", "c", EdgeKind.STAIRS)
        )
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.35f, 0f),
            currentFloor = 0
        )!!

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = edges,
            projection = projection,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, -0.1f, 1f)
        )

        assertTrue(visual.arrows.all { it.position.z <= 2f })
        assertEquals(EdgeKind.STAIRS, visual.transitionCue?.kind)
        assertEquals(1, visual.transitionCue?.toFloor)
    }

    @Test
    fun sameFloorElevatorEdgeIsTransitionNotCorridor() {
        val nodes = listOf(
            node("a", z = 0f, floor = 0),
            node("b", z = 2f, floor = 0),
            node("c", z = 4f, floor = 0)
        )
        val edges = listOf(
            edge("a", "b"),
            edge("b", "c", EdgeKind.ELEVATOR)
        )
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.35f, 0f),
            currentFloor = 0
        )!!

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = edges,
            projection = projection,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, 0f, 1f)
        )

        assertTrue(visual.arrows.all { it.segmentIndex == 0 })
        assertEquals(EdgeKind.ELEVATOR, visual.transitionCue?.kind)
    }

    @Test
    fun onlyCurrentFloorCorridorSegmentsAreProjected() {
        val nodes = listOf(
            node("a", z = 0f, floor = 0),
            node("b", z = 2f, floor = 0),
            node("c", z = 4f, floor = 1)
        )
        val edges = listOf(edge("a", "b"), edge("b", "c"))

        val floorZeroProjection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.35f, 1f),
            currentFloor = 0
        )
        val floorOneProjection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.35f, 3f),
            currentFloor = 1
        )

        assertEquals(0, floorZeroProjection?.segmentIndex)
        assertNull(floorOneProjection)
    }

    @Test
    fun floorYUsesEstimatePlusArrowOffset() {
        val nodes = listOf(node("a", z = 0f), node("b", z = 3f))
        val edges = listOf(edge("a", "b"))
        val projection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.35f, 0f),
            currentFloor = 0
        )!!

        val visual = RouteVisualSampler.sample(
            nodes = nodes,
            edges = edges,
            projection = projection,
            currentFloor = 0,
            floorHeightEstimate = FloorHeightEstimate(0, -1f, 1f)
        )

        assertEquals(-1f + FLOOR_ARROW_Y_OFFSET_M, visual.arrows.first().position.y, 0.001f)
    }

    private fun node(
        id: String,
        z: Float,
        floor: Int = 0,
        y: Float = 1.35f
    ) = RouteSampleNode(
        id = id,
        position = Vec3(0f, y, z),
        floor = floor
    )

    private fun edge(
        fromId: String,
        toId: String,
        kind: EdgeKind = EdgeKind.CORRIDOR
    ) = RouteSampleEdge(fromId, toId, kind)
}
