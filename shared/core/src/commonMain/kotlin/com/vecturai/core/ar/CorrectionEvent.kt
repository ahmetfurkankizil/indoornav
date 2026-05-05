package com.VecturAI.core.ar

import kotlinx.serialization.Serializable

/**
 * Event emitted by the native AR layer when a marker is observed.
 *
 * Used for both entrance markers (session init) and checkpoint markers
 * (mid-route correction). The [role] field distinguishes them.
 *
 * @property markerId The marker ID from the building package
 * @property role Whether this is an entrance or checkpoint marker
 * @property arX Detected marker X in AR world coords
 * @property arY Detected marker Y in AR world coords
 * @property arZ Detected marker Z in AR world coords
 * @property arRotationYDeg Detected marker Y-rotation in AR world (degrees)
 * @property confidence Detection confidence (0.0 to 1.0)
 * @property timestampMs Event timestamp in milliseconds
 */
@Serializable
data class MarkerObservationEvent(
    val markerId: String,
    val role: MarkerRole,
    val arX: Double,
    val arY: Double,
    val arZ: Double,
    val arRotationYDeg: Double = 0.0,
    val confidence: Double = 1.0,
    val timestampMs: Long = 0L,
)

/**
 * Result of applying a checkpoint-based alignment correction.
 *
 * @property applied Whether the correction was actually applied
 * @property translationDeltaMeters Magnitude of translation correction (meters)
 * @property rotationDeltaDeg Magnitude of rotation correction (degrees)
 * @property newAlignment The updated alignment transform (null if not applied)
 * @property reason Human-readable reason for applying or rejecting
 */
@Serializable
data class CorrectionResult(
    val applied: Boolean,
    val translationDeltaMeters: Double = 0.0,
    val rotationDeltaDeg: Double = 0.0,
    val newAlignment: AlignmentTransform? = null,
    val reason: String = "",
)
