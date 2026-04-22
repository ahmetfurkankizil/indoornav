# File Dossier: MarkerAlignmentResult.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\ar\MarkerAlignmentResult.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.ar

import kotlinx.serialization.Serializable

/**
 * Result of detecting and aligning to an entrance marker.
 *
 * Created by the native AR layer when a marker is detected, then
 * passed to [ArNavigationCoordinator] to establish alignment.
 *
 * @property markerId The entrance marker ID from the building package
 * @property entranceNodeId The nearest graph node to this marker
 * @property markerBuildingX Marker X in building-local coords (from package)
 * @property markerBuildingY Marker Y in building-local coords
 * @property markerBuildingZ Marker Z in building-local coords
 * @property markerArX Detected marker X in AR world coords
 * @property markerArY Detected marker Y in AR world coords
 * @property markerArZ Detected marker Z in AR world coords
 * @property markerArRotationYDeg Detected marker Y-rotation in AR world (degrees)
 * @property markerBuildingRotationYDeg Marker Y-rotation in building coords (degrees)
 * @property confidence Detection conf
```

## Status
Mapped (Pass 3 Normalization)
