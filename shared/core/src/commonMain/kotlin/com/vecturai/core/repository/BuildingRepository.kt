package com.Vectura AI.core.repository

import com.Vectura AI.core.domain.EntranceMarker
import com.Vectura AI.core.domain.NavGraph
import com.Vectura AI.core.domain.Room

/**
 * Repository for building data access.
 *
 * Provides access to the building's static data: rooms, navigation graph,
 * and entrance markers. Data is loaded from the building package (downloaded
 * remotely and cached locally).
 *
 * Implementations:
 * - Remote: downloads building package from server
 * - Local: reads from SQLite/file cache
 * - Combined: tries local first, falls back to remote, then caches
 */
interface BuildingRepository {

    /**
     * Load the list of available buildings.
     *
     * For MVP, this returns a single building.
     * TODO: Support multiple buildings in future versions.
     */
    suspend fun getAvailableBuildings(): List<BuildingInfo>

    /**
     * Load all rooms for a given building.
     *
     * @param buildingId Building identifier
     * @return List of rooms, or empty if building not found
     */
    suspend fun getRooms(buildingId: String): List<Room>

    /**
     * Load the navigation graph for a given building.
     *
     * @param buildingId Building identifier
     * @return Navigation graph, or null if not available
     */
    suspend fun getNavGraph(buildingId: String): NavGraph?

    /**
     * Load entrance markers for a given building.
     *
     * @param buildingId Building identifier
     * @return List of entrance markers
     */
    suspend fun getEntranceMarkers(buildingId: String): List<EntranceMarker>

    /**
     * Check if building data is cached locally.
     *
     * @param buildingId Building identifier
     * @return true if complete building package is available offline
     */
    suspend fun isCached(buildingId: String): Boolean

    /**
     * Download and cache building data for offline use.
     *
     * @param buildingId Building identifier
     */
    suspend fun cacheBuildingData(buildingId: String)
}

/**
 * Basic building information for listing.
 *
 * @property id Unique building identifier
 * @property name Display name
 * @property address Physical address
 * @property version Data version for cache invalidation
 */
data class BuildingInfo(
    val id: String,
    val name: String,
    val address: String = "",
    val version: Int = 1,
)
