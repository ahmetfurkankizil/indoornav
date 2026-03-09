package com.vecturai.tools.preprocessor.model

import kotlinx.serialization.Serializable

/**
 * Human-authored building annotation config — input to the preprocessor.
 *
 * All coordinates use the building-local coordinate system:
 * - meters, Y-up, right-handed
 * - origin chosen by the author
 */
@Serializable
data class AuthoringConfig(
    val buildingId: String,
    val buildingName: String,
    val floorId: String = "ground",
    val asset: AssetReference,
    val tags: List<String> = emptyList(),
    val entranceMarkers: List<AuthoringMarker>,
    val checkpointMarkers: List<AuthoringCheckpointMarker> = emptyList(),
    val nodes: List<AuthoringNode>,
    val edges: List<AuthoringEdge>,
    val rooms: List<AuthoringRoom>,
    val routeRendering: AuthoringRouteRendering = AuthoringRouteRendering(),
    val graphMetadata: GraphMetadata? = null,
)

@Serializable
data class AssetReference(
    val glbFile: String,
    val sourceApp: String = "polycam",
    val scanDate: String? = null,
)

@Serializable
data class AuthoringMarker(
    val id: String,
    val startNodeId: String,
    val physicalWidthMeters: Double,
    val physicalHeightMeters: Double,
    val position: Position3D,
    val forwardBasis: String = "-z",
    val rotationYDegrees: Double = 0.0,
    val referenceImageName: String? = null,
    val notes: String? = null,
)

/**
 * Checkpoint marker for mid-route alignment correction.
 *
 * Similar to entrance markers but with a correction role.
 * Not required — packages work without them.
 */
@Serializable
data class AuthoringCheckpointMarker(
    val id: String,
    val nearestNodeId: String,
    val physicalWidthMeters: Double,
    val physicalHeightMeters: Double,
    val position: Position3D,
    val rotationYDegrees: Double = 0.0,
    val referenceImageName: String? = null,
    val notes: String? = null,
)

@Serializable
data class Position3D(
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
)

@Serializable
data class AuthoringNode(
    val id: String,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val type: String = "waypoint",
    val label: String? = null,
)

@Serializable
data class AuthoringEdge(
    val id: String,
    val from: String,
    val to: String,
    val cost: Double,
    val bidirectional: Boolean = true,
    val label: String? = null,
)

@Serializable
data class AuthoringRoom(
    val id: String,
    val displayName: String,
    val destinationNodeId: String,
    val category: String? = null,
    val keywords: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val description: String? = null,
)

@Serializable
data class AuthoringRouteRendering(
    val arrowSpacingMeters: Double = 1.5,
    val lookaheadDistanceMeters: Double = 8.0,
    val destinationThresholdMeters: Double = 1.5,
    val turnMarkerThresholdDegrees: Double = 30.0,
    val arrowHeightOffsetMeters: Double = 0.05,
)

@Serializable
data class GraphMetadata(
    val authorName: String? = null,
    val authoredDate: String? = null,
    val notes: String? = null,
)

