# File Dossier: RouteSegment.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\domain\RouteSegment.kt`

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
 * A single segment of a computed navigation route.
 *
 * A complete route from origin to destination is composed of an ordered
 * list of [RouteSegment]s. Each segment represents movement from one
 * nav node to the next, along with the instruction to display.
 *
 * @property fromNodeId Starting node of this segment
 * @property toNodeId Ending node of this segment
 * @property distanceMeters Distance of this segment in meters
 * @property instruction Human-readable instruction (e.g., "Turn left", "Continue straight")
 * @property headingDegrees Direction of travel in degrees (0 = north in nav-graph space)
 */
@Serializable
data class RouteSegment(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val instruction: String = "",
    val headingDegrees: Double = 0.0,
)

/**
 * A computed route from origin to destination within the building.
 *
 * @property originNodeId
```

## Status
Mapped (Pass 3 Normalization)
