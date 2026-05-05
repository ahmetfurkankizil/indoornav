package com.VecturAI.data.local

/**
 * Interface for local cache data source.
 *
 * Provides file-system or database-backed caching of building data
 * for offline access after initial download.
 *
 * TODO: Define table schema via SqlDelight .sq files
 * TODO: Implement proper cache invalidation by version
 */
interface LocalCacheDataSource {

    /**
     * Check if a building package is cached locally.
     */
    suspend fun hasCachedData(buildingId: String): Boolean

    /**
     * Get cached JSON data for a specific data type.
     *
     * @param buildingId Building identifier
     * @param dataType Type of data (e.g., "nav_graph", "rooms", "markers")
     * @return Cached JSON string, or null if not cached
     */
    suspend fun getCachedData(buildingId: String, dataType: String): String?

    /**
     * Store data in the local cache.
     *
     * @param buildingId Building identifier
     * @param dataType Type of data being cached
     * @param jsonData JSON string to cache
     */
    suspend fun cacheData(buildingId: String, dataType: String, jsonData: String)

    /**
     * Get the cached data version for a building.
     *
     * @return Version number, or -1 if not cached
     */
    suspend fun getCachedVersion(buildingId: String): Int

    /**
     * Clear cached data for a specific building.
     */
    suspend fun clearCache(buildingId: String)

    /**
     * Clear all cached data.
     */
    suspend fun clearAllCaches()
}
