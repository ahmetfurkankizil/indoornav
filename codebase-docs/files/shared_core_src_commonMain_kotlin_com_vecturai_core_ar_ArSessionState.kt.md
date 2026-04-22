# File Dossier: ArSessionState.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\ar\ArSessionState.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.ar

import kotlinx.serialization.Serializable

/**
 * AR session state machine.
 *
 * Observed by both shared UI (for textual feedback) and native AR
 * (for session management decisions). State transitions are driven
 * by [ArNavigationCoordinator].
 */
@Serializable
sealed class ArSessionState {

    /** No AR session active. */
    @Serializable
    data object Idle : ArSessionState()

    /** AR session running, waiting for marker detection. */
    @Serializable
    data class WaitingForMarker(
        val buildingId: String,
        val destinationName: String,
    ) : ArSessionState()

    /** Marker detected but not yet stable/aligned. */
    @Serializable
    data class MarkerDetected(
        val markerId: String,
    ) : ArSessionState()

    /** World alignment established. Ready to render. */
    @Serializable
    data class Aligned(
        val markerId: String,
        val entranceNodeId: String,
    ) : ArSessionState()

    /** Tracking quality
```

## Status
Mapped (Pass 3 Normalization)
