package com.vecturai.tools.preprocessor.draft

import com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator
import com.vecturai.tools.preprocessor.analysis.ZoneSuggester
import kotlin.math.sqrt

/**
 * Generates a draft navigation graph from discovered zones.
 *
 * Strategy:
 * - Place a node at each zone centroid
 * - For the largest zone, add extra waypoints along its longest axis
 * - Connect zones that are spatially adjacent
 * - Edge costs = Euclidean distance between nodes
 */
class NavigationGraphDrafter(
    /** Maximum distance between zone centroids to create an edge. */
    private val adjacencyThreshold: Double = 8.0,
    /** For large zones, spacing between intermediate waypoints. */
    private val waypointSpacing: Double = 3.0,
) {

    data class DraftNode(
        val id: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val type: String,
        val label: String?,
        /** Which zone this node belongs to (null for inter-zone waypoints). */
        val zoneId: String?,
    )

    data class DraftEdge(
        val id: String,
        val from: String,
        val to: String,
        val cost: Double,
        val bidirectional: Boolean = true,
    )

    data class DraftNavGraph(
        val nodes: List<DraftNode>,
        val edges: List<DraftEdge>,
    )

    /**
     * Generate a draft navigation graph from zones and the occupancy grid.
     *
     * @param zones Discovered zones (sorted largest first)
     * @param grid The occupancy grid (for spatial reference)
     * @param floorY The estimated floor Y coordinate
     * @return DraftNavGraph with nodes and edges
     */
    fun draft(
        zones: List<ZoneSuggester.Zone>,
        grid: OccupancyGridGenerator.OccupancyGrid,
        floorY: Double,
    ): DraftNavGraph {
        if (zones.isEmpty()) {
            return DraftNavGraph(emptyList(), emptyList())
        }

        val allNodes = mutableListOf<DraftNode>()
        val allEdges = mutableListOf<DraftEdge>()
        var nodeCounter = 1
        var edgeCounter = 1

        // Map zoneId → list of node IDs in that zone
        val zoneNodeIds = mutableMapOf<String, MutableList<String>>()

        for ((zoneIndex, zone) in zones.withIndex()) {
            val isLargest = zoneIndex == 0

            // Place centroid node
            val centroidNodeId = "n%02d".format(nodeCounter++)
            val nodeType = if (isLargest) "junction" else "room_entry"
            allNodes.add(
                DraftNode(
                    id = centroidNodeId,
                    x = zone.centroidX,
                    y = floorY,
                    z = zone.centroidZ,
                    type = nodeType,
                    label = zone.label,
                    zoneId = zone.id,
                )
            )
            zoneNodeIds.getOrPut(zone.id) { mutableListOf() }.add(centroidNodeId)

            // For the largest zone, add intermediate waypoints along its extent
            if (isLargest && zone.cellCount > 16) {
                val waypoints = generateWaypoints(zone, grid, floorY, nodeCounter)
                if (waypoints.isNotEmpty()) {
                    nodeCounter += waypoints.size
                    allNodes.addAll(waypoints)
                    zoneNodeIds.getOrPut(zone.id) { mutableListOf() }
                        .addAll(waypoints.map { it.id })

                    // Connect waypoints in chain: centroid → w1 → w2 → ...
                    var prevId = centroidNodeId
                    for (wp in waypoints) {
                        val prevNode = allNodes.first { it.id == prevId }
                        val dist = euclidean(prevNode.x, prevNode.z, wp.x, wp.z)
                        allEdges.add(
                            DraftEdge(
                                id = "e%02d".format(edgeCounter++),
                                from = prevId,
                                to = wp.id,
                                cost = roundCost(dist),
                            )
                        )
                        prevId = wp.id
                    }
                }
            }
        }

        // Connect zones that are spatially adjacent
        for (i in zones.indices) {
            for (j in i + 1 until zones.size) {
                val zi = zones[i]
                val zj = zones[j]
                val dist = euclidean(zi.centroidX, zi.centroidZ, zj.centroidX, zj.centroidZ)

                if (dist <= adjacencyThreshold) {
                    // Find the closest node pair between the two zones
                    val nodesI = zoneNodeIds[zi.id] ?: continue
                    val nodesJ = zoneNodeIds[zj.id] ?: continue

                    var bestDist = Double.MAX_VALUE
                    var bestFrom = nodesI.first()
                    var bestTo = nodesJ.first()

                    for (niId in nodesI) {
                        for (njId in nodesJ) {
                            val ni = allNodes.first { it.id == niId }
                            val nj = allNodes.first { it.id == njId }
                            val d = euclidean(ni.x, ni.z, nj.x, nj.z)
                            if (d < bestDist) {
                                bestDist = d
                                bestFrom = niId
                                bestTo = njId
                            }
                        }
                    }

                    allEdges.add(
                        DraftEdge(
                            id = "e%02d".format(edgeCounter++),
                            from = bestFrom,
                            to = bestTo,
                            cost = roundCost(bestDist),
                        )
                    )
                }
            }
        }

        return DraftNavGraph(nodes = allNodes, edges = allEdges)
    }

    /**
     * Generate intermediate waypoints along the longest axis of a zone.
     */
    private fun generateWaypoints(
        zone: ZoneSuggester.Zone,
        grid: OccupancyGridGenerator.OccupancyGrid,
        floorY: Double,
        startCounter: Int,
    ): List<DraftNode> {
        // Find bounding box of zone cells in world coords
        var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
        var minZ = Double.MAX_VALUE; var maxZ = -Double.MAX_VALUE

        for ((col, row) in zone.cells) {
            val (wx, wz) = grid.cellToWorld(col, row)
            if (wx < minX) minX = wx; if (wx > maxX) maxX = wx
            if (wz < minZ) minZ = wz; if (wz > maxZ) maxZ = wz
        }

        val extentX = maxX - minX
        val extentZ = maxZ - minZ
        val waypoints = mutableListOf<DraftNode>()
        var counter = startCounter

        // Sample along the longer axis
        if (extentX > extentZ && extentX > waypointSpacing * 2) {
            // Sample along X axis at zone centroid Z
            val steps = (extentX / waypointSpacing).toInt().coerceAtMost(10)
            for (s in 1..steps) {
                val t = s.toDouble() / (steps + 1)
                val wx = minX + t * extentX
                waypoints.add(
                    DraftNode(
                        id = "n%02d".format(counter++),
                        x = roundCoord(wx),
                        y = floorY,
                        z = roundCoord(zone.centroidZ),
                        type = "junction",
                        label = "${zone.label} Waypoint $s",
                        zoneId = zone.id,
                    )
                )
            }
        } else if (extentZ > waypointSpacing * 2) {
            // Sample along Z axis at zone centroid X
            val steps = (extentZ / waypointSpacing).toInt().coerceAtMost(10)
            for (s in 1..steps) {
                val t = s.toDouble() / (steps + 1)
                val wz = minZ + t * extentZ
                waypoints.add(
                    DraftNode(
                        id = "n%02d".format(counter++),
                        x = roundCoord(zone.centroidX),
                        y = floorY,
                        z = roundCoord(wz),
                        type = "junction",
                        label = "${zone.label} Waypoint $s",
                        zoneId = zone.id,
                    )
                )
            }
        }

        return waypoints
    }

    private fun euclidean(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        val dx = x2 - x1; val dz = z2 - z1
        return sqrt(dx * dx + dz * dz)
    }

    private fun roundCost(d: Double): Double = "%.2f".format(d).toDouble()
    private fun roundCoord(d: Double): Double = "%.2f".format(d).toDouble()
}
