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
 * @property generatedAt ISO 8601 timestamp when the package was generated
 * @property preprocessorVersion Version of the preprocessor that created this package
 * @property assetFile Name of the .glb preview asset
 */
@Serializable
data class BuildingManifest(
    val buildingId: String,
    val buildingName: String,
    val floorId: String = "ground",
    val version: Int = 1,
    val schemaVersion: Int = 1,
    val generatedAt: String = "",
    val preprocessorVersion: String = "0.1.0",
    val assetFile: String? = null,
    val files: Map<String, String> = emptyMap(),
)
