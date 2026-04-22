# File Dossier: AppVersion.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\config\AppVersion.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.config

/**
 * App version and build metadata.
 *
 * Single source of truth for version across all platforms.
 * Update this file when cutting a release or RC.
 */
object AppVersion {
    const val NAME = "VecturAI"
    const val VERSION = "1.7.0-rc1"
    const val BUILD_PHASE = "Phase 8 — RC"
    const val BUILD_DATE = "2026-03-10"

    fun displayString(): String = "$NAME v$VERSION"
    fun fullString(): String = "$NAME v$VERSION ($BUILD_PHASE, $BUILD_DATE)"
}

```

## Status
Mapped (Pass 3 Normalization)
