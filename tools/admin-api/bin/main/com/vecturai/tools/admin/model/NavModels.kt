package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

// ── Nodes ──────────────────────────────────────────────────────────────

@Serializable
data class NodeResponse(
    val id: String,
    val floorId: String,
    val buildingId: String,
    val label: String,
    val nodeType: String,
    val canvasX: Double,
    val canvasY: Double,
    val worldX: Double? = null,
    val worldY: Double? = null,
    val worldZ: Double? = null,
    val metadata: String = "{}",
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateNodeRequest(
    val label: String,
    val nodeType: String = "room",
    val canvasX: Double,
    val canvasY: Double,
    val metadata: String = "{}",
)

@Serializable
data class UpdateNodeRequest(
    val label: String? = null,
    val nodeType: String? = null,
    val canvasX: Double? = null,
    val canvasY: Double? = null,
    val metadata: String? = null,
)

// ── Edges ──────────────────────────────────────────────────────────────

@Serializable
data class Waypoint(val x: Double, val y: Double)

@Serializable
data class EdgeResponse(
    val id: String,
    val floorId: String,
    val buildingId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val isBidirectional: Boolean,
    val distance: Double? = null,
    val edgeType: String,
    val waypoints: List<Waypoint>,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CreateEdgeRequest(
    val fromNodeId: String,
    val toNodeId: String,
    val edgeType: String = "walk",
    val isBidirectional: Boolean = true,
    val waypoints: List<Waypoint> = emptyList(),
    val createdBy: String = "human",
)

@Serializable
data class UpdateEdgeRequest(
    val edgeType: String? = null,
    val isBidirectional: Boolean? = null,
    val waypoints: List<Waypoint>? = null,
)

// ── AI edge suggestion ─────────────────────────────────────────────────

@Serializable
data class AiSuggestRequest(
    val floorPlanImageBase64: String,
    val nodes: List<AiNodeInput>,
)

@Serializable
data class AiNodeInput(
    val id: String,
    val label: String,
    val nodeType: String,
    val canvasX: Double,
    val canvasY: Double,
)

@Serializable
data class SuggestedEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val waypoints: List<Waypoint> = emptyList(),
    val confidence: Double = 1.0,
)

@Serializable
data class AiSuggestResponse(
    val nodes: List<AiNodeInput> = emptyList(),
    val edges: List<SuggestedEdge> = emptyList()
)

// ── Floor connections ──────────────────────────────────────────────────

@Serializable
data class FloorConnectionResponse(
    val id: String,
    val buildingId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val connectionType: String,
    val isBidirectional: Boolean,
    val createdAt: String,
)

@Serializable
data class CreateFloorConnectionRequest(
    val fromNodeId: String,
    val toNodeId: String,
    val connectionType: String,
    val isBidirectional: Boolean = true,
)

// ── DB Admin ───────────────────────────────────────────────────────────

@Serializable
data class NavPackageListItem(
    val id: String,
    val buildingId: String,
    val buildingName: String,
    val version: Int,
    val checksum: String,
    val isCurrent: Boolean,
    val generatedAt: String,
)

@Serializable
data class SystemStats(
    val managerCount: Int,
    val buildingCount: Int,
    val floorCount: Int,
    val nodeCount: Int,
    val edgeCount: Int,
    val navPackageCount: Int,
)
