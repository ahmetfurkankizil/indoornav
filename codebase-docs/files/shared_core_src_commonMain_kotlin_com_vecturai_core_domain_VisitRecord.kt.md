# File Dossier: VisitRecord.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\domain\VisitRecord.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.domain

import kotlinx.serialization.Serializable

/**
 * A record of a completed or ended navigation visit.
 *
 * Lives in core so that [HistoryRepository] and [NavigationSessionCoordinator]
 * can reference it without creating circular dependencies between core → feature.
 *
 * The feature-history module re-exports / uses this type directly via its own
 * typealias or by importing core.
 */
@Serializable
data class VisitRecord(
    /** Unique visit/session ID. */
    val visitId: String,
    /** Building identifier. */
    val buildingId: String,
    /** Building display name. */
    val buildingName: String = "",
    /** Destination room ID. */
    val roomId: String,
    /** Destination room display name. */
    val roomName: String,
    /** Visit start timestamp (ISO-8601). */
    val visitedAtIso: String,
    /** Visit end timestamp (ISO-8601). */
    val endedAtIso: String? = null,
    /** Completion status label. */
    val completionStatus: String = "
```

## Status
Mapped (Pass 3 Normalization)
