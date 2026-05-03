package com.Vectura AI.core.routing

import com.Vectura AI.core.domain.NavGraph
import com.Vectura AI.core.domain.NavNode
import com.Vectura AI.core.domain.Route
import com.Vectura AI.core.domain.RouteSegment
import kotlin.math.*

/**
 * Shortest-path routing engine using Dijkstra's algorithm.
 *
 * Computes the minimum-cost path between two nodes in the navigation graph.
 * Handles bidirectional edges (expanded in NavGraph.adjacencyList).
 * Generates turn-by-turn navigation instructions based on heading changes.
 */
class DijkstraRouteEngine : RouteEngine {

    override val strategyName: String = "dijkstra-shortest-path"

    override suspend fun computeRoute(
        graph: NavGraph,
        fromNodeId: String,
        toNodeId: String,
    ): Route? {
        val fromNode = graph.nodeMap[fromNodeId] ?: return null
        val toNode = graph.nodeMap[toNodeId] ?: return null
        if (fromNodeId == toNodeId) {
            return Route(
                originNodeId = fromNodeId,
                destinationNodeId = toNodeId,
                segments = emptyList(),
                totalDistanceMeters = 0.0,
                estimatedTimeSeconds = 0,
            )
        }

        // Dijkstra's algorithm
        val dist = mutableMapOf<String, Double>()
        val prev = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()

        // Initialize all distances to infinity
        for (node in graph.nodes) {
            dist[node.id] = Double.MAX_VALUE
        }
        dist[fromNodeId] = 0.0

        // Simple priority queue via sorted selection (sufficient for building-scale graphs)
        while (true) {
            // Find unvisited node with minimum distance
            val current = dist.entries
                .filter { it.key !in visited && it.value < Double.MAX_VALUE }
                .minByOrNull { it.value }
                ?.key
                ?: break // No reachable unvisited nodes

            if (current == toNodeId) break // Found shortest path to destination
            visited.add(current)

            val neighbors = graph.adjacencyList[current] ?: continue
            for (edge in neighbors) {
                if (edge.to in visited) continue
                val newDist = dist[current]!! + edge.weight
                if (newDist < (dist[edge.to] ?: Double.MAX_VALUE)) {
                    dist[edge.to] = newDist
                    prev[edge.to] = current
                }
            }
        }

        // Check if destination is reachable
        if (toNodeId !in prev && fromNodeId != toNodeId) return null

        // Reconstruct path
        val path = mutableListOf(toNodeId)
        var current = toNodeId
        while (current != fromNodeId) {
            current = prev[current] ?: return null
            path.add(0, current)
        }

        // Build segments with instructions
        val segments = buildSegments(path, graph)

        val totalDistance = segments.sumOf { it.distanceMeters }
        return Route(
            originNodeId = fromNodeId,
            destinationNodeId = toNodeId,
            segments = segments,
            totalDistanceMeters = totalDistance,
            estimatedTimeSeconds = (totalDistance / AVERAGE_WALKING_SPEED).toInt(),
        )
    }

    /**
     * Build route segments from a node path, generating turn-by-turn instructions.
     */
    private fun buildSegments(path: List<String>, graph: NavGraph): List<RouteSegment> {
        if (path.size < 2) return emptyList()

        val segments = mutableListOf<RouteSegment>()
        var prevHeading: Double? = null

        for (i in 0 until path.size - 1) {
            val fromNode = graph.nodeMap[path[i]]!!
            val toNode = graph.nodeMap[path[i + 1]]!!
            val distance = euclideanDistance(fromNode, toNode)
            val heading = calculateHeading(fromNode, toNode)
            val instruction = generateInstruction(prevHeading, heading, i, path.size - 1, toNode)
            prevHeading = heading

            segments.add(
                RouteSegment(
                    fromNodeId = fromNode.id,
                    toNodeId = toNode.id,
                    distanceMeters = distance,
                    instruction = instruction,
                    headingDegrees = heading,
                )
            )
        }
        return segments
    }

    /**
     * Generate a human-readable turn instruction based on heading change.
     */
    private fun generateInstruction(
        prevHeading: Double?,
        currentHeading: Double,
        segmentIndex: Int,
        lastIndex: Int,
        toNode: NavNode,
    ): String {
        if (segmentIndex == lastIndex) {
            val label = toNode.label ?: toNode.id
            return "Arrive at $label"
        }

        if (prevHeading == null || segmentIndex == 0) {
            return "Head forward"
        }

        val turn = normalizeDegrees(currentHeading - prevHeading)
        return when {
            abs(turn) < TURN_THRESHOLD -> "Continue straight"
            turn in TURN_THRESHOLD..135.0 -> "Turn right"
            turn > 135.0 -> "Make a U-turn right"
            turn in -135.0..-TURN_THRESHOLD -> "Turn left"
            turn < -135.0 -> "Make a U-turn left"
            else -> "Continue"
        }
    }

    companion object {
        private const val AVERAGE_WALKING_SPEED = 1.2 // m/s
        private const val TURN_THRESHOLD = 30.0 // degrees

        fun euclideanDistance(a: NavNode, b: NavNode): Double {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        /**
         * Calculate heading from node A to node B in degrees.
         * 0° = +X direction, 90° = +Z direction (plan view).
         */
        fun calculateHeading(from: NavNode, to: NavNode): Double {
            val dx = to.x - from.x
            val dz = to.z - from.z
            val radians = atan2(dz, dx)
            return radians * 180.0 / kotlin.math.PI
        }

        /**
         * Normalize angle to [-180, 180) range.
         */
        fun normalizeDegrees(degrees: Double): Double {
            var d = degrees % 360.0
            if (d >= 180.0) d -= 360.0
            if (d < -180.0) d += 360.0
            return d
        }
    }
}
