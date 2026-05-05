# File Dossier: NavigationState.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\domain\NavigationState.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.domain

/**
 * Represents the current state of the navigation flow.
 *
 * This sealed class defines all possible states the navigation system
 * can be in. It is observed by both the Compose UI (for textual guidance)
 * and the native AR shells (for 3D rendering).
 *
 * State transitions:
 * ```
 * Idle → Scanning → Navigating → Arrived
 *                  ↗ (re-scan)
 *        Scanning ←─── Navigating (if alignment lost)
 * ```
 */
sealed class NavigationState {

    /** No active navigation session. Default state. */
    data object Idle : NavigationState()

    /**
     * Waiting for the user to scan the entrance marker.
     *
     * @property buildingId Building being navigated
     * @property targetRoom The room the user wants to reach
     */
    data class Scanning(
        val buildingId: String,
        val targetRoom: Room,
    ) : NavigationState()

    /**
     * Actively navigating with AR guidance.
     *
     * @property route The computed rou
```

## Status
Mapped (Pass 3 Normalization)
