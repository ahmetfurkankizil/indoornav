# File Dossier: CheckpointMarker.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\domain\CheckpointMarker.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.domain

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
 * @property physicalHeightMeters Physical pr
```

## Status
Mapped (Pass 3 Normalization)
