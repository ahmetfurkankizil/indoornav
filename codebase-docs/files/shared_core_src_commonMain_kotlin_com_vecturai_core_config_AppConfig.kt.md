# File Dossier: AppConfig.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\config\AppConfig.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.config

/**
 * App configuration profiles.
 *
 * Controls debug overlays, logging, demo mode defaults, and
 * simulate-scan availability per build variant.
 */
sealed class AppConfig {

    abstract val profileName: String
    abstract val logVerbose: Boolean
    abstract val showDebugOverlays: Boolean
    abstract val simulateAlignmentEnabled: Boolean
    abstract val preloadSamplePackage: Boolean
    abstract val showDemoModeLabel: Boolean
    abstract val defaultToDemoMode: Boolean

    /** Development profile — everything enabled. */
    data object Dev : AppConfig() {
        override val profileName = "dev"
        override val logVerbose = true
        override val showDebugOverlays = true
        override val simulateAlignmentEnabled = true
        override val preloadSamplePackage = true
        override val showDemoModeLabel = true
        override val defaultToDemoMode = false
    }

    /** Demo/investor profile — simulate enabled, debug hidden by 
```

## Status
Mapped (Pass 3 Normalization)
