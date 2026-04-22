package com.vecturai.android.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class AndroidReviewedPackageLoader(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    @Serializable
    data class PackageNode(
        val id: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val type: String,
        val label: String? = null,
    )

    @Serializable
    data class PackageEdge(
        val id: String,
        val from: String,
        val to: String,
        val cost: Double,
        val bidirectional: Boolean = true,
    )

    @Serializable
    data class Position3D(
        val x: Double,
        val y: Double,
        val z: Double,
    )

    @Serializable
    data class PackageMarker(
        val id: String,
        val displayName: String? = null,
        val startNodeId: String,
        val physicalWidthMeters: Double,
        val physicalHeightMeters: Double,
        val position: Position3D,
        val forwardBasis: String,
        val rotationYDegrees: Double? = null,
        val referenceImageName: String? = null,
        val notes: String? = null,
    )

    @Serializable
    data class PackageRoom(
        val id: String,
        val displayName: String,
        val destinationNodeId: String,
        val category: String? = null,
        val description: String? = null,
    )

    @Serializable
    data class RouteRenderingConfig(
        val arrowSpacingMeters: Double,
        val lookaheadDistanceMeters: Double,
        val destinationThresholdMeters: Double,
        val turnMarkerThresholdDegrees: Double,
        val arrowHeightOffsetMeters: Double,
    )

    @Serializable
    data class Manifest(
        val packageVersion: String,
        val buildingId: String,
        val buildingName: String,
        val floorId: String,
        val reviewStatus: String,
        val files: ManifestFiles,
    )

    @Serializable
    data class ManifestFiles(
        val rooms: String,
        val navGraph: String,
        val entranceMarkers: String,
        val routeRendering: String,
    )

    @Serializable
    private data class RoomsFile(val rooms: List<PackageRoom>)

    @Serializable
    private data class NavGraphFile(
        val nodes: List<PackageNode>,
        val edges: List<PackageEdge>,
    )

    @Serializable
    private data class EntranceMarkersFile(val entranceMarkers: List<PackageMarker>)

    data class ReviewedConfig(
        val manifest: Manifest,
        val rooms: List<PackageRoom>,
        val nodes: List<PackageNode>,
        val edges: List<PackageEdge>,
        val entranceMarkers: List<PackageMarker>,
        val routeRendering: RouteRenderingConfig,
    )

    data class LoadedPackage(
        val config: ReviewedConfig,
        val routeNodeIds: List<String>,
        val arrows: List<ArrowPlacementData>,
        val routePoints: List<Pair<Double, Double>>,
        val totalDistance: Double,
        val entranceMarker: PackageMarker?,
        val destinationName: String,
        val destinationPosition: Pair<Double, Double>,
    )

    sealed class PackageError(
        override val message: String,
    ) : Exception(message) {
        data object ManifestMissing : PackageError("Reviewed package manifest.json not found in assets")
        data class FileMissing(val name: String) : PackageError("Reviewed package file missing: $name")
        data class DecodingFailed(val name: String, val causeError: Throwable) :
            PackageError("Failed to decode $name: ${causeError.message ?: "unknown error"}")
        data object NoEntranceMarker : PackageError("No entrance markers defined in reviewed package")
        data object NoRooms : PackageError("No rooms defined in reviewed package")
    }

    fun loadReviewedPackage(): Result<ReviewedConfig> {
        val manifest = decodeAsset<Manifest>("manifest.json").getOrElse {
            return Result.failure(it)
        }

        val roomsFile = decodeAsset<RoomsFile>(manifest.files.rooms).getOrElse {
            return Result.failure(it)
        }
        if (roomsFile.rooms.isEmpty()) return Result.failure(PackageError.NoRooms)

        val graphFile = decodeAsset<NavGraphFile>(manifest.files.navGraph).getOrElse {
            return Result.failure(it)
        }

        val markersFile = decodeAsset<EntranceMarkersFile>(manifest.files.entranceMarkers).getOrElse {
            return Result.failure(it)
        }
        if (markersFile.entranceMarkers.isEmpty()) return Result.failure(PackageError.NoEntranceMarker)

        val rendering = decodeAsset<RouteRenderingConfig>(manifest.files.routeRendering).getOrElse {
            return Result.failure(it)
        }

        return Result.success(
            ReviewedConfig(
                manifest = manifest,
                rooms = roomsFile.rooms,
                nodes = graphFile.nodes,
                edges = graphFile.edges,
                entranceMarkers = markersFile.entranceMarkers,
                routeRendering = rendering,
            )
        )
    }

    fun computeRoute(config: ReviewedConfig, destinationRoomId: String): LoadedPackage? {
        val nodeMap = config.nodes.associateBy { it.id }
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        for (edge in config.edges) {
            adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to to edge.cost)
            if (edge.bidirectional) {
                adjacency.getOrPut(edge.to) { mutableListOf() }.add(edge.from to edge.cost)
            }
        }

        val startNodeId = config.entranceMarkers.firstOrNull()?.startNodeId
            ?: config.nodes.firstOrNull()?.id
            ?: return null
        val room = config.rooms.firstOrNull { it.id == destinationRoomId } ?: return null
        val routeNodeIds = dijkstra(startNodeId, room.destinationNodeId, adjacency)
        if (routeNodeIds.size < 2) return null

        val routeNodes = routeNodeIds.mapNotNull { nodeMap[it] }
        if (routeNodes.size < 2) return null

        val arrows = generateArrows(
            nodes = routeNodes,
            destinationLabel = room.displayName,
            spacing = config.routeRendering.arrowSpacingMeters,
            heightOffset = config.routeRendering.arrowHeightOffsetMeters,
        )
        val points = routeNodes.map { it.x to it.z }
        val distance = totalDistance(points)
        val destinationNode = routeNodes.last()

        return LoadedPackage(
            config = config,
            routeNodeIds = routeNodeIds,
            arrows = arrows,
            routePoints = points,
            totalDistance = distance,
            entranceMarker = config.entranceMarkers.firstOrNull(),
            destinationName = room.displayName,
            destinationPosition = destinationNode.x to destinationNode.z,
        )
    }

    private inline fun <reified T> decodeAsset(fileName: String): Result<T> {
        val path = "reviewed-package/$fileName"
        val raw = try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            return if (fileName == "manifest.json") {
                Result.failure(PackageError.ManifestMissing)
            } else {
                Result.failure(PackageError.FileMissing(fileName))
            }
        }

        return try {
            Result.success(json.decodeFromString<T>(raw))
        } catch (t: Throwable) {
            Result.failure(PackageError.DecodingFailed(fileName, t))
        }
    }

    private fun dijkstra(
        start: String,
        end: String,
        adjacency: Map<String, List<Pair<String, Double>>>,
    ): List<String> {
        val dist = mutableMapOf(start to 0.0)
        val previous = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()

        while (true) {
            val current = dist
                .filterKeys { it !in visited }
                .minByOrNull { it.value }
                ?.key
                ?: break

            if (current == end) break
            visited.add(current)

            for ((neighbor, cost) in adjacency[current].orEmpty()) {
                val alternate = dist.getValue(current) + cost
                if (alternate < (dist[neighbor] ?: Double.MAX_VALUE)) {
                    dist[neighbor] = alternate
                    previous[neighbor] = current
                }
            }
        }

        val path = mutableListOf<String>()
        var cursor: String? = end
        while (cursor != null) {
            path.add(0, cursor)
            cursor = previous[cursor]
        }
        return if (path.firstOrNull() == start) path else emptyList()
    }

    private fun generateArrows(
        nodes: List<PackageNode>,
        destinationLabel: String,
        spacing: Double,
        heightOffset: Double,
    ): List<ArrowPlacementData> {
        if (nodes.size < 2) return emptyList()

        val arrows = mutableListOf<ArrowPlacementData>()
        var cumulativeDistance = 0.0

        for (i in 0 until nodes.lastIndex) {
            val current = nodes[i]
            val next = nodes[i + 1]
            val dx = next.x - current.x
            val dz = next.z - current.z
            val segmentLength = sqrt(dx * dx + dz * dz)
            if (segmentLength <= 0.01) continue

            val directionX = dx / segmentLength
            val directionZ = dz / segmentLength
            val count = max(1, (segmentLength / spacing).toInt())

            for (step in 0 until count) {
                val t = step.toDouble() / count.toDouble()
                val arrowCumulative = cumulativeDistance + t * segmentLength
                val type: ArrowPlacementType
                val label: String?

                if (i == nodes.size - 2 && step == count - 1) {
                    type = ArrowPlacementType.DESTINATION
                    label = destinationLabel
                } else if (step == 0 && i > 0) {
                    val previous = nodes[i - 1]
                    val previousDx = current.x - previous.x
                    val previousDz = current.z - previous.z
                    val cross = previousDx * directionZ - previousDz * directionX
                    if (abs(cross) > 0.3) {
                        type = if (cross > 0) ArrowPlacementType.TURN_RIGHT else ArrowPlacementType.TURN_LEFT
                        label = if (cross > 0) "Turn right" else "Turn left"
                    } else {
                        type = ArrowPlacementType.FOLLOW
                        label = null
                    }
                } else {
                    type = ArrowPlacementType.FOLLOW
                    label = null
                }

                arrows.add(
                    ArrowPlacementData(
                        id = "a${arrows.size}",
                        positionX = current.x + t * dx,
                        positionY = current.y + heightOffset,
                        positionZ = current.z + t * dz,
                        forwardDx = directionX,
                        forwardDy = 0.0,
                        forwardDz = directionZ,
                        type = type,
                        label = label,
                        cumulativeDistance = arrowCumulative,
                    )
                )
            }

            cumulativeDistance += segmentLength
        }

        return arrows
    }

    private fun totalDistance(points: List<Pair<Double, Double>>): Double {
        var distance = 0.0
        for (i in 1 until points.size) {
            val dx = points[i].first - points[i - 1].first
            val dz = points[i].second - points[i - 1].second
            distance += sqrt(dx * dx + dz * dz)
        }
        return distance
    }
}

data class ArrowPlacementData(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double,
    val forwardDx: Double,
    val forwardDy: Double,
    val forwardDz: Double,
    val type: ArrowPlacementType,
    val label: String?,
    val cumulativeDistance: Double,
)

enum class ArrowPlacementType {
    FOLLOW,
    TURN_LEFT,
    TURN_RIGHT,
    U_TURN,
    DESTINATION,
}
