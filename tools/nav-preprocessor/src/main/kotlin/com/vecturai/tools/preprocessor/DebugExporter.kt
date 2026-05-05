package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.model.AuthoringConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

/**
 * Exports debug artifacts for visual validation.
 *
 * Produces:
 * - graph_debug.json: full graph with computed metadata
 * - plan_view_debug.svg: visual plan-view of nodes, edges, rooms, markers
 */
class DebugExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun export(config: AuthoringConfig, outputDir: String) {
        val outDir = File(outputDir)
        outDir.mkdirs()

        exportDebugJson(config, outDir)
        exportDebugSvg(config, outDir)
    }

    // ── graph_debug.json ────────────────────────────────────

    private fun exportDebugJson(config: AuthoringConfig, outDir: File) {
        val nodeMap = config.nodes.associateBy { it.id }
        val debug = DebugGraph(
            buildingId = config.buildingId,
            nodeCount = config.nodes.size,
            edgeCount = config.edges.size,
            roomCount = config.rooms.size,
            markerCount = config.entranceMarkers.size,
            nodes = config.nodes.map { n ->
                DebugNode(
                    id = n.id, x = n.x, y = n.y, z = n.z,
                    type = n.type, label = n.label,
                    connectedEdges = config.edges.count { it.from == n.id || it.to == n.id },
                )
            },
            edges = config.edges.map { e ->
                val fromNode = nodeMap[e.from]
                val toNode = nodeMap[e.to]
                val euclidean = if (fromNode != null && toNode != null) {
                    val dx = toNode.x - fromNode.x; val dy = toNode.y - fromNode.y; val dz = toNode.z - fromNode.z
                    kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                } else 0.0
                DebugEdge(
                    id = e.id, from = e.from, to = e.to,
                    cost = e.cost, euclideanDistance = euclidean,
                    bidirectional = e.bidirectional,
                )
            },
        )
        File(outDir, "graph_debug.json").writeText(json.encodeToString(debug))
    }

    // ── plan_view_debug.svg ─────────────────────────────────

    private fun exportDebugSvg(config: AuthoringConfig, outDir: File) {
        // Use X and Z for plan view (Y is up/elevation)
        val nodes = config.nodes
        if (nodes.isEmpty()) return

        val xs = nodes.map { it.x }
        val zs = nodes.map { it.z }
        val minX = xs.min(); val maxX = xs.max()
        val minZ = zs.min(); val maxZ = zs.max()

        val padding = 80.0
        val svgWidth = 800.0; val svgHeight = 600.0
        val drawW = svgWidth - 2 * padding
        val drawH = svgHeight - 2 * padding

        val rangeX = (maxX - minX).coerceAtLeast(1.0)
        val rangeZ = (maxZ - minZ).coerceAtLeast(1.0)
        val scale = minOf(drawW / rangeX, drawH / rangeZ)

        fun tx(x: Double) = padding + (x - minX) * scale
        fun tz(z: Double) = padding + (z - minZ) * scale

        val nodeMap = nodes.associateBy { it.id }
        val roomNodeIds = config.rooms.map { it.destinationNodeId }.toSet()
        val markerNodeIds = config.entranceMarkers.map { it.startNodeId }.toSet()

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="${svgWidth.roundToInt()}" height="${(svgHeight + 80).roundToInt()}" viewBox="0 0 ${svgWidth.roundToInt()} ${(svgHeight + 80).roundToInt()}">""")
        sb.appendLine("""<style>""")
        sb.appendLine("""  text { font-family: 'Inter', 'Helvetica', sans-serif; }""")
        sb.appendLine("""  .edge { stroke: #94a3b8; stroke-width: 1.5; }""")
        sb.appendLine("""  .node-circle { fill: #3b82f6; stroke: #1e40af; stroke-width: 1.5; }""")
        sb.appendLine("""  .room-circle { fill: #10b981; stroke: #065f46; stroke-width: 2; }""")
        sb.appendLine("""  .marker-circle { fill: #f59e0b; stroke: #92400e; stroke-width: 2; }""")
        sb.appendLine("""  .checkpoint-marker { fill: #8b5cf6; stroke: #5b21b6; stroke-width: 2; }""")
        sb.appendLine("""  .node-label { font-size: 10px; fill: #334155; }""")
        sb.appendLine("""  .room-label { font-size: 11px; fill: #065f46; font-weight: bold; }""")
        sb.appendLine("""  .title { font-size: 16px; fill: #1e293b; font-weight: bold; }""")
        sb.appendLine("""  .legend-text { font-size: 11px; fill: #475569; }""")
        sb.appendLine("""</style>""")

        // Background
        sb.appendLine("""<rect width="100%" height="100%" fill="#f8fafc" rx="8"/>""")

        // Title
        sb.appendLine("""<text x="20" y="30" class="title">${escapeXml(config.buildingName)} — Plan View Debug</text>""")
        sb.appendLine("""<text x="20" y="48" class="legend-text">${config.nodes.size} nodes, ${config.edges.size} edges, ${config.rooms.size} rooms, ${config.checkpointMarkers.size} checkpoints</text>""")

        // Translate content down for title
        sb.appendLine("""<g transform="translate(0, 30)">""")

        // Edges
        for (edge in config.edges) {
            val from = nodeMap[edge.from] ?: continue
            val to = nodeMap[edge.to] ?: continue
            sb.appendLine("""<line x1="${tx(from.x).f()}" y1="${tz(from.z).f()}" x2="${tx(to.x).f()}" y2="${tz(to.z).f()}" class="edge"/>""")
        }

        // Nodes
        for (node in nodes) {
            val cx = tx(node.x).f()
            val cy = tz(node.z).f()
            val cssClass = when {
                node.id in markerNodeIds -> "marker-circle"
                node.id in roomNodeIds -> "room-circle"
                else -> "node-circle"
            }
            val radius = if (node.id in roomNodeIds || node.id in markerNodeIds) 7 else 5
            sb.appendLine("""<circle cx="$cx" cy="$cy" r="$radius" class="$cssClass"/>""")

            // Label
            val label = node.label ?: node.id
            val labelClass = if (node.id in roomNodeIds) "room-label" else "node-label"
            sb.appendLine("""<text x="$cx" y="${(tz(node.z) - 10).f()}" text-anchor="middle" class="$labelClass">${escapeXml(label)}</text>""")
        }

        // Room labels (use room display name instead of node ID)
        for (room in config.rooms) {
            val node = nodeMap[room.destinationNodeId] ?: continue
            val cx = tx(node.x).f()
            val cy = (tz(node.z) + 18).f()
            sb.appendLine("""<text x="$cx" y="$cy" text-anchor="middle" class="room-label">${escapeXml(room.displayName)}</text>""")
        }

        // Marker labels
        for (marker in config.entranceMarkers) {
            val node = nodeMap[marker.startNodeId] ?: continue
            val cx = tx(node.x).f()
            val cy = (tz(node.z) + 18).f()
            sb.appendLine("""<text x="$cx" y="$cy" text-anchor="middle" style="font-size:10px;fill:#92400e;">⬟ ${escapeXml(marker.id)}</text>""")
        }

        // Checkpoint markers (purple diamonds)
        for (cp in config.checkpointMarkers) {
            val cpX = tx(cp.position.x).f()
            val cpZ = tz(cp.position.z).f()
            val cpXd = tx(cp.position.x)
            val cpZd = tz(cp.position.z)
            val s = 8.0  // diamond half-size
            val diamondPoints = "${cpXd},${cpZd - s} ${cpXd + s},${cpZd} ${cpXd},${cpZd + s} ${cpXd - s},${cpZd}"
            sb.appendLine("""<polygon points="$diamondPoints" class="checkpoint-marker"/>""")
            sb.appendLine("""<text x="$cpX" y="${(cpZd + 18).f()}" text-anchor="middle" style="font-size:10px;fill:#5b21b6;">◆ ${escapeXml(cp.id)}</text>""")
        }

        sb.appendLine("""</g>""")

        // Legend
        val ly = svgHeight + 20
        sb.appendLine("""<g transform="translate(20, ${ly.roundToInt()})">""")
        sb.appendLine("""<circle cx="8" cy="8" r="5" class="node-circle"/>""")
        sb.appendLine("""<text x="20" y="12" class="legend-text">Waypoint</text>""")
        sb.appendLine("""<circle cx="108" cy="8" r="7" class="room-circle"/>""")
        sb.appendLine("""<text x="120" y="12" class="legend-text">Room Entry</text>""")
        sb.appendLine("""<circle cx="218" cy="8" r="7" class="marker-circle"/>""")
        sb.appendLine("""<text x="230" y="12" class="legend-text">Entrance Marker</text>""")
        sb.appendLine("""<polygon points="318,1 325,8 318,15 311,8" class="checkpoint-marker"/>""")
        sb.appendLine("""<text x="332" y="12" class="legend-text">Checkpoint</text>""")
        sb.appendLine("""<line x1="428" y1="8" x2="458" y2="8" class="edge"/>""")
        sb.appendLine("""<text x="465" y="12" class="legend-text">Edge</text>""")
        sb.appendLine("""</g>""")

        sb.appendLine("""</svg>""")

        File(outDir, "plan_view_debug.svg").writeText(sb.toString())
    }

    private fun Double.f() = "%.2f".format(this)

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")



    fun exportDraftDebug(
        grid: com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator.OccupancyGrid,
        zones: List<com.vecturai.tools.preprocessor.analysis.ZoneSuggester.Zone>,
        draftGraph: com.vecturai.tools.preprocessor.draft.NavigationGraphDrafter.DraftNavGraph,
        floorEstimate: com.vecturai.tools.preprocessor.analysis.FloorPlaneEstimator.FloorEstimate,
        geometry: com.vecturai.tools.preprocessor.glb.GlbGeometryExtractor.GeometryResult,
        outputDir: String,
    ) {
        val outDir = File(outputDir)
        outDir.mkdirs()
        exportOccupancySvg(grid, zones, outDir)
        exportDraftGraphSvg(grid, zones, draftGraph, outDir)
        exportGeometryStats(grid, zones, draftGraph, floorEstimate, geometry, outDir)
    }

    private fun exportOccupancySvg(
        grid: com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator.OccupancyGrid,
        zones: List<com.vecturai.tools.preprocessor.analysis.ZoneSuggester.Zone>,
        outDir: File,
    ) {
        val cellPx = 6
        val padding = 40
        val svgW = grid.width * cellPx + 2 * padding
        val svgH = grid.height * cellPx + 2 * padding + 60

        // Assign colors to zones
        val zoneColors = listOf("#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16")
        val cellZoneMap = mutableMapOf<Pair<Int, Int>, Int>()
        zones.forEachIndexed { idx, zone ->
            for (cell in zone.cells) cellZoneMap[cell] = idx
        }

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$svgW" height="$svgH">""")
        sb.appendLine("""<rect width="100%" height="100%" fill="#1e1e2e" rx="8"/>""")
        sb.appendLine("""<text x="20" y="28" fill="#cdd6f4" font-family="Inter,sans-serif" font-size="14" font-weight="bold">Occupancy Grid — ${grid.width}×${grid.height} cells (${grid.cellSize}m)</text>""")

        sb.appendLine("""<g transform="translate($padding, 40)">""")
        for (row in 0 until grid.height) {
            for (col in 0 until grid.width) {
                val x = col * cellPx
                val y = row * cellPx
                val fill = if (grid.cells[row][col] == com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator.OCCUPIED) {
                    val zoneIdx = cellZoneMap[Pair(col, row)]
                    if (zoneIdx != null) zoneColors[zoneIdx % zoneColors.size] else "#6c7086"
                } else "#313244"
                sb.appendLine("""<rect x="$x" y="$y" width="$cellPx" height="$cellPx" fill="$fill" stroke="#1e1e2e" stroke-width="0.5"/>""")
            }
        }
        sb.appendLine("</g>")

        // Legend
        val ly = grid.height * cellPx + 50
        sb.appendLine("""<g transform="translate(20, $ly)">""")
        zones.forEachIndexed { idx, zone ->
            val lx = idx * 120
            val color = zoneColors[idx % zoneColors.size]
            sb.appendLine("""<rect x="$lx" y="0" width="12" height="12" fill="$color" rx="2"/>""")
            sb.appendLine("""<text x="${lx + 16}" y="10" fill="#cdd6f4" font-family="Inter,sans-serif" font-size="10">${escapeXml(zone.label)} (${zone.cellCount})</text>""")
        }
        sb.appendLine("</g>")
        sb.appendLine("</svg>")

        File(outDir, "occupancy_debug.svg").writeText(sb.toString())
    }

    private fun exportDraftGraphSvg(
        grid: com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator.OccupancyGrid,
        zones: List<com.vecturai.tools.preprocessor.analysis.ZoneSuggester.Zone>,
        draftGraph: com.vecturai.tools.preprocessor.draft.NavigationGraphDrafter.DraftNavGraph,
        outDir: File,
    ) {
        if (draftGraph.nodes.isEmpty()) return

        val padding = 80.0
        val svgWidth = 800.0
        val svgHeight = 600.0
        val drawW = svgWidth - 2 * padding
        val drawH = svgHeight - 2 * padding

        val xs = draftGraph.nodes.map { it.x }
        val zs = draftGraph.nodes.map { it.z }
        val minX = xs.min(); val maxX = xs.max()
        val minZ = zs.min(); val maxZ = zs.max()
        val rangeX = (maxX - minX).coerceAtLeast(1.0)
        val rangeZ = (maxZ - minZ).coerceAtLeast(1.0)
        val scale = minOf(drawW / rangeX, drawH / rangeZ)

        fun tx(x: Double) = padding + (x - minX) * scale
        fun tz(z: Double) = padding + (z - minZ) * scale

        val nodeMap = draftGraph.nodes.associateBy { it.id }

        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="${svgWidth.roundToInt()}" height="${(svgHeight + 80).roundToInt()}">""")
        sb.appendLine("""<rect width="100%" height="100%" fill="#1e1e2e" rx="8"/>""")
        sb.appendLine("""<text x="20" y="30" fill="#cdd6f4" font-family="Inter,sans-serif" font-size="16" font-weight="bold">Draft Navigation Graph — ${draftGraph.nodes.size} nodes, ${draftGraph.edges.size} edges</text>""")
        sb.appendLine("""<text x="20" y="50" fill="#a6adc8" font-family="Inter,sans-serif" font-size="11">⚠ DRAFT — review and edit before use</text>""")

        sb.appendLine("""<g transform="translate(0, 30)">""")

        // Edges
        for (edge in draftGraph.edges) {
            val from = nodeMap[edge.from] ?: continue
            val to = nodeMap[edge.to] ?: continue
            sb.appendLine("""<line x1="${tx(from.x).f()}" y1="${tz(from.z).f()}" x2="${tx(to.x).f()}" y2="${tz(to.z).f()}" stroke="#94a3b8" stroke-width="2" stroke-dasharray="6,3"/>""")
            // Edge cost label
            val mx = ((tx(from.x) + tx(to.x)) / 2).f()
            val my = ((tz(from.z) + tz(to.z)) / 2).f()
            sb.appendLine("""<text x="$mx" y="$my" text-anchor="middle" fill="#94a3b8" font-family="Inter,sans-serif" font-size="9">${edge.cost}m</text>""")
        }

        // Nodes
        for (node in draftGraph.nodes) {
            val cx = tx(node.x).f()
            val cy = tz(node.z).f()
            val (fill, stroke, radius) = when (node.type) {
                "room_entry" -> Triple("#10b981", "#065f46", 7)
                "junction" -> Triple("#3b82f6", "#1e40af", 6)
                else -> Triple("#f59e0b", "#92400e", 5)
            }
            sb.appendLine("""<circle cx="$cx" cy="$cy" r="$radius" fill="$fill" stroke="$stroke" stroke-width="2"/>""")

            val label = node.label ?: node.id
            sb.appendLine("""<text x="$cx" y="${(tz(node.z) - 12).f()}" text-anchor="middle" fill="#cdd6f4" font-family="Inter,sans-serif" font-size="10">${escapeXml(label)}</text>""")
        }

        sb.appendLine("</g>")
        sb.appendLine("</svg>")

        File(outDir, "draft_graph_debug.svg").writeText(sb.toString())
    }

    @Serializable
    data class GeometryStats(
        val totalVertices: Int,
        val meshCount: Int,
        val primitiveCount: Int,
        val boundingBox: BoundsInfo?,
        val floorEstimate: FloorInfo,
        val occupancyGrid: GridInfo,
        val zones: List<ZoneInfo>,
        val draftGraph: GraphInfo,
    )

    @Serializable
    data class BoundsInfo(
        val minX: Float, val minY: Float, val minZ: Float,
        val maxX: Float, val maxY: Float, val maxZ: Float,
        val extentX: Float, val extentY: Float, val extentZ: Float,
    )

    @Serializable
    data class FloorInfo(
        val floorY: Double,
        val confidence: Double,
        val floorVertexCount: Int,
    )

    @Serializable
    data class GridInfo(
        val width: Int,
        val height: Int,
        val cellSize: Double,
        val occupiedCells: Int,
    )

    @Serializable
    data class ZoneInfo(
        val id: String,
        val label: String,
        val cellCount: Int,
        val centroidX: Double,
        val centroidZ: Double,
        val confidence: String,
    )

    @Serializable
    data class GraphInfo(
        val nodeCount: Int,
        val edgeCount: Int,
    )

    private fun exportGeometryStats(
        grid: com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator.OccupancyGrid,
        zones: List<com.vecturai.tools.preprocessor.analysis.ZoneSuggester.Zone>,
        draftGraph: com.vecturai.tools.preprocessor.draft.NavigationGraphDrafter.DraftNavGraph,
        floorEstimate: com.vecturai.tools.preprocessor.analysis.FloorPlaneEstimator.FloorEstimate,
        geometry: com.vecturai.tools.preprocessor.glb.GlbGeometryExtractor.GeometryResult,
        outDir: File,
    ) {
        val stats = GeometryStats(
            totalVertices = geometry.vertices.size,
            meshCount = geometry.meshCount,
            primitiveCount = geometry.primitiveCount,
            boundingBox = geometry.boundingBox?.let { bb ->
                BoundsInfo(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, bb.extentX, bb.extentY, bb.extentZ)
            },
            floorEstimate = FloorInfo(
                floorY = floorEstimate.floorY,
                confidence = floorEstimate.confidence,
                floorVertexCount = floorEstimate.floorVertexCount,
            ),
            occupancyGrid = GridInfo(
                width = grid.width,
                height = grid.height,
                cellSize = grid.cellSize,
                occupiedCells = grid.occupiedCount,
            ),
            zones = zones.map { z ->
                ZoneInfo(z.id, z.label, z.cellCount, z.centroidX, z.centroidZ, z.confidence)
            },
            draftGraph = GraphInfo(draftGraph.nodes.size, draftGraph.edges.size),
        )
        File(outDir, "geometry_stats.json").writeText(json.encodeToString(stats))
    }
}

// ── Debug DTOs ──

@Serializable
data class DebugGraph(
    val buildingId: String,
    val nodeCount: Int,
    val edgeCount: Int,
    val roomCount: Int,
    val markerCount: Int,
    val nodes: List<DebugNode>,
    val edges: List<DebugEdge>,
)

@Serializable
data class DebugNode(
    val id: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val type: String,
    val label: String?,
    val connectedEdges: Int,
)

@Serializable
data class DebugEdge(
    val id: String,
    val from: String,
    val to: String,
    val cost: Double,
    val euclideanDistance: Double,
    val bidirectional: Boolean,
)
