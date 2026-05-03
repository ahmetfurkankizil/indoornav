package com.Vectura AI.core.ar

import com.Vectura AI.core.domain.NavGraph
import com.Vectura AI.core.domain.Route
import com.Vectura AI.core.domain.RouteRenderingConfig
import kotlin.math.*

/**
 * Converts a computed route into ordered arrow placements for AR rendering.
 *
 * Given a route (sequence of graph nodes) and rendering config, produces
 * a list of [ArrowPlacement]s spaced along the path at configured intervals.
 *
 * Algorithm:
 * 1. Walk each route segment (edge between consecutive nodes)
 * 2. Interpolate arrow positions at [RouteRenderingConfig.arrowSpacingMeters]
 * 3. At each node, check turn angle; insert turn marker if exceeds threshold
 * 4. Place destination marker at the final node
 * 5. Compute forward direction for each arrow
 */
class RouteToArrowMapper {

    /**
     * Map a route to renderable arrow placements.
     *
     * @param route Computed shortest-path route
     * @param graph Navigation graph (for node lookups)
     * @param config Rendering configuration
     * @param heightOffset Y offset above floor for arrows
     * @return Ordered list of arrow placements in building-local coords
     */
    fun map(
        route: Route,
        graph: NavGraph,
        config: RouteRenderingConfig,
    ): ArRenderableRoute {
        if (route.segments.isEmpty()) {
            return ArRenderableRoute(
                routeId = "${route.originNodeId}-${route.destinationNodeId}",
                arrows = emptyList(),
                totalDistanceMeters = 0.0,
                destinationName = route.destinationRoom?.name ?: "",
            )
        }

        val arrows = mutableListOf<ArrowPlacement>()
        var arrowIndex = 0
        var cumulativeDistance = 0.0
        val spacing = config.arrowSpacingMeters
        val turnThreshold = config.turnMarkerThresholdDegrees
        val yOffset = config.arrowHeightOffsetMeters

        // Collect all path nodes in order
        val pathNodeIds = mutableListOf(route.segments.first().fromNodeId)
        route.segments.forEach { pathNodeIds.add(it.toNodeId) }

        val pathNodes = pathNodeIds.mapNotNull { graph.nodeMap[it] }
        if (pathNodes.size < 2) {
            return ArRenderableRoute(
                routeId = "${route.originNodeId}-${route.destinationNodeId}",
                arrows = emptyList(),
                totalDistanceMeters = route.totalDistanceMeters,
                destinationName = route.destinationRoom?.name ?: "",
            )
        }

        // Walk each edge and interpolate arrows
        for (i in 0 until pathNodes.size - 1) {
            val from = pathNodes[i]
            val to = pathNodes[i + 1]

            val dx = to.x - from.x
            val dy = to.y - from.y
            val dz = to.z - from.z
            val edgeLength = sqrt(dx * dx + dy * dy + dz * dz)

            if (edgeLength < 0.001) continue

            // Normalized direction
            val ndx = dx / edgeLength
            val ndy = dy / edgeLength
            val ndz = dz / edgeLength

            // Check for turn at this node (except first node)
            if (i > 0) {
                val prevNode = pathNodes[i - 1]
                val turnAngle = computeTurnAngle(prevNode.x, prevNode.z, from.x, from.z, to.x, to.z)

                if (abs(turnAngle) >= turnThreshold) {
                    val turnType = when {
                        turnAngle > 135.0 -> ArrowType.U_TURN
                        turnAngle > 0 -> ArrowType.TURN_RIGHT
                        turnAngle < -135.0 -> ArrowType.U_TURN
                        else -> ArrowType.TURN_LEFT
                    }
                    val label = when (turnType) {
                        ArrowType.TURN_RIGHT -> "Turn right"
                        ArrowType.TURN_LEFT -> "Turn left"
                        ArrowType.U_TURN -> "U-turn"
                        else -> null
                    }
                    arrows.add(
                        ArrowPlacement(
                            id = "arrow-${arrowIndex++}",
                            positionX = from.x,
                            positionY = from.y + yOffset,
                            positionZ = from.z,
                            forwardDx = ndx,
                            forwardDy = ndy,
                            forwardDz = ndz,
                            type = turnType,
                            label = label,
                            distanceFromStartMeters = cumulativeDistance,
                        )
                    )
                }
            }

            // Interpolate follow arrows along this edge
            var walked = 0.0
            while (walked < edgeLength) {
                val t = walked / edgeLength
                val px = from.x + dx * t
                val py = from.y + dy * t
                val pz = from.z + dz * t

                arrows.add(
                    ArrowPlacement(
                        id = "arrow-${arrowIndex++}",
                        positionX = px,
                        positionY = py + yOffset,
                        positionZ = pz,
                        forwardDx = ndx,
                        forwardDy = ndy,
                        forwardDz = ndz,
                        type = ArrowType.FOLLOW,
                        distanceFromStartMeters = cumulativeDistance + walked,
                    )
                )

                walked += spacing
            }

            cumulativeDistance += edgeLength
        }

        // Destination marker at the last node
        val lastNode = pathNodes.last()
        val prevNode = pathNodes[pathNodes.size - 2]
        val fdx = lastNode.x - prevNode.x
        val fdz = lastNode.z - prevNode.z
        val fLen = sqrt(fdx * fdx + fdz * fdz).coerceAtLeast(0.001)

        arrows.add(
            ArrowPlacement(
                id = "arrow-dest",
                positionX = lastNode.x,
                positionY = lastNode.y + yOffset,
                positionZ = lastNode.z,
                forwardDx = fdx / fLen,
                forwardDy = 0.0,
                forwardDz = fdz / fLen,
                type = ArrowType.DESTINATION,
                label = route.destinationRoom?.name ?: "Destination",
                distanceFromStartMeters = cumulativeDistance,
            )
        )

        return ArRenderableRoute(
            routeId = "${route.originNodeId}-${route.destinationNodeId}",
            arrows = arrows,
            totalDistanceMeters = route.totalDistanceMeters,
            destinationName = route.destinationRoom?.name ?: "",
        )
    }

    /**
     * Compute the signed turn angle at a junction point.
     *
     * Positive = right turn, Negative = left turn.
     * Returns value in [-180, 180] degrees.
     */
    private fun computeTurnAngle(
        prevX: Double, prevZ: Double,
        currentX: Double, currentZ: Double,
        nextX: Double, nextZ: Double,
    ): Double {
        val inDx = currentX - prevX
        val inDz = currentZ - prevZ
        val outDx = nextX - currentX
        val outDz = nextZ - currentZ

        val inAngle = atan2(inDz, inDx)
        val outAngle = atan2(outDz, outDx)

        var turn = (outAngle - inAngle) * 180.0 / kotlin.math.PI
        if (turn > 180.0) turn -= 360.0
        if (turn < -180.0) turn += 360.0
        return turn
    }
}
