# File Dossier: Room.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\domain\Room.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.domain

import kotlinx.serialization.Serializable

/**
 * A room or point of interest within the building.
 *
 * Rooms are the primary searchable navigation destinations.
 *
 * @property id Unique room identifier
 * @property name Human-readable display name (e.g., "Conference Room B")
 * @property description Optional room description
 * @property category Room category for filtering (e.g., "office", "restroom")
 * @property entryNodeIds IDs of NavNodes that serve as entry points
 * @property keywords Searchable keywords (e.g., ["printer", "copy"])
 * @property aliases Alternate names (e.g., ["HR Office", "Room 203"])
 * @property centerX Center X coordinate for map display (meters, building-local)
 * @property centerY Center Y coordinate for map display (meters, building-local)
 * @property floor Floor identifier (always "ground" for single-floor MVP)
 * @property metadata Arbitrary key-value metadata for extensibility
 */
@Serializable
data class Room(
    
```

## Status
Mapped (Pass 3 Normalization)
