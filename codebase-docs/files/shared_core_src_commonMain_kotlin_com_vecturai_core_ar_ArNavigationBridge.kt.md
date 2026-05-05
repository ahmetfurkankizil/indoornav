# File Dossier: ArNavigationBridge.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\ar\ArNavigationBridge.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.ar

import com.VecturAI.core.domain.BuildingPackage
import com.VecturAI.core.domain.Room

/**
 * Bridge interface between shared navigation logic and native AR layer.
 *
 * Implemented differently on each platform, but called uniformly
 * from shared code (via the [ArNavigationCoordinator]).
 *
 * The shared layer calls these to communicate state changes.
 * The native layer implements them to drive AR session behavior.
 */
interface ArNavigationBridge {

    /**
     * Start an AR session for navigating to the given destination.
     *
     * The native layer should:
     * 1. Configure the AR session
     * 2. Load marker reference images
     * 3. Start the camera and tracking
     */
    fun startSession(buildingPackage: BuildingPackage, destination: Room)

    /** Stop the AR session and clean up resources. */
    fun stopSession()

    /**
     * Called by shared code when marker alignment completes.
     * Native layer receives the alignment transform f
```

## Status
Mapped (Pass 3 Normalization)
