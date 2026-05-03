package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

@Serializable
data class DraftSummary(
    val jobId: String,
    val status: JobStatus,
    val buildingId: String? = null,
    val buildingName: String? = null,
    val floorId: String? = null,
    val artifactAvailability: ArtifactAvailability,
    val counts: GraphCounts,
    val rooms: List<DraftRoom> = emptyList(),
    val generationMetadata: GenerationMetadataSummary? = null,
    val geometryStats: GeometryStatsSummary? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class ArtifactAvailability(
    val hasOccupancyPreview: Boolean = false,
    val hasGraphPreview: Boolean = false,
    val hasAuthoringConfig: Boolean = false,
    val hasGenerationMetadata: Boolean = false,
    val hasGeometryStats: Boolean = false,
)

@Serializable
data class GraphCounts(
    val nodes: Int = 0,
    val edges: Int = 0,
    val rooms: Int = 0,
    val entranceMarkers: Int = 0,
    val checkpointMarkers: Int = 0,
)

@Serializable
data class DraftRoom(
    val id: String,
    val displayName: String,
    val category: String? = null,
    val destinationNodeId: String? = null,
    val description: String? = null,
)

@Serializable
data class GenerationMetadataSummary(
    val generatedBy: String? = null,
    val timestamp: String? = null,
    val confidence: String? = null,
    val editRequired: Boolean? = null,
    val floorY: Double? = null,
    val floorConfidence: Double? = null,
)

@Serializable
data class GeometryStatsSummary(
    val totalVertices: Int? = null,
    val meshCount: Int? = null,
    val extentX: Double? = null,
    val extentY: Double? = null,
    val extentZ: Double? = null,
    val occupancyGridWidth: Int? = null,
    val occupancyGridHeight: Int? = null,
    val occupancyGridCellSize: Double? = null,
    val zoneCount: Int? = null,
)
