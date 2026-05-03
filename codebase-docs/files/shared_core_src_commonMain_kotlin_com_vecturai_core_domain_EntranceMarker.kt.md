# File Dossier: EntranceMarker.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\domain\EntranceMarker.kt`

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
 * @property qrPayload QR code content (typically "Vectura AI://building/{buildingId}/marker/{id}")
 * @property positionX Marker X in building-local coords (meters)
 * @property positionY Marker Y in building-local coords (meters)
 * @property positionZ Marker Z in building-local coords (meters)
 * @property rotationYDegrees Marker rotation around Y-axis in degrees
 * @property forwardBasis Semantic facing direction of the marker ("+x", "-x", "+z", "-z")
 * @property nearestNodeId Graph 
```

## Status
Mapped (Pass 3 Normalization)
