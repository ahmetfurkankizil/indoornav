# File Dossier: DefaultBuildingRepository.kt

## Path
`shared\core\src\commonMain\kotlin\com\Vectura AI\core\loading\DefaultBuildingRepository.kt`

## Type
Authored Source

## Role
Authored Source for the shared component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.core.loading

import com.Vectura AI.core.domain.EntranceMarker
import com.Vectura AI.core.domain.NavGraph
import com.Vectura AI.core.domain.Room
import com.Vectura AI.core.repository.BuildingInfo
import com.Vectura AI.core.repository.BuildingRepository

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
