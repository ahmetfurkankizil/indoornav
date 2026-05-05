package com.VecturAI.core.domain

import kotlinx.serialization.Serializable

/**
 * An entrance marker used for AR world alignment.
 *
 * Coordinates use the building-local coordinate system (meters, Y-up).
 *
 * When a user scans this marker, the AR system:
 * 1. Decodes the QR payload to identify the building + marker
 * 2. Detects the marker image to get 6-DoF pose in device coords
 * 3. Uses the known marker pose in building coords to compute the transform
 *
 * @property id Unique marker identifier
 * @property qrPayload QR code content (typically "VecturAI://building/{buildingId}/marker/{id}")
 * @property positionX Marker X in building-local coords (meters)
 * @property positionY Marker Y in building-local coords (meters)
 * @property positionZ Marker Z in building-local coords (meters)
 * @property rotationYDegrees Marker rotation around Y-axis in degrees
 * @property forwardBasis Semantic facing direction of the marker ("+x", "-x", "+z", "-z")
 * @property nearestNodeId Graph node closest to this marker (start of navigation)
 * @property physicalWidthMeters Physical printed width for AR scale detection
 * @property physicalHeightMeters Physical printed height
 * @property referenceImageName Name of the AR reference image asset
 */
@Serializable
data class EntranceMarker(
    val id: String,
    val qrPayload: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double = 0.0,
    val rotationYDegrees: Double = 0.0,
    val forwardBasis: String = "-z",
    val nearestNodeId: String,
    val physicalWidthMeters: Double = 0.2,
    val physicalHeightMeters: Double = 0.2,
    val referenceImageName: String? = null,
)
