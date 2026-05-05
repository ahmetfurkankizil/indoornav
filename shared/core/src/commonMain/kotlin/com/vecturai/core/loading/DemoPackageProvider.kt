package com.VecturAI.core.loading

import com.VecturAI.core.domain.*
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
        version = 1,
    )

    /** Demo navigation graph. */
    fun navGraph(): NavGraph {
        val nodes = listOf(
            NavNode("n01", 0.0, 0.0, 0.0, NodeType.ENTRANCE, "Entrance"),
            NavNode("n02", 3.0, 0.0, 0.0, NodeType.WAYPOINT, "Hallway A"),
            NavNode("n03", 6.0, 0.0, 0.0, NodeType.JUNCTION, "Junction B"),
            NavNode("n04", 6.0, 0.0, 4.0, NodeType.WAYPOINT, "Hallway C"),
            NavNode("n05", 3.0, 0.0, 4.0, NodeType.ROOM_ENTRY, "Conference Room Entry"),
            NavNode("n06", 0.0, 0.0, 4.0, NodeType.ROOM_ENTRY, "Kitchen Entry"),
            NavNode("n07", 9.0, 0.0, 0.0, NodeType.ROOM_ENTRY, "Office A Entry"),
            NavNode("n08", 9.0, 0.0, 4.0, NodeType.ROOM_ENTRY, "Office B Entry"),
        )

        val edges = listOf(
            NavEdge(from = "n01", to = "n02", weight = 3.0),
            NavEdge(from = "n02", to = "n01", weight = 3.0),
            NavEdge(from = "n02", to = "n03", weight = 3.0),
            NavEdge(from = "n03", to = "n02", weight = 3.0),
            NavEdge(from = "n03", to = "n04", weight = 4.0),
            NavEdge(from = "n04", to = "n03", weight = 4.0),
            NavEdge(from = "n04", to = "n05", weight = 3.0),
            NavEdge(from = "n05", to = "n04", weight = 3.0),
            NavEdge(from = "n05", to = "n06", weight = 3.0),
            NavEdge(from = "n06", to = "n05", weight = 3.0),
            NavEdge(from = "n03", to = "n07", weight = 3.0),
            NavEdge(from = "n07", to = "n03", weight = 3.0),
            NavEdge(from = "n04", to = "n08", weight = 3.0),
            NavEdge(from = "n08", to = "n04", weight = 3.0),
        )

        return NavGraph(buildingId = "demo-office-01", nodes = nodes, edges = edges)
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
            qrPayload = "VecturAI://demo-office-01/marker-main",
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
