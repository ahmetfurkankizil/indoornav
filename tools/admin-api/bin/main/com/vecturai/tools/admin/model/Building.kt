package com.vecturai.tools.admin.model

import kotlinx.serialization.Serializable

@Serializable
data class BuildingResponse(
    val id: String,
    val managerId: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val qrToken: String,
    val widthMeters: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val floors: List<FloorResponse>? = null,
)

@Serializable
data class CreateBuildingRequest(
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val widthMeters: Double = 25.0,
)

@Serializable
data class UpdateBuildingRequest(
    val name: String? = null,
    val description: String? = null,
    val address: String? = null,
    val widthMeters: Double? = null,
)

@Serializable
data class FloorResponse(
    val id: String,
    val buildingId: String,
    val floorNumber: Int,
    val floorName: String,
    val boundsMinX: Double? = null,
    val boundsMaxX: Double? = null,
    val boundsMinZ: Double? = null,
    val boundsMaxZ: Double? = null,
    val floorY: Double? = null,
    val uploadStatus: String,
    val mapFileType: String = "glb",
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class FloorBoundsRequest(
    val minX: Double,
    val maxX: Double,
    val minZ: Double,
    val maxZ: Double,
    val floorY: Double,
)

@Serializable
data class EntranceMarkerResponse(
    val id: String,
    val buildingId: String,
    val displayName: String? = null,
    val startNodeId: String,
    val physicalWidthMeters: Double,
    val physicalHeightMeters: Double,
    val worldX: Double? = null,
    val worldY: Double? = null,
    val worldZ: Double? = null,
    val forwardBasis: String,
    val rotationYDegrees: Double? = null,
    val referenceImageName: String? = null,
)

@Serializable
data class CreateEntranceMarkerRequest(
    val displayName: String? = null,
    val startNodeId: String,
    val physicalWidthMeters: Double = 0.21,
    val physicalHeightMeters: Double = 0.21,
    val worldX: Double? = null,
    val worldY: Double? = null,
    val worldZ: Double? = null,
    val forwardBasis: String = "+Z",
    val rotationYDegrees: Double? = null,
    val referenceImageName: String? = null,
)
