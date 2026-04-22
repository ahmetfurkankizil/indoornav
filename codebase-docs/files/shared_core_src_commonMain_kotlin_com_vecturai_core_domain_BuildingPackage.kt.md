# File Dossier: BuildingPackage.kt

## Path
`shared\core\src\commonMain\kotlin\com\vecturai\core\domain\BuildingPackage.kt`

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
 * Complete building package — all data needed to navigate a single building.
 *
 * This is the runtime representation of a processed building package.
 * It is deserialized from the package JSON files by [BuildingPackageLoader].
 */
@Serializable
data class BuildingPackage(
    val manifest: BuildingManifest,
    val navGraph: NavGraph,
    val rooms: List<Room>,
    val entranceMarkers: List<EntranceMarker>,
    val renderingConfig: RouteRenderingConfig,
    val checkpointMarkers: List<CheckpointMarker> = emptyList(),
)

/**
 * Building package manifest — top-level metadata.
 *
 * @property buildingId Unique building identifier
 * @property buildingName Display name
 * @property floorId Floor identifier
 * @property version Data version for cache invalidation (monotonically increasing)
 * @property schemaVersion Contract schema version
 * @property generatedAt ISO 8601 timestamp when the package was gene
```

## Status
Mapped (Pass 3 Normalization)
