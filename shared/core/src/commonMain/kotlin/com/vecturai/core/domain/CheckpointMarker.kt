package com.vecturai.core.domain

import kotlinx.serialization.Serializable

/**
 * A checkpoint marker used for mid-route alignment correction.
 *
 * Unlike entrance markers, checkpoint markers do NOT initialize the AR session.
 * They provide secondary alignment observations that the [CorrectionCoordinator]
 * uses to apply bounded corrections to the active alignment transform.
 *
 * Coordinates use the building-local coordinate system (meters, Y-up).
 *
 * @property id Unique marker identifier (must not overlap with entrance marker IDs)
 * @property positionX Marker X in building-local coords (meters)
 * @property positionY Marker Y in building-local coords (meters)
 * @property positionZ Marker Z in building-local coords (meters)
 * @property rotationYDegrees Marker rotation around Y-axis in degrees
 * @property nearestNodeId Graph node closest to this marker
 * @property physicalWidthMeters Physical printed width for AR scale detection
 * @property physicalHeightMeters Physical printed height
 * @property referenceImageName Name of the AR reference image asset
 * @property notes Optional placement or operational notes
 */
@Serializable
data class CheckpointMarker(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double = 0.0,
    val rotationYDegrees: Double = 0.0,
    val nearestNodeId: String,
    val physicalWidthMeters: Double = 0.2,
    val physicalHeightMeters: Double = 0.2,
    val referenceImageName: String? = null,
    val notes: String? = null,
)
