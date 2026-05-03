package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.model.AuthoringConfig

/**
 * Validates the navigation graph for structural integrity.
 *
 * Checks:
 * - No duplicate node/edge IDs
 * - All edge endpoints reference existing nodes
 * - All room destination nodes exist
 * - All entrance marker start nodes exist
 * - All edge costs are positive
 * - Graph connectivity (every room reachable from every entrance)
 */
class GraphValidator {

    data class ValidationResult(
        val errors: List<String>,
        val warnings: List<String>,
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    fun validate(config: AuthoringConfig): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val nodeIds = config.nodes.map { it.id }.toSet()

        // ── Duplicate IDs ───────────────────────────────────
        val duplicateNodes = config.nodes.groupBy { it.id }.filter { it.value.size > 1 }.keys
        if (duplicateNodes.isNotEmpty()) {
            errors.add("Duplicate node IDs: $duplicateNodes")
        }

        val duplicateEdges = config.edges.groupBy { it.id }.filter { it.value.size > 1 }.keys
        if (duplicateEdges.isNotEmpty()) {
            errors.add("Duplicate edge IDs: $duplicateEdges")
        }

        val duplicateRooms = config.rooms.groupBy { it.id }.filter { it.value.size > 1 }.keys
        if (duplicateRooms.isNotEmpty()) {
            errors.add("Duplicate room IDs: $duplicateRooms")
        }

        // ── Edge endpoint references ────────────────────────
        for (edge in config.edges) {
            if (edge.from !in nodeIds) {
                errors.add("Edge '${edge.id}': 'from' node '${edge.from}' does not exist")
            }
            if (edge.to !in nodeIds) {
                errors.add("Edge '${edge.id}': 'to' node '${edge.to}' does not exist")
            }
            if (edge.cost <= 0) {
                errors.add("Edge '${edge.id}': cost must be positive, got ${edge.cost}")
            }
            if (edge.from == edge.to) {
                errors.add("Edge '${edge.id}': self-loop (from == to == '${edge.from}')")
            }
        }

        // ── Room destination node references ────────────────
        for (room in config.rooms) {
            if (room.destinationNodeId !in nodeIds) {
                errors.add("Room '${room.id}': destinationNodeId '${room.destinationNodeId}' does not exist")
            }
        }

        // ── Entrance marker start node references ───────────
        for (marker in config.entranceMarkers) {
            if (marker.startNodeId !in nodeIds) {
                errors.add("Marker '${marker.id}': startNodeId '${marker.startNodeId}' does not exist")
            }
        }

        // ── Entrance marker ID uniqueness ────────────────────
        val entranceMarkerIds = config.entranceMarkers.map { it.id }
        val duplicateEntranceMarkers = entranceMarkerIds.groupBy { it }.filter { it.value.size > 1 }.keys
        if (duplicateEntranceMarkers.isNotEmpty()) {
            errors.add("Duplicate entrance marker IDs: $duplicateEntranceMarkers")
        }

        // ── Checkpoint marker validation ─────────────────────
        if (config.checkpointMarkers.isNotEmpty()) {
            val checkpointIds = config.checkpointMarkers.map { it.id }

            // Unique checkpoint marker IDs
            val duplicateCheckpoints = checkpointIds.groupBy { it }.filter { it.value.size > 1 }.keys
            if (duplicateCheckpoints.isNotEmpty()) {
                errors.add("Duplicate checkpoint marker IDs: $duplicateCheckpoints")
            }

            // No overlap between entrance and checkpoint marker IDs
            val overlappingIds = entranceMarkerIds.toSet().intersect(checkpointIds.toSet())
            if (overlappingIds.isNotEmpty()) {
                errors.add("Marker IDs overlap between entrance and checkpoint: $overlappingIds")
            }

            // Checkpoint marker nearestNodeId references
            for (cp in config.checkpointMarkers) {
                if (cp.nearestNodeId !in nodeIds) {
                    errors.add("Checkpoint marker '${cp.id}': nearestNodeId '${cp.nearestNodeId}' does not exist")
                }
            }
        }

        // ── Connectivity check ──────────────────────────────
        if (errors.isEmpty()) {
            val adjacency = buildAdjacency(config)
            val startNodeIds = config.entranceMarkers.map { it.startNodeId }.toSet()
            val roomNodeIds = config.rooms.map { it.destinationNodeId }.toSet()

            for (startId in startNodeIds) {
                val reachable = bfs(startId, adjacency)
                val unreachable = roomNodeIds - reachable
                if (unreachable.isNotEmpty()) {
                    val unreachableRooms = config.rooms.filter { it.destinationNodeId in unreachable }
                        .map { "'${it.displayName}' (${it.destinationNodeId})" }
                    errors.add(
                        "From entrance node '$startId', cannot reach rooms: ${unreachableRooms.joinToString()}"
                    )
                }
            }
        }

        // ── Warnings ────────────────────────────────────────
        val connectedNodes = if (config.edges.isNotEmpty()) {
            val adj = buildAdjacency(config)
            bfs(config.nodes.first().id, adj)
        } else emptySet()

        val orphanNodes = nodeIds - connectedNodes
        if (orphanNodes.isNotEmpty()) {
            warnings.add("Orphan nodes (not connected to any edge): $orphanNodes")
        }

        return ValidationResult(errors, warnings)
    }

    private fun buildAdjacency(config: AuthoringConfig): Map<String, Set<String>> {
        val adj = mutableMapOf<String, MutableSet<String>>()
        for (edge in config.edges) {
            adj.getOrPut(edge.from) { mutableSetOf() }.add(edge.to)
            if (edge.bidirectional) {
                adj.getOrPut(edge.to) { mutableSetOf() }.add(edge.from)
            }
        }
        return adj
    }

    private fun bfs(start: String, adjacency: Map<String, Set<String>>): Set<String> {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(start)
        visited.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in adjacency[current] ?: emptySet()) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return visited
    }
}
