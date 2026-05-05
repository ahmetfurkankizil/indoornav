# File Dossier: PoseModels.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\ar\PoseModels.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.ar

import kotlinx.serialization.Serializable

/**
 * Platform-neutral camera pose from AR runtime.
 *
 * Sent periodically (~2 Hz) from native AR layer to shared code.
 * Coordinates are in the AR world frame (meters, Y-up).
 */
@Serializable
data class CameraPose(
    val x: Double,
    val y: Double,
    val z: Double,
    val rotationYDeg: Double = 0.0,
    val timestampMs: Long = 0L,
)

/**
 * Tracking quality reported by the AR runtime.
 */
@Serializable
enum class TrackingQuality {
    /** Normal tracking — full confidence. */
    NORMAL,
    /** Limited tracking — some drift expected. */
    LIMITED,
    /** Tracking not available — session paused or initializing. */
    NOT_AVAILABLE,
}

/**
 * Progress update event from native → shared.
 *
 * Bundles camera pose with tracking quality for the
 * shared ProgressEstimator to consume.
 */
@Serializable
data class PoseUpdateEvent(
    val cameraPose: CameraPose,
    val trackingQuality: TrackingQuality = 
```

## Status
Mapped (Pass 3 Normalization)
