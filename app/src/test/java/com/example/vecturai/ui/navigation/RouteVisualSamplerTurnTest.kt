package com.example.vecturai.ui.navigation

import com.example.vecturai.ar.Vec3
import com.example.vecturai.graph.EdgeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteVisualSamplerTurnTest {
    @Test
    fun lRouteSamplesBeforeAndAfterCornerWithoutDiagonalShortcut() {
        val nodes = listOf(
            node("a", x = 0f, z = 0f),
            node("b", x = 0f, z = 2f),
            node("c", x = 3f, z = 2f)
        )
        val edges = listOf(edge("a", "b"), edge("b", "c"))
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

        assertTrue(visual.arrows.any { it.position.z < 2f && it.position.x == 0f })
        assertTrue(visual.arrows.any { it.position.z == 2f && it.position.x > 0f })
        assertEquals(0f, visual.arrows[0].yawDegrees, 0.001f)
        assertEquals(90f, visual.arrows.first { it.position.x > 0f }.yawDegrees, 0.001f)
    }

    @Test
    fun turnDirectionChangesOnlyNearTheCorner() {
        val nodes = listOf(
            node("a", x = 0f, z = 0f),
            node("b", x = 0f, z = 3f),
            node("c", x = -3f, z = 3f)
        )
        val edges = listOf(edge("a", "b"), edge("b", "c"))

        val farProjection = RouteVisualSampler.projectToPath(
            nodes = nodes,
            edges = edges,
            userPosition = Vec3(0f, 1.4f, 0f),
            currentFloor = 0
        )!!
        val nearProjection = farProjection.copy(
            segmentT = 0.8f,
            cumulativeMeters = 2.4f
        )

        assertEquals(
            null,
            RouteVisualSampler.turnDirectionNearProjection(nodes, edges, farProjection, 0)
        )
        assertEquals(
            RouteTurnDirection.LEFT,
            RouteVisualSampler.turnDirectionNearProjection(nodes, edges, nearProjection, 0)
        )
    }

    private fun node(
        id: String,
        x: Float,
        z: Float
    ) = RouteSampleNode(
        id = id,
        position = Vec3(x, 1.35f, z),
        floor = 0
    )

    private fun edge(
        fromId: String,
        toId: String,
        kind: EdgeKind = EdgeKind.CORRIDOR
    ) = RouteSampleEdge(fromId, toId, kind)
}
