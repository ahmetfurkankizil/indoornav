package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.AuthoringConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant

/**
 * Exports the validated authoring config into final package JSON files.
 *
 * Produces: manifest.json, nav_graph.json, rooms.json,
 *           entrance_markers.json, route_rendering.json
 * Also copies the .glb asset into the package as preview.glb.
 */
class PackageExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    data class ExportResult(
        val outputDir: String,
        val files: List<String>,
    )

    fun export(
        config: AuthoringConfig,
        glbPath: String,
        outputDir: String,
        overwrite: Boolean,
    ): ExportResult {
        val outDir = File(outputDir)
        if (outDir.exists() && !overwrite) {
            throw ValidationException("Output directory exists: $outputDir (use --overwrite to replace)")
        }
        outDir.mkdirs()

        val files = mutableListOf<String>()

        // ── nav_graph.json ──
        val navGraph = PackageNavGraph(
            buildingId = config.buildingId,
            floorId = config.floorId,
            schemaVersion = 1,
            nodes = config.nodes.map { n ->
                PackageNode(
                    id = n.id, x = n.x, y = n.y, z = n.z,
                    type = n.type, label = n.label,
                    roomId = config.rooms.find { it.destinationNodeId == n.id }?.id,
                )
            },
            edges = config.edges.map { e ->
                PackageEdge(id = e.id, from = e.from, to = e.to, weight = e.cost, bidirectional = e.bidirectional)
            },
        )
        writeJson(outDir, "nav_graph.json", json.encodeToString(navGraph))
        files.add("nav_graph.json")

        // ── rooms.json ──
        val rooms = PackageRooms(
            buildingId = config.buildingId,
            schemaVersion = 1,
            rooms = config.rooms.map { r ->
                val destNode = config.nodes.find { it.id == r.destinationNodeId }
                PackageRoom(
                    id = r.id,
                    name = r.displayName,
                    description = r.description,
                    category = r.category,
                    entryNodeIds = listOf(r.destinationNodeId),
                    keywords = r.keywords,
                    aliases = r.aliases,
                    centerX = destNode?.x ?: 0.0,
                    centerY = destNode?.z ?: 0.0,
                    floor = config.floorId,
                )
            },
        )
        writeJson(outDir, "rooms.json", json.encodeToString(rooms))
        files.add("rooms.json")

        // ── entrance_markers.json ──
        val markers = PackageMarkers(
            buildingId = config.buildingId,
            schemaVersion = 1,
            markers = config.entranceMarkers.map { m ->
                PackageMarker(
                    id = m.id,
                    qrPayload = "VecturAI://building/${config.buildingId}/marker/${m.id}",
                    positionX = m.position.x,
                    positionY = m.position.y,
                    positionZ = m.position.z,
                    rotationYDegrees = m.rotationYDegrees,
                    forwardBasis = m.forwardBasis,
                    nearestNodeId = m.startNodeId,
                    physicalWidthMeters = m.physicalWidthMeters,
                    physicalHeightMeters = m.physicalHeightMeters,
                    referenceImageName = m.referenceImageName,
                )
            },
        )
        writeJson(outDir, "entrance_markers.json", json.encodeToString(markers))
        files.add("entrance_markers.json")

        // ── checkpoint_markers.json (optional) ──
        if (config.checkpointMarkers.isNotEmpty()) {
            val checkpointMarkers = PackageCheckpointMarkers(
                buildingId = config.buildingId,
                schemaVersion = 1,
                markers = config.checkpointMarkers.map { cp ->
                    PackageCheckpointMarker(
                        id = cp.id,
                        positionX = cp.position.x,
                        positionY = cp.position.y,
                        positionZ = cp.position.z,
                        rotationYDegrees = cp.rotationYDegrees,
                        nearestNodeId = cp.nearestNodeId,
                        physicalWidthMeters = cp.physicalWidthMeters,
                        physicalHeightMeters = cp.physicalHeightMeters,
                        referenceImageName = cp.referenceImageName,
                        notes = cp.notes,
                    )
                },
            )
            writeJson(outDir, "checkpoint_markers.json", json.encodeToString(checkpointMarkers))
            files.add("checkpoint_markers.json")
        }

        // ── route_rendering.json ──
        val rendering = PackageRouteRendering(
            arrowSpacingMeters = config.routeRendering.arrowSpacingMeters,
            lookaheadDistanceMeters = config.routeRendering.lookaheadDistanceMeters,
            destinationThresholdMeters = config.routeRendering.destinationThresholdMeters,
            turnMarkerThresholdDegrees = config.routeRendering.turnMarkerThresholdDegrees,
            arrowHeightOffsetMeters = config.routeRendering.arrowHeightOffsetMeters,
        )
        writeJson(outDir, "route_rendering.json", json.encodeToString(rendering))
        files.add("route_rendering.json")

        // ── Copy .glb asset ──
        val glbFile = File(glbPath)
        if (glbFile.exists()) {
            glbFile.copyTo(File(outDir, "preview.glb"), overwrite = true)
            files.add("preview.glb")
        }

        // ── manifest.json ──
        val manifest = PackageManifest(
            buildingId = config.buildingId,
            buildingName = config.buildingName,
            floorId = config.floorId,
            version = 1,
            schemaVersion = 1,
            generatedAt = Instant.now().toString(),
            preprocessorVersion = "1.0.0",
            assetFile = if (glbFile.exists()) "preview.glb" else null,
            files = files.associateWith { "included" },
        )
        writeJson(outDir, "manifest.json", json.encodeToString(manifest))
        files.add("manifest.json")

        return ExportResult(outDir.absolutePath, files)
    }

    private fun writeJson(dir: File, name: String, content: String) {
        File(dir, name).writeText(content)
    }
}

// ── Package-level serializable DTOs (match contract schemas) ──

@Serializable
data class PackageManifest(
    val buildingId: String,
    val buildingName: String,
    val floorId: String,
    val version: Int,
    val schemaVersion: Int,
    val generatedAt: String,
    val preprocessorVersion: String,
    val assetFile: String?,
    val files: Map<String, String>,
)

@Serializable
data class PackageNavGraph(
    val buildingId: String,
    val floorId: String,
    val schemaVersion: Int,
    val nodes: List<PackageNode>,
    val edges: List<PackageEdge>,
)

@Serializable
data class PackageNode(
    val id: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val type: String,
    val label: String?,
    val roomId: String?,
)

@Serializable
data class PackageEdge(
    val id: String,
    val from: String,
    val to: String,
    val weight: Double,
    val bidirectional: Boolean,
)

@Serializable
data class PackageRooms(
    val buildingId: String,
    val schemaVersion: Int,
    val rooms: List<PackageRoom>,
)

@Serializable
data class PackageRoom(
    val id: String,
    val name: String,
    val description: String?,
    val category: String?,
    val entryNodeIds: List<String>,
    val keywords: List<String>,
    val aliases: List<String>,
    val centerX: Double,
    val centerY: Double,
    val floor: String,
)

@Serializable
data class PackageMarkers(
    val buildingId: String,
    val schemaVersion: Int,
    val markers: List<PackageMarker>,
)

@Serializable
data class PackageMarker(
    val id: String,
    val qrPayload: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double,
    val rotationYDegrees: Double,
    val forwardBasis: String,
    val nearestNodeId: String,
    val physicalWidthMeters: Double,
    val physicalHeightMeters: Double,
    val referenceImageName: String?,
)

@Serializable
data class PackageCheckpointMarkers(
    val buildingId: String,
    val schemaVersion: Int,
    val markers: List<PackageCheckpointMarker>,
)

@Serializable
data class PackageCheckpointMarker(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double,
    val rotationYDegrees: Double,
    val nearestNodeId: String,
    val physicalWidthMeters: Double,
    val physicalHeightMeters: Double,
    val referenceImageName: String?,
    val notes: String?,
)

@Serializable
data class PackageRouteRendering(
    val schemaVersion: Int = 1,
    val arrowSpacingMeters: Double,
    val lookaheadDistanceMeters: Double,
    val destinationThresholdMeters: Double,
    val turnMarkerThresholdDegrees: Double,
    val arrowHeightOffsetMeters: Double,
)
