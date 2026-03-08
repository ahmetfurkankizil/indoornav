package com.vecturai.core.loading

import com.vecturai.core.domain.*
import kotlinx.serialization.json.Json

/**
 * Provides a pre-built demo building package for instant app startup.
 *
 * Embeds the sample building data as JSON strings so the app can
 * start without running the preprocessor CLI. This is the primary
 * way demo builds load their data.
 */
object DemoPackageProvider {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Demo building manifest. */
    fun manifest(): BuildingManifest = BuildingManifest(
        buildingId = "demo-office-01",
        buildingName = "VecturAI Demo Office",
        floorCount = 1,
        version = "1.0.0",
    )

    /** Demo navigation graph. */
    fun navGraph(): NavGraph {
        val nodes = listOf(
            NavNode("n01", 0.0, 0.0, 0.0, "Entrance"),
            NavNode("n02", 3.0, 0.0, 0.0, "Hallway A"),
            NavNode("n03", 6.0, 0.0, 0.0, "Junction B"),
            NavNode("n04", 6.0, 0.0, 4.0, "Hallway C"),
            NavNode("n05", 3.0, 0.0, 4.0, "Conference Room Entry"),
            NavNode("n06", 0.0, 0.0, 4.0, "Kitchen Entry"),
            NavNode("n07", 9.0, 0.0, 0.0, "Office A Entry"),
            NavNode("n08", 9.0, 0.0, 4.0, "Office B Entry"),
        )

        val edges = listOf(
            NavEdge("n01", "n02", 3.0), NavEdge("n02", "n01", 3.0),
            NavEdge("n02", "n03", 3.0), NavEdge("n03", "n02", 3.0),
            NavEdge("n03", "n04", 4.0), NavEdge("n04", "n03", 4.0),
            NavEdge("n04", "n05", 3.0), NavEdge("n05", "n04", 3.0),
            NavEdge("n05", "n06", 3.0), NavEdge("n06", "n05", 3.0),
            NavEdge("n03", "n07", 3.0), NavEdge("n07", "n03", 3.0),
            NavEdge("n04", "n08", 3.0), NavEdge("n08", "n04", 3.0),
        )

        return NavGraph(nodes = nodes, edges = edges)
    }

    /** Demo rooms. */
    fun rooms(): List<Room> = listOf(
        Room(
            id = "conference-room",
            name = "Conference Room",
            aliases = listOf("Meeting Room", "Board Room"),
            keywords = listOf("meeting", "conference", "presentation"),
            category = "meeting",
            description = "Main conference room with projector and whiteboard",
            entryNodeIds = listOf("n05"),
        ),
        Room(
            id = "kitchen",
            name = "Kitchen",
            aliases = listOf("Break Room", "Cafeteria"),
            keywords = listOf("food", "coffee", "lunch", "break"),
            category = "amenity",
            description = "Kitchen with coffee machine and microwave",
            entryNodeIds = listOf("n06"),
        ),
        Room(
            id = "office-a",
            name = "Office A",
            aliases = listOf("Dev Room"),
            keywords = listOf("office", "workspace", "desk"),
            category = "workspace",
            description = "Open plan office space",
            entryNodeIds = listOf("n07"),
        ),
        Room(
            id = "office-b",
            name = "Office B",
            aliases = listOf("Design Studio"),
            keywords = listOf("office", "design", "studio"),
            category = "workspace",
            description = "Design team office",
            entryNodeIds = listOf("n08"),
        ),
    )

    /** Demo entrance markers. */
    fun entranceMarkers(): List<EntranceMarker> = listOf(
        EntranceMarker(
            id = "marker-main",
            qrPayload = "vecturai://demo-office-01/marker-main",
            positionX = 0.0,
            positionY = 1.2,
            positionZ = 0.0,
            rotationYDegrees = 0.0,
            forwardBasis = "-z",
            nearestNodeId = "n01",
            physicalWidthMeters = 0.21,
            physicalHeightMeters = 0.21,
            referenceImageName = "entrance_marker_main",
        ),
    )

    /** Demo rendering config. */
    fun renderingConfig(): RouteRenderingConfig = RouteRenderingConfig(
        arrowSpacingMeters = 1.5,
        turnMarkerThresholdDegrees = 30.0,
        arrowHeightOffsetMeters = 0.05,
        destinationThresholdMeters = 1.5,
    )

    /** Build the complete demo package. */
    fun buildPackage(): BuildingPackage = BuildingPackage(
        manifest = manifest(),
        navGraph = navGraph(),
        rooms = rooms(),
        entranceMarkers = entranceMarkers(),
        renderingConfig = renderingConfig(),
    )

    /** Curated demo destinations (ordered for investor demo). */
    val curatedDestinations = listOf("Conference Room", "Kitchen", "Office A")

    /** Default demo destination. */
    val defaultDestination = "Conference Room"

    /** Default start node. */
    val defaultStartNode = "n01"
}
