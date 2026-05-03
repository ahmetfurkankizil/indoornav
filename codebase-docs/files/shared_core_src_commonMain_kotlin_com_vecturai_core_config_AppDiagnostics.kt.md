# File Dossier: AppDiagnostics.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\config\AppDiagnostics.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.config

import com.Vectura AI.core.navigation.NavigationSession
import kotlinx.serialization.Serializable

/**
 * Diagnostic snapshot for debug/QA.
 *
 * Captures current app state for troubleshooting and demo verification.
 * Extended in v1.6 with correction/confidence diagnostics.
 */
@Serializable
data class AppDiagnostics(
    val appVersion: String,
    val configProfile: String,
    val loadedBuildingId: String?,
    val loadedBuildingName: String?,
    val packageNodeCount: Int,
    val packageRoomCount: Int,
    val packageMarkerCount: Int,
    val packageCheckpointMarkerCount: Int = 0,
    val activeSessionId: String?,
    val activeDestination: String?,
    val sessionMode: String?,
    val historyCount: Int,
    val isDemoMode: Boolean,
    val simulateEnabled: Boolean,
    // v1.6 — correction & confidence
    val currentMarkerId: String? = null,
    val currentMarkerRole: String? = null,
    val lastCorrectionTimeMs: Long = 0L,
    val correctionCou
```

## Status
Mapped (Pass 3 Normalization)
