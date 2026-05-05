# File Dossier: CorrectionEvent.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\ar\CorrectionEvent.kt`

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
    val confidence: Doubl
```

## Status
Mapped (Pass 3 Normalization)
