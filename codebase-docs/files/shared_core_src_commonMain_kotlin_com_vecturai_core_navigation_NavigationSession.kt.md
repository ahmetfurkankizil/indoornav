# File Dossier: NavigationSession.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\navigation\NavigationSession.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.navigation

import kotlinx.serialization.Serializable

/**
 * A navigation session from start to completion/cancellation.
 *
 * Created when the user initiates navigation, updated as the session
 * progresses, and persisted as a [VisitRecord] when the session ends.
 */
@Serializable
data class NavigationSession(
    val sessionId: String,
    val buildingId: String,
    val buildingName: String,
    val destinationRoomId: String,
    val destinationDisplayName: String,
    val startedAtIso: String,
    val endedAtIso: String? = null,
    val completionStatus: CompletionStatus? = null,
    val routeDistanceMeters: Double = 0.0,
    val routeStepCount: Int = 0,
    val entranceMarkerId: String? = null,
    val mode: SessionMode = SessionMode.REAL_SCAN,
    val progressFraction: Double = 0.0,
    val debugNotes: String? = null,
) {
    val isActive: Boolean get() = completionStatus == null
    val durationSeconds: Long?
        get() {
            if (endedAtIso 
```

## Status
Mapped (Pass 3 Normalization)
