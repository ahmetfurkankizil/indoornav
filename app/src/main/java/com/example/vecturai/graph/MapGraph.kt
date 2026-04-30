package com.example.vecturai.graph

import com.google.ar.core.Pose
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.sqrt

@Serializable
data class MapGraph(
    val buildingName: String,
    val createdAtEpochMs: Long,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>
) {
    val labeledNodes: List<MapNode>
        get() = nodes.filter { !it.label.isNullOrBlank() }

    @Transient
    private var cachedNodeById: Map<String, MapNode>? = null

    @Transient
    private var cachedAdjacency: Map<String, List<MapEdge>>? = null

    fun nodeById(): Map<String, MapNode> {
        cachedNodeById?.let { return it }
        return nodes.associateBy { it.id }.also { cachedNodeById = it }
    }

    fun adjacency(): Map<String, List<MapEdge>> {
        cachedAdjacency?.let { return it }
        val adjacency = edges
            .flatMap { edge ->
                if (edge.bidirectional) {
                    listOf(
                        edge,
                        edge.copy(
                            fromNodeId = edge.toNodeId,
                            toNodeId = edge.fromNodeId,
                            bidirectional = false
                        )
                    )
                } else {
                    listOf(edge)
                }
            }
            .groupBy { it.fromNodeId }
        cachedAdjacency = adjacency
        return adjacency
    }

    fun withRefreshedWeights(): MapGraph {
        val byId = nodeById()
        val refreshedEdges = edges.map { edge ->
            val from = byId[edge.fromNodeId] ?: return@map edge
            val to = byId[edge.toNodeId] ?: return@map edge
            edge.copy(distanceMeters = euclideanDistance(from, to))
        }
        return copy(edges = refreshedEdges)
    }

    private fun euclideanDistance(a: MapNode, b: MapNode): Float {
        val dx = a.xMeters - b.xMeters
        val dy = a.yMeters - b.yMeters
        val dz = a.zMeters - b.zMeters
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

@Serializable
data class MapNode(
    val id: String,
    val cloudAnchorId: String,
    val label: String? = null,
    val xMeters: Float,
    val yMeters: Float,
    val zMeters: Float,
    val floor: Int = 0,
    val qx: Float = 0f,
    val qy: Float = 0f,
    val qz: Float = 0f,
    val qw: Float = 1f
) {
    fun graphPose(): Pose = Pose(
        floatArrayOf(xMeters, yMeters, zMeters),
        floatArrayOf(qx, qy, qz, qw)
    )
}

@Serializable
enum class EdgeKind {
    CORRIDOR,
    STAIRS,
    ELEVATOR
}

@Serializable
data class MapEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float,
    val kind: EdgeKind = EdgeKind.CORRIDOR,
    val bidirectional: Boolean = true
)
