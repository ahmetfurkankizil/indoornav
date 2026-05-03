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
        }
    }

    override suspend fun getRooms(buildingId: String): List<Room> {
        return store.get(buildingId)?.rooms ?: emptyList()
    }

    override suspend fun getNavGraph(buildingId: String): NavGraph? {
        return store.get(buildingId)?.navGraph
    }

    override suspend fun getEntranceMarkers(buildingId: String): List<EntranceMarker> {
        return store.get(buildingId)?.entranceMarkers ?: emptyList()
    }

    override suspend fun isCached(buildingId: String): Boolean {
        return store.has(buildingId)
    }

    override suspend fun cacheBuildingData(buildingId: String) {
        // In the MVP, packages are loaded directly into the store.
        // This method is a no-op since the store IS the cache.
        // Future: download from remote and persist to SqlDelight.
    }
}
