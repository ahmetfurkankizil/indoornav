# File Dossier: RouteRenderingConfig.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\domain\RouteRenderingConfig.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.domain

import kotlinx.serialization.Serializable

/**
 * Configuration for how navigation routes are rendered in AR and 2D preview.
 *
 * Loaded from the building package `route_rendering.json`.
 */
@Serializable
data class RouteRenderingConfig(
    val arrowSpacingMeters: Double = 1.5,
    val lookaheadDistanceMeters: Double = 8.0,
    val destinationThresholdMeters: Double = 1.5,
    val turnMarkerThresholdDegrees: Double = 30.0,
    val arrowHeightOffsetMeters: Double = 0.05,
    val schemaVersion: Int = 1,
)

```

## Status
Mapped (Pass 3 Normalization)
