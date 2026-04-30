package com.example.vecturai.graph

import com.google.ar.core.Pose
import kotlinx.serialization.Serializable

@Serializable
data class MapGraph(
    val buildingName: String,
    val createdAtEpochMs: Long,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>
) {
    val labeledNodes: List<MapNode>
        get() = nodes.filter { !it.label.isNullOrBlank() }
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
data class MapEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float
)
