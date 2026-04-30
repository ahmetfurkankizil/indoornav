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
        val arrowSpacingMeters: Double = 0.6,
        val lookaheadDistanceMeters: Double = 8.0,
        val destinationThresholdMeters: Double = 1.2,
        val turnMarkerThresholdDegrees: Double = 25.0,
        val arrowHeightOffsetMeters: Double = 0.1,
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
    data class UnifiedFloor(
        val floorId: String,
        val floorNumber: Int,
        val floorName: String,
        val floorY: Double,
        val nodes: List<PackageNode>,
        val edges: List<PackageEdge>,
    )

    @Serializable
    data class UnifiedPackage(
        val buildingId: String,
        val buildingName: String,
        val version: Int,
        val floors: List<UnifiedFloor>,
        val entranceMarkers: List<PackageMarker>,
        val buildingWidthMeters: Double = 25.0,
        val routeRendering: RouteRenderingConfig,
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

    fun parseUnifiedPackage(jsonString: String): Result<ReviewedConfig> = try {
        val unified = json.decodeFromString<UnifiedPackage>(jsonString)
        
        // SMART SCALE DETECTOR: Calculate the actual width of the map in units
        val allNodesRaw = unified.floors.flatMap { it.nodes }
        val minX = allNodesRaw.minOfOrNull { it.x } ?: 0.0
        val maxX = allNodesRaw.maxOfOrNull { it.x } ?: 1.0
        val mapUnitWidth = maxX - minX
        
        // Scale factor: Real Meters / Map Units
        val scaleFactor = if (mapUnitWidth > 0.1) unified.buildingWidthMeters / mapUnitWidth else 1.0
        
        println("[PackageLoader] Map unit width: $mapUnitWidth, Target width: ${unified.buildingWidthMeters}m, Scale factor: $scaleFactor")

        val allNodes = allNodesRaw.map { n ->
            n.copy(x = n.x * scaleFactor, y = n.y * scaleFactor, z = n.z * scaleFactor)
        }
        val allEdges = unified.floors.flatMap { it.edges }
        
        val rooms = allNodes.filter { it.type == "room" }.map { n ->
            PackageRoom(
                id = n.id,
                displayName = n.label ?: "Room ${n.id.take(4)}",
                destinationNodeId = n.id,
                category = "room"
            )
        }

        val scaledMarkers = unified.entranceMarkers.map { m ->
            m.copy(
                position = m.position.copy(
                    x = m.position.x * scaleFactor,
                    y = m.position.y * scaleFactor,
                    z = m.position.z * scaleFactor
                )
            )
        }

        Result.success(
            ReviewedConfig(
                manifest = Manifest(
                    packageVersion = unified.version.toString(),
                    buildingId = unified.buildingId,
                    buildingName = unified.buildingName,
                    floorId = unified.floors.firstOrNull()?.floorId ?: "0",
                    reviewStatus = "published",
                    files = ManifestFiles("", "", "", "")
                ),
                rooms = rooms,
                nodes = allNodes,
                edges = allEdges,
                entranceMarkers = scaledMarkers,
                routeRendering = unified.routeRendering
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun computeRoute(config: ReviewedConfig, destinationRoomId: String): LoadedPackage? {
        val nodeMap = config.nodes.associateBy { it.id }
        val adjacency = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        println("[RouteDebug] --- LOADING EDGES ---")
        for (edge in config.edges) {
            println("[RouteDebug] Edge in package: ${edge.from} -> ${edge.to} (cost: ${edge.cost})")
            adjacency.getOrPut(edge.from) { mutableListOf() }.add(edge.to to edge.cost)
            if (edge.bidirectional) {
                adjacency.getOrPut(edge.to) { mutableListOf() }.add(edge.from to edge.cost)
            }
        }
        println("[RouteDebug] --- END EDGES ---")

        val startNodeId = config.entranceMarkers.firstOrNull()?.startNodeId
            ?: config.nodes.find { it.type == "entrance" }?.id
            ?: config.nodes.firstOrNull()?.id
            ?: return null
        
        println("[RouteDebug] Starting navigation from node: $startNodeId")
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

        // LOGGING: Verify the path nodes in Logcat
        println("[RouteDebug] Path Nodes: ${nodes.map { "${it.id}(${it.x}, ${it.z})" }.joinToString(" -> ")}")

        val arrows = mutableListOf<ArrowPlacementData>()
        var cumulativeDistance = 0.0

        for (i in 0 until nodes.size - 1) {
            val current = nodes[i]
            val next = nodes[i + 1]
            val dx = next.x - current.x
            val dz = next.z - current.z
            val segmentLength = sqrt(dx * dx + dz * dz)
            
            if (segmentLength < 0.05) continue // Skip tiny segments

            val directionX = dx / segmentLength
            val directionZ = dz / segmentLength

            // Calculate how many arrows for this specific segment
            // We ensure at least one arrow at the start of the segment
            val count = max(1, (segmentLength / spacing).toInt())
            val actualSpacing = segmentLength / count

            for (step in 0 until count) {
                val t = step.toDouble() / count.toDouble()
                val arrowX = current.x + t * dx
                val arrowZ = current.z + t * dz
                val arrowCumulative = cumulativeDistance + (t * segmentLength)

                var type = ArrowPlacementType.FOLLOW
                var label: String? = null

                // TURN DETECTION: Check if this is the start of a segment and we have a previous segment
                if (step == 0 && i > 0) {
                    val prev = nodes[i - 1]
                    val prevDx = current.x - prev.x
                    val prevDz = current.z - prev.z
                    val prevLen = sqrt(prevDx * prevDx + prevDz * prevDz)
                    if (prevLen > 0.01) {
                        val pDx = prevDx / prevLen
                        val pDz = prevDz / prevLen
                        // Cross product for turn direction
                        val cross = pDx * directionZ - pDz * directionX
                        if (abs(cross) > 0.05) { // High sensitivity
                            type = if (cross > 0) ArrowPlacementType.TURN_RIGHT else ArrowPlacementType.TURN_LEFT
                            label = if (cross > 0) "Turn right" else "Turn left"
                            println("[RouteDebug] TURN detected at ${current.id}: $label (cross: $cross)")
                        }
                    }
                }

                arrows.add(
                    ArrowPlacementData(
                        id = "a${arrows.size}",
                        positionX = arrowX,
                        positionY = current.y + heightOffset,
                        positionZ = arrowZ,
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

        // Add final destination marker at the very last node
        val lastNode = nodes.last()
        val prevNode = nodes[nodes.size - 2]
        val lastDx = lastNode.x - prevNode.x
        val lastDz = lastNode.z - prevNode.z
        val lastLen = sqrt(lastDx * lastDx + lastDz * lastDz)
        
        arrows.add(
            ArrowPlacementData(
                id = "a_dest",
                positionX = lastNode.x,
                positionY = lastNode.y + heightOffset,
                positionZ = lastNode.z,
                forwardDx = if (lastLen > 0) lastDx / lastLen else 0.0,
                forwardDy = 0.0,
                forwardDz = if (lastLen > 0) lastDz / lastLen else 1.0,
                type = ArrowPlacementType.DESTINATION,
                label = destinationLabel,
                cumulativeDistance = cumulativeDistance,
            )
        )

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
