package com.example.vecturai.graph

import java.util.PriorityQueue

class Pathfinder(private val graph: MapGraph) {
    private val nodeById = graph.nodes.associateBy { it.id }
    private val adjacentEdges: Map<String, List<MapEdge>> =
        graph.edges
            .flatMap { edge ->
                listOf(
                    edge,
                    MapEdge(edge.toNodeId, edge.fromNodeId, edge.distanceMeters)
                )
            }
            .groupBy { it.fromNodeId }

    fun shortestPath(startId: String, goalId: String): List<MapNode>? {
        if (startId !in nodeById || goalId !in nodeById) return null
        if (startId == goalId) return listOfNotNull(nodeById[startId])

        val distance = mutableMapOf(startId to 0f)
        val previous = mutableMapOf<String, String>()
        val queue = PriorityQueue(compareBy<Pair<String, Float>> { it.second })
        queue.add(startId to 0f)

        while (queue.isNotEmpty()) {
            val (nodeId, currentDistance) = queue.poll() ?: break
            if (nodeId == goalId) break
            if (currentDistance > (distance[nodeId] ?: Float.MAX_VALUE)) continue

            for (edge in adjacentEdges[nodeId].orEmpty()) {
                val newDistance = currentDistance + edge.distanceMeters
                if (newDistance < (distance[edge.toNodeId] ?: Float.MAX_VALUE)) {
                    distance[edge.toNodeId] = newDistance
                    previous[edge.toNodeId] = nodeId
                    queue.add(edge.toNodeId to newDistance)
                }
            }
        }

        if (goalId !in previous) return null
        val nodeIds = generateSequence(goalId) { previous[it] }.toList().reversed()
        return nodeIds.mapNotNull { nodeById[it] }
    }
}
