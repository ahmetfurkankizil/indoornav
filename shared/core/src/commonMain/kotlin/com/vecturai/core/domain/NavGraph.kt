package com.vecturai.core.domain

import kotlinx.serialization.Serializable

/**
 * A node in the indoor navigation graph.
 *
 * Each node represents a walkable waypoint within the building.
 * Nodes are connected by [NavEdge]s to form the routing graph.
 *
 * @property id Unique identifier for the node (from preprocessor output)
 * @property x X coordinate in the building's local coordinate system (meters)
 * @property y Y coordinate in the building's local coordinate system (meters)
 * @property z Z coordinate (elevation), always 0.0 for single-floor MVP
 * @property label Optional human-readable label (e.g., "Hallway Junction A")
 * @property roomId If this node is inside a room, the associated room ID
 */
@Serializable
data class NavNode(
    val id: String,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val label: String? = null,
    val roomId: String? = null,
)

/**
 * A directed edge connecting two [NavNode]s in the navigation graph.
 *
 * @property from Source node ID
 * @property to Destination node ID
 * @property weight Edge weight (typically Euclidean distance in meters)
 * @property isAccessible Whether the edge is currently traversable
 */
@Serializable
data class NavEdge(
    val from: String,
    val to: String,
    val weight: Double,
    val isAccessible: Boolean = true,
)

/**
 * The complete indoor navigation graph for a single floor.
 *
 * This is the primary data structure used by the [RouteEngine] to compute
 * shortest paths. It is generated offline by the nav-preprocessor tool
 * and loaded at runtime from the building package.
 *
 * @property buildingId Identifier for the building this graph belongs to
 * @property floorId Floor identifier (single value for MVP)
 * @property nodes All navigable waypoints
 * @property edges All connections between waypoints
 * @property version Schema version for forward compatibility
 */
@Serializable
data class NavGraph(
    val buildingId: String,
    val floorId: String = "ground",
    val nodes: List<NavNode>,
    val edges: List<NavEdge>,
    val version: Int = 1,
) {
    /** Quick lookup of a node by its ID. */
    val nodeMap: Map<String, NavNode> by lazy {
        nodes.associateBy { it.id }
    }

    /** Adjacency list representation for efficient graph traversal. */
    val adjacencyList: Map<String, List<NavEdge>> by lazy {
        edges.groupBy { it.from }
    }
}
