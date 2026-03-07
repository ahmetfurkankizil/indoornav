package com.vecturai.core.loading

import com.vecturai.core.domain.BuildingPackage

/**
 * Simple in-memory store for loaded building packages.
 *
 * For MVP, this serves as the "cache" — a package is loaded once
 * (from bundled assets, local files, or remote download) and stored
 * in memory for the app's lifetime.
 *
 * Future: replace with SqlDelight-backed persistent cache.
 */
class InMemoryPackageStore {

    private val packages = mutableMapOf<String, BuildingPackage>()

    /** Store a loaded package. */
    fun put(buildingId: String, pkg: BuildingPackage) {
        packages[buildingId] = pkg
    }

    /** Retrieve a stored package. */
    fun get(buildingId: String): BuildingPackage? = packages[buildingId]

    /** Check if a package is stored. */
    fun has(buildingId: String): Boolean = buildingId in packages

    /** Remove a stored package. */
    fun remove(buildingId: String) {
        packages.remove(buildingId)
    }

    /** Clear all stored packages. */
    fun clear() {
        packages.clear()
    }

    /** Get all stored building IDs. */
    fun storedBuildingIds(): Set<String> = packages.keys.toSet()
}
