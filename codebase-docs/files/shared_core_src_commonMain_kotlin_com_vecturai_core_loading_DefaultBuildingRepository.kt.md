# File Dossier: DefaultBuildingRepository.kt

## Path
`shared\core\src\commonMain\kotlin\com\VecturAI\core\loading\DefaultBuildingRepository.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.core.loading

import com.VecturAI.core.domain.EntranceMarker
import com.VecturAI.core.domain.NavGraph
import com.VecturAI.core.domain.Room
import com.VecturAI.core.repository.BuildingInfo
import com.VecturAI.core.repository.BuildingRepository

/**
 * Default implementation of [BuildingRepository] backed by [InMemoryPackageStore].
 *
 * Packages are loaded externally (via bundled JSON, remote download, etc.)
 * and stored in the in-memory store. This repository simply reads from it.
 */
class DefaultBuildingRepository(
    private val store: InMemoryPackageStore,
) : BuildingRepository {

    override suspend fun getAvailableBuildings(): List<BuildingInfo> {
        return store.storedBuildingIds().mapNotNull { id ->
            val pkg = store.get(id) ?: return@mapNotNull null
            BuildingInfo(
                id = pkg.manifest.buildingId,
                name = pkg.manifest.buildingName,
                version = pkg.manifest.version,
            )
       
```

## Status
Mapped (Pass 3 Normalization)
