package com.example.vecturai.graph

import java.util.PriorityQueue
import kotlin.math.acos
import kotlin.math.sqrt

class Pathfinder(private val graph: MapGraph) {
    private val nodeById = graph.nodeById()
    private val adjacentEdges = graph.adjacency()

    fun shortestPath(startId: String, goalId: String): List<MapNode>? {
        val start = nodeById[startId] ?: return null
        val goal = nodeById[goalId] ?: return null
        if (startId == goalId) return listOf(start)

        val startState = SearchState(nodeId = startId, incomingFromId = null)
        val gScore = mutableMapOf(startState to 0f)
        val previous = mutableMapOf<SearchState, SearchState>()
        val queue = PriorityQueue(
            compareBy<QueueEntry>(
                { it.estimatedTotal },
                { it.heuristic },
                { it.state.nodeId },
                { it.state.incomingFromId.orEmpty() }
            )
        )
        queue.add(
            QueueEntry(
                state = startState,
                cost = 0f,
                heuristic = euclidean(start, goal)
            )
        )

        var bestGoalState: SearchState? = null
        while (queue.isNotEmpty()) {
            val entry = queue.poll() ?: break
            val state = entry.state
            if (entry.cost > (gScore[state] ?: Float.MAX_VALUE)) continue
            if (state.nodeId == goalId) {
                bestGoalState = state
                break
            }

            val here = nodeById[state.nodeId] ?: continue
            for (edge in adjacentEdges[state.nodeId].orEmpty()) {
                val next = nodeById[edge.toNodeId] ?: continue
                val nextState = SearchState(
                    nodeId = edge.toNodeId,
                    incomingFromId = state.nodeId
                )
                val tentative = entry.cost + edgeCost(
                    previousNode = state.incomingFromId?.let { nodeById[it] },
                    here = here,
                    next = next,
                    edge = edge
                )
                if (tentative < (gScore[nextState] ?: Float.MAX_VALUE)) {
                    gScore[nextState] = tentative
                    previous[nextState] = state
                    val heuristic = euclidean(next, goal)
                    queue.add(
                        QueueEntry(
                            state = nextState,
                            cost = tentative,
                            heuristic = heuristic
                        )
                    )
                }
            }
        }

        val goalState = bestGoalState ?: return null
        val nodeIds = generateSequence(goalState) { previous[it] }
            .toList()
            .asReversed()
            .map { it.nodeId }
        return smoothPath(nodeIds.mapNotNull { nodeById[it] })
    }

    private fun edgeCost(
        previousNode: MapNode?,
        here: MapNode,
        next: MapNode,
        edge: MapEdge
    ): Float {
        val floorCost = when (edge.kind) {
            EdgeKind.CORRIDOR -> edge.distanceMeters
            EdgeKind.STAIRS -> edge.distanceMeters * STAIRS_MULTIPLIER
            EdgeKind.ELEVATOR -> edge.distanceMeters * ELEVATOR_MULTIPLIER
        }
        if (previousNode == null) return floorCost

        val incoming = (here.point() - previousNode.point()).horizontal().normalized()
        val outgoing = (next.point() - here.point()).horizontal().normalized()
        val cosAngle = incoming.dot(outgoing).coerceIn(-1f, 1f)
        return floorCost + TURN_COST_METERS_PER_RADIAN * acos(cosAngle)
    }

    private fun smoothPath(path: List<MapNode>): List<MapNode> {
        if (path.size < 3) return path

        var current = path
        var changed: Boolean
        do {
            changed = false
            val smoothed = mutableListOf<MapNode>()
            smoothed += current.first()
            var index = 1
            while (index < current.lastIndex) {
                val a = smoothed.last()
                val b = current[index]
                val c = current[index + 1]
                if (canDropIntermediate(a, b, c)) {
                    changed = true
                } else {
                    smoothed += b
                }
                index++
            }
            smoothed += current.last()
            current = smoothed
        } while (changed && current.size >= 3)

        return current
    }

    private fun canDropIntermediate(a: MapNode, b: MapNode, c: MapNode): Boolean {
        if (!b.label.isNullOrBlank()) return false
        if (a.floor != b.floor || b.floor != c.floor) return false
        val ac = c.point() - a.point()
        if (ac.length() < 1e-3f) return false
        if (!hasDirectEdge(a.id, c.id)) return false
        return distancePointToSegment(b.point(), a.point(), c.point()) <= SMOOTHING_MAX_DEVIATION_M
    }

    private fun hasDirectEdge(fromId: String, toId: String): Boolean =
        adjacentEdges[fromId].orEmpty().any { it.toNodeId == toId }

    private fun distancePointToSegment(point: GraphPoint, start: GraphPoint, end: GraphPoint): Float {
        val segment = end - start
        val len2 = segment.dot(segment)
        if (len2 < 1e-6f) return (point - start).length()
        val t = ((point - start).dot(segment) / len2).coerceIn(0f, 1f)
        val projection = start + segment * t
        return (point - projection).length()
    }

    private fun euclidean(a: MapNode, b: MapNode): Float = (a.point() - b.point()).length()

    private fun MapNode.point(): GraphPoint = GraphPoint(xMeters, yMeters, zMeters)

    private data class SearchState(
        val nodeId: String,
        val incomingFromId: String?
    )

    private data class QueueEntry(
        val state: SearchState,
        val cost: Float,
        val heuristic: Float
    ) {
        val estimatedTotal: Float = cost + heuristic
    }

    private data class GraphPoint(
        val x: Float,
        val y: Float,
        val z: Float
    ) {
        operator fun plus(other: GraphPoint) = GraphPoint(x + other.x, y + other.y, z + other.z)
        operator fun minus(other: GraphPoint) = GraphPoint(x - other.x, y - other.y, z - other.z)
        operator fun times(scale: Float) = GraphPoint(x * scale, y * scale, z * scale)

        fun horizontal() = GraphPoint(x, 0f, z)
        fun dot(other: GraphPoint): Float = x * other.x + y * other.y + z * other.z
        fun length(): Float = sqrt(dot(this))
        fun normalized(): GraphPoint {
            val len = length()
            return if (len < 1e-6f) GraphPoint(0f, 0f, -1f) else GraphPoint(x / len, y / len, z / len)
        }
    }

    companion object {
        private const val TURN_COST_METERS_PER_RADIAN = 0.6f
        private const val STAIRS_MULTIPLIER = 1.5f
        private const val ELEVATOR_MULTIPLIER = 1.2f
        private const val SMOOTHING_MAX_DEVIATION_M = 0.75f
    }
}
