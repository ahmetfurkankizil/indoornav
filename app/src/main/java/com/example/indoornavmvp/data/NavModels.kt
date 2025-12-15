package com.example.indoornavmvp.data

/**
 * Type-safe wrapper for node identifiers.
 */
@JvmInline
value class NodeId(val value: String)

/**
 * Represents a navigation node in the graph.
 */
data class NavNode(
    val id: NodeId,
    val label: String
)

/**
 * Represents a directed edge between two nodes with a traversal cost.
 */
data class Edge(
    val from: NodeId,
    val to: NodeId,
    val cost: Float
)

/**
 * Represents a room/destination with its associated door node.
 */
data class Room(
    val id: String,
    val name: String,
    val doorNode: NodeId
)

/**
 * The complete navigation graph containing nodes, edges, and rooms.
 */
data class NavGraph(
    val nodes: List<NavNode>,
    val edges: List<Edge>,
    val rooms: List<Room>
)

