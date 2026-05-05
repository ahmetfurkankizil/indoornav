package com.VecturAI.core.loading

import com.VecturAI.core.domain.*
import kotlinx.serialization.json.Json

/**
 * Loads a building package from its constituent JSON strings.
 *
 * This is the central deserialization point for building data.
 * It accepts raw JSON strings (from local cache or remote download)
 * and produces the typed domain models.
 */
class BuildingPackageLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    /**
     * Load a complete building package from individual JSON strings.
     *
     * @throws Exception on malformed JSON
     */
    fun load(
        manifestJson: String,
        navGraphJson: String,
        roomsJson: String,
        markersJson: String,
        renderingJson: String,
    ): BuildingPackage {
        val manifest = json.decodeFromString<BuildingManifest>(manifestJson)
        val navGraph = json.decodeFromString<NavGraph>(navGraphJson)
        val roomsWrapper = json.decodeFromString<RoomsWrapper>(roomsJson)
        val markersWrapper = json.decodeFromString<MarkersWrapper>(markersJson)
        val rendering = json.decodeFromString<RouteRenderingConfig>(renderingJson)

        return BuildingPackage(
            manifest = manifest,
            navGraph = navGraph,
            rooms = roomsWrapper.rooms,
            entranceMarkers = markersWrapper.markers,
            renderingConfig = rendering,
        )
    }

    /**
     * Load a NavGraph from JSON.
     */
    fun loadNavGraph(jsonStr: String): NavGraph =
        json.decodeFromString<NavGraph>(jsonStr)

    /**
     * Load rooms from JSON.
     */
    fun loadRooms(jsonStr: String): List<Room> =
        json.decodeFromString<RoomsWrapper>(jsonStr).rooms

    /**
     * Load entrance markers from JSON.
     */
    fun loadMarkers(jsonStr: String): List<EntranceMarker> =
        json.decodeFromString<MarkersWrapper>(jsonStr).markers
}

/**
 * Wrapper for rooms JSON deserialization (top-level has buildingId + rooms array).
 */
@kotlinx.serialization.Serializable
internal data class RoomsWrapper(
    val buildingId: String = "",
    val rooms: List<Room>,
)

/**
 * Wrapper for markers JSON deserialization.
 */
@kotlinx.serialization.Serializable
internal data class MarkersWrapper(
    val buildingId: String = "",
    val markers: List<EntranceMarker>,
)
