# File Dossier: NavigationConfidence.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\ar\NavigationConfidence.kt`

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
 * Role of a marker in the AR navigation system.
 */
@Serializable
enum class MarkerRole {
    /** Initializes the AR session and establishes world alignment. */
    ENTRANCE,
    /** Provides mid-route alignment correction without restarting the session. */
    CHECKPOINT,
}

/**
 * Alignment confidence — how trustworthy the current building↔AR transform is.
 */
@Serializable
enum class AlignmentConfidence {
    /** Fresh marker alignment, minimal drift expected. */
    HIGH,
    /** Some time since last alignment; moderate drift possible. */
    MODERATE,
    /** Significant time or distance since alignment; drift likely. */
    LOW,
    /** No alignment established. */
    NONE,
}

/**
 * Progress confidence — how trustworthy the current progress estimate is.
 */
@Serializable
enum class ProgressConfidence {
    /** On-route, tracking normal, recent alignment. */
    RELIABLE,
    /** Tracking normal but al
```

## Status
Mapped (Pass 3 Normalization)
