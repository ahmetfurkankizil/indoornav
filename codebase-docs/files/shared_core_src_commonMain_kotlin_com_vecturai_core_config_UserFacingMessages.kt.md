# File Dossier: UserFacingMessages.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\config\UserFacingMessages.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.config

/**
 * User-facing messages for presentation-safe UI strings.
 *
 * Provides clear, non-technical language for all user-visible states.
 * Separate from debug logging. Use these in UI overlays and status displays.
 *
 * Two tiers:
 * - [user]: Clean, non-technical language for presenters and end users
 * - [debug]: Technical detail for operators and developers
 */
object UserFacingMessages {

    // ── Package Loading ─────────────────────────
    object PackageLoading {
        const val LOADING = "Loading building data…"
        const val LOADED = "Building loaded"
        const val NOT_FOUND = "Building data not available. Please check your connection."
        const val CORRUPT = "Building data could not be read. Try reloading."
        const val DEBUG_NOT_FOUND = "[Package] File not found at path"
        const val DEBUG_CORRUPT = "[Package] JSON parse failed"
    }

    // ── Marker Detection ────────────────────────
    object MarkerDetection {

```

## Status
Mapped (Pass 3 Normalization)
