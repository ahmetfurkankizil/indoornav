# File Dossier: ArrowPlacement.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\ar\ArrowPlacement.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.ar

import kotlinx.serialization.Serializable

/**
 * Type of arrow placement in the AR route.
 */
@Serializable
enum class ArrowType {
    /** Regular follow-the-path arrow */
    FOLLOW,
    /** Turn-left indicator */
    TURN_LEFT,
    /** Turn-right indicator */
    TURN_RIGHT,
    /** U-turn indicator */
    U_TURN,
    /** Final destination marker */
    DESTINATION,
}

/**
 * A single arrow/marker to be rendered in the AR scene.
 *
 * All coordinates are in **building-local** space (meters, Y-up).
 * The native layer applies [AlignmentTransform] to get AR-world positions.
 *
 * @property id Unique arrow identifier (for entity management)
 * @property positionX Building-local X
 * @property positionY Building-local Y (height above floor)
 * @property positionZ Building-local Z
 * @property forwardDx Forward direction X component (normalized)
 * @property forwardDy Forward direction Y component
 * @property forwardDz Forward direction Z component (normali
```

## Status
Mapped (Pass 3 Normalization)
