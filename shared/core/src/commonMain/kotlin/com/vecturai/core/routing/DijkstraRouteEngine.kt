package com.vecturai.core.routing

import com.vecturai.core.domain.NavGraph
import com.vecturai.core.domain.Route
import com.vecturai.core.domain.RouteSegment

/**
 * Shortest-path routing engine using Dijkstra's algorithm.
 *
 * This is the default routing strategy for the MVP. It computes
 * the path with minimum total edge weight (distance) between
 * two nodes in the navigation graph.
 *
 * TODO: Implement actual Dijkstra's algorithm
 * TODO: Generate human-readable turn-by-turn instructions
 * TODO: Calculate heading changes between consecutive segments
 * TODO: Estimate walking time based on average walking speed
 */
class DijkstraRouteEngine : RouteEngine {

    override val strategyName: String = "dijkstra-shortest-path"

    override suspend fun computeRoute(
        graph: NavGraph,
        fromNodeId: String,
        toNodeId: String,
    ): Route? {
        // Validate that both nodes exist in the graph
        val fromNode = graph.nodeMap[fromNodeId] ?: return null
        val toNode = graph.nodeMap[toNodeId] ?: return null

        // TODO: Implement Dijkstra's algorithm
        // 1. Initialize distances map: all nodes → Infinity, source → 0
        // 2. Priority queue ordered by distance
        // 3. For each node, relax edges to neighbors
        // 4. Reconstruct path from predecessor map
        // 5. Convert node sequence to RouteSegments with instructions

        // Stub: return a direct single-segment route
        val directDistance = calculateDistance(
            fromNode.x, fromNode.y,
            toNode.x, toNode.y,
        )

        val stubSegment = RouteSegment(
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            distanceMeters = directDistance,
            instruction = "Navigate to destination", // TODO: Real turn-by-turn
        )

        return Route(
            originNodeId = fromNodeId,
            destinationNodeId = toNodeId,
            segments = listOf(stubSegment),
            totalDistanceMeters = directDistance,
            estimatedTimeSeconds = (directDistance / AVERAGE_WALKING_SPEED).toInt(),
        )
    }

    private fun calculateDistance(
        x1: Double, y1: Double,
        x2: Double, y2: Double,
    ): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        /** Average indoor walking speed in meters/second. */
        private const val AVERAGE_WALKING_SPEED = 1.2
    }
}
