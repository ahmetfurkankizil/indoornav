package com.example.indoornavmvp.data

import java.util.PriorityQueue

/**
 * Pathfinding utility using Dijkstra's algorithm.
 */
object PathFinder {

    /**
     * Find the shortest path between two nodes in the navigation graph.
     * 
     * @param graph The navigation graph containing nodes and edges
     * @param start The starting node ID
     * @param goal The destination node ID
     * @return List of NodeIds representing the path, or empty list if no path exists
     */
    fun shortestPath(graph: NavGraph, start: NodeId, goal: NodeId): List<NodeId> {
        // Early exit if start equals goal
        if (start == goal) return listOf(start)

        // Build adjacency list from edges
        val neighbors = graph.edges.groupBy { it.from }

        // Distance map with default infinity
        val dist = mutableMapOf<NodeId, Float>()
        graph.nodes.forEach { dist[it.id] = Float.POSITIVE_INFINITY }
        dist[start] = 0f

        // Previous node map for path reconstruction
        val prev = mutableMapOf<NodeId, NodeId?>()

        // Priority queue: (nodeId, distance)
        val queue = PriorityQueue<Pair<NodeId, Float>>(compareBy { it.second })
        queue.add(start to 0f)

        // Set of all valid node IDs
        val allNodeIds = graph.nodes.map { it.id }.toSet()

        // Visited set to avoid reprocessing
        val visited = mutableSetOf<NodeId>()

        while (queue.isNotEmpty()) {
            val (currentNode, currentDist) = queue.poll()

            // Skip if already visited
            if (currentNode in visited) continue
            visited.add(currentNode)

            // Found the goal
            if (currentNode == goal) break

            // Skip if this distance is outdated
            if (currentDist > (dist[currentNode] ?: Float.POSITIVE_INFINITY)) continue

            // Explore neighbors
            val edgesFromCurrent = neighbors[currentNode] ?: emptyList()
            for (edge in edgesFromCurrent) {
                val neighborNode = edge.to
                
                // Skip if neighbor is not a valid node
                if (neighborNode !in allNodeIds) continue

                val newDist = currentDist + edge.cost
                val currentNeighborDist = dist[neighborNode] ?: Float.POSITIVE_INFINITY

                if (newDist < currentNeighborDist) {
                    dist[neighborNode] = newDist
                    prev[neighborNode] = currentNode
                    queue.add(neighborNode to newDist)
                }
            }
        }

        // Reconstruct path
        if (goal !in prev && start != goal) {
            return emptyList() // No path found
        }

        val path = mutableListOf<NodeId>()
        var current: NodeId? = goal
        
        while (current != null) {
            path.add(current)
            current = prev[current]
        }

        // Check if we actually reached the start
        if (path.isEmpty() || path.last() != start) {
            return emptyList()
        }

        path.reverse()
        return path
    }
}

