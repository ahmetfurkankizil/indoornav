package com.Vectura AI.tools.admin.model

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
