# File Dossier: CompletionStatus.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\navigation\CompletionStatus.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.navigation

import kotlinx.serialization.Serializable

/**
 * Completion status of a navigation session.
 */
@Serializable
enum class CompletionStatus {
    /** User reached the destination. */
    COMPLETED_AT_DESTINATION,
    /** User manually ended navigation before arriving. */
    ENDED_MANUALLY,
    /** User cancelled before marker alignment. */
    CANCELLED_BEFORE_ALIGNMENT,
    /** User cancelled after alignment but before arriving. */
    CANCELLED_AFTER_ALIGNMENT,
    /** Tracking was lost and session ended. */
    LOST_TRACKING_ENDED,
    /** Demo mode completed (simulated). */
    DEMO_COMPLETED,
}

/**
 * Navigation session mode.
 */
@Serializable
enum class SessionMode {
    REAL_SCAN,
    SIMULATED_SCAN,
}

```

## Status
Mapped (Pass 3 Normalization)
