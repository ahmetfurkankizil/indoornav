# File Dossier: DemoMode.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\navigation\DemoMode.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.core.navigation

import com.vecturai.core.domain.BuildingPackage
import com.vecturai.core.domain.Room

/**
 * Demo mode helper for investor-style presentations.
 *
 * Provides deterministic, one-tap navigation flows using the
 * sample building package and curated demo destinations.
 */
class DemoMode {

    /** Curated demo destination names (from sample building). */
    val demoDestinations = listOf(
        "Conference Room",
        "Kitchen",
        "Office A",
    )

    /** Default demo destination. */
    val defaultDestination = "Conference Room"

    /** Default start node (entrance). */
    val defaultStartNodeId = "n01"

    /**
     * Find a demo destination room in the package.
     * Falls back to the first available room.
     */
    fun findDemoRoom(buildingPackage: BuildingPackage, preferredName: String? = null): Room? {
        val rooms = buildingPackage.rooms
        val name = preferredName ?: defaultDestination

        return rooms.firstOr
```

## Status
Mapped (Pass 3 Normalization)
