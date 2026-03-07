package com.vecturai.core.domain

import kotlinx.serialization.Serializable

/**
 * An entrance marker used for AR world alignment.
 *
 * Each entrance marker is a physical marker placed at a known location
 * in the building. It contains both a QR code (for payload decoding) and
 * a visual reference image (for ARKit/ARCore image detection).
 *
 * When a user scans this marker, the AR system establishes the relationship
 * between the physical world coordinate system and the navigation graph
 * coordinate system.
 *
 * @property id Unique marker identifier
 * @property qrPayload The data encoded in the QR code (e.g., building ID, marker ID)
 * @property positionX Marker position X in the nav-graph coordinate system
 * @property positionY Marker position Y in the nav-graph coordinate system
 * @property positionZ Marker position Z in the nav-graph coordinate system
 * @property rotationYDegrees Marker rotation around Y-axis in degrees
 * @property nearestNodeId The closest NavNode to this marker's position
 * @property physicalWidthMeters Physical width of the marker for AR scale detection
 * @property referenceImageName Name of the reference image asset for AR detection
 */
@Serializable
data class EntranceMarker(
    val id: String,
    val qrPayload: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double = 0.0,
    val rotationYDegrees: Double = 0.0,
    val nearestNodeId: String,
    val physicalWidthMeters: Double = 0.2,
    val referenceImageName: String? = null,
)
