package com.VecturAI.data.remote

/**
 * Interface for remote building data source.
 *
 * Downloads building packages from the VecturAI backend server.
 * A building package contains the manifest, navigation graph,
 * rooms, entrance markers, and rendering configuration.
 *
 * TODO: Define API endpoint specifications
 * TODO: Handle authentication if required
 * TODO: Support package versioning for incremental updates
 */
interface RemoteBuildingDataSource {

    /**
     * Fetch the manifest for a building.
     *
     * The manifest contains metadata and the current version of
     * all data files in the building package.
     *
     * @param buildingId Building identifier
     * @return Manifest JSON string, or null if building not found
     */
    suspend fun fetchManifest(buildingId: String): String?

    /**
     * Fetch a specific data file from the building package.
     *
     * @param buildingId Building identifier
     * @param fileName Name of the data file (e.g., "nav_graph.json")
     * @return File content as JSON string, or null if not found
     */
    suspend fun fetchBuildingData(buildingId: String, fileName: String): String?

    /**
     * Fetch the complete building package as a bundle.
     *
     * Downloads all data files in one request for initial caching.
     *
     * @param buildingId Building identifier
     * @return Map of filename to JSON content
     */
    suspend fun fetchBuildingPackage(buildingId: String): Map<String, String>?

    /**
     * Check the latest version available on the server.
     *
     * @param buildingId Building identifier
     * @return Latest version number, or -1 if unavailable
     */
    suspend fun getLatestVersion(buildingId: String): Int
    /**
     * Fetch the complete building package using a QR token.
     *
     * @param token The QR token scanned from the entrance
     * @return Package JSON string, or null if not found
     */
    suspend fun fetchBuildingPackageByToken(token: String): String?
}
