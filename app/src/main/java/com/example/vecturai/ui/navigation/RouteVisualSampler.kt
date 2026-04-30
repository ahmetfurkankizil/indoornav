package com.example.vecturai.ui.navigation

import com.example.vecturai.ar.Vec3
import com.example.vecturai.ar.distanceMeters
import com.example.vecturai.ar.horizontalDistanceMeters
import com.example.vecturai.graph.EdgeKind
import kotlin.math.abs
import kotlin.math.atan2

enum class RouteTurnDirection {
    LEFT,
    RIGHT
}

data class RouteVisualDistances(
    val distanceToNextMeters: Float?,
    val distanceToDestinationMeters: Float?
)

object RouteVisualSampler {
    fun projectToPath(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>,
        userPosition: Vec3,
        currentFloor: Int
    ): RouteProjection? {
        val segments = buildSegments(nodes, edges)
        val userHorizontal = userPosition.horizontal()

        var bestSegment: RouteSegment? = null
        var bestT = 0f
        var bestDistance = Float.MAX_VALUE
        for (segment in segments) {
            if (!segment.isFloorCorridor(currentFloor)) continue

            val a = segment.from.position.horizontal()
            val b = segment.to.position.horizontal()
            val ab = b - a
            val len2 = ab.x * ab.x + ab.z * ab.z
            if (len2 < MIN_SEGMENT_LENGTH_M * MIN_SEGMENT_LENGTH_M) continue

            val fromA = userHorizontal - a
            val t = ((fromA.x * ab.x + fromA.z * ab.z) / len2).coerceIn(0f, 1f)
            val projected = a + ab * t
            val distance = (userHorizontal - projected).length()
            if (distance < bestDistance) {
                bestDistance = distance
                bestSegment = segment
                bestT = t
            }
        }

        val segment = bestSegment ?: return null
        return RouteProjection(
            segmentIndex = segment.index,
            segmentT = bestT,
            cumulativeMeters = segment.cumulativeStartMeters + segment.lengthMeters * bestT,
            perpDist = bestDistance
        )
    }

    fun sample(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>,
        projection: RouteProjection,
        currentFloor: Int,
        floorHeightEstimate: FloorHeightEstimate
    ): RouteVisualState {
        val segments = buildSegments(nodes, edges)
        if (segments.isEmpty()) return RouteVisualState.Empty

        val totalLength = segments.last().cumulativeEndMeters
        val transition = firstTransitionAfter(segments, projection, currentFloor)
        val stopDistance = minOf(transition?.cumulativeStartMeters ?: totalLength, totalLength)
        val arrowY = floorHeightEstimate.yMeters + FLOOR_ARROW_Y_OFFSET_M
        val arrows = mutableListOf<FloorArrowPose>()

        for (sampleIndex in 0 until FLOOR_ARROW_COUNT) {
            val distanceAhead = FLOOR_ARROW_START_AHEAD_M + sampleIndex * FLOOR_ARROW_SPACING_M
            if (distanceAhead > FLOOR_ARROW_MAX_DISTANCE_M) break

            var sampleDistance = projection.cumulativeMeters + distanceAhead
            if (sampleDistance > stopDistance) {
                sampleDistance = if (
                    sampleIndex == 0 &&
                    stopDistance > projection.cumulativeMeters + MIN_REMAINING_SAMPLE_M
                ) {
                    stopDistance
                } else {
                    break
                }
            }

            val segment = segmentAtDistance(segments, sampleDistance) ?: break
            if (!segment.isFloorCorridor(currentFloor)) break

            val t = ((sampleDistance - segment.cumulativeStartMeters) / segment.lengthMeters)
                .coerceIn(0f, 1f)
            val position = segment.positionAt(t, arrowY)
            val distanceBeforeSegmentEnd = (segment.cumulativeEndMeters - sampleDistance).coerceAtLeast(0f)
            val turnBoost = if (hasUpcomingTurn(segments, segment, currentFloor, distanceBeforeSegmentEnd)) {
                0.12f
            } else {
                0f
            }

            arrows += FloorArrowPose(
                sampleIndex = sampleIndex,
                position = position,
                yawDegrees = segment.yawDegrees(),
                alpha = (1f - distanceAhead / FLOOR_ARROW_MAX_DISTANCE_M + turnBoost).coerceIn(0.24f, 1f),
                scale = (1.08f - sampleIndex * 0.035f + turnBoost).coerceIn(0.82f, 1.18f),
                distanceAheadMeters = distanceAhead,
                segmentIndex = segment.index
            )
        }

        val pathSegments = floorPathSegments(
            segments = segments,
            projection = projection,
            currentFloor = currentFloor,
            floorY = arrowY,
            stopDistance = stopDistance
        )

        return RouteVisualState(
            arrows = arrows,
            segments = pathSegments,
            transitionCue = transition?.toCue(arrowY)
        )
    }

    fun distances(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>,
        projection: RouteProjection,
        currentFloor: Int
    ): RouteVisualDistances {
        val segments = buildSegments(nodes, edges)
        if (segments.isEmpty()) {
            return RouteVisualDistances(null, null)
        }

        val totalLength = segments.last().cumulativeEndMeters
        val distanceToDestination = (totalLength - projection.cumulativeMeters).coerceAtLeast(0f)
        val distanceToNext = distanceToNextRouteEvent(segments, projection, currentFloor)
            ?: distanceToDestination
        return RouteVisualDistances(
            distanceToNextMeters = distanceToNext.coerceAtLeast(0f),
            distanceToDestinationMeters = distanceToDestination
        )
    }

    fun turnDirectionNearProjection(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>,
        projection: RouteProjection,
        currentFloor: Int,
        thresholdMeters: Float = TURN_PROMPT_DISTANCE_M
    ): RouteTurnDirection? {
        val segments = buildSegments(nodes, edges)
        val current = segments.firstOrNull { it.index == projection.segmentIndex }
            ?: return null
        if (!current.isFloorCorridor(currentFloor)) return null
        val next = segments.firstOrNull { it.index == current.index + 1 }
            ?: return null
        if (!next.isFloorCorridor(currentFloor)) return null

        val distanceToCorner = current.cumulativeEndMeters - projection.cumulativeMeters
        if (distanceToCorner !in 0f..thresholdMeters) return null

        val yawDelta = shortestYawDelta(current.yawDegrees(), next.yawDegrees())
        if (abs(yawDelta) < TURN_PROMPT_MIN_DEG) return null
        return if (yawDelta > 0f) RouteTurnDirection.RIGHT else RouteTurnDirection.LEFT
    }

    fun transitionCue(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>,
        projection: RouteProjection,
        currentFloor: Int,
        floorY: Float
    ): RouteTransitionCue? =
        firstTransitionAfter(buildSegments(nodes, edges), projection, currentFloor)?.toCue(floorY)

    private fun floorPathSegments(
        segments: List<RouteSegment>,
        projection: RouteProjection,
        currentFloor: Int,
        floorY: Float,
        stopDistance: Float
    ): List<FloorPathSegment> {
        val result = mutableListOf<FloorPathSegment>()
        for (segment in segments) {
            if (segment.cumulativeEndMeters <= projection.cumulativeMeters) continue
            if (segment.cumulativeStartMeters >= stopDistance) break
            if (!segment.isFloorCorridor(currentFloor)) break

            val fromDistance = maxOf(segment.cumulativeStartMeters, projection.cumulativeMeters)
            val toDistance = minOf(segment.cumulativeEndMeters, stopDistance)
            if (toDistance - fromDistance <= MIN_REMAINING_SAMPLE_M) continue
            val fromT = ((fromDistance - segment.cumulativeStartMeters) / segment.lengthMeters)
                .coerceIn(0f, 1f)
            val toT = ((toDistance - segment.cumulativeStartMeters) / segment.lengthMeters)
                .coerceIn(0f, 1f)
            result += FloorPathSegment(
                from = segment.positionAt(fromT, floorY),
                to = segment.positionAt(toT, floorY),
                alpha = 0.28f,
                segmentIndex = segment.index
            )
        }
        return result
    }

    private fun distanceToNextRouteEvent(
        segments: List<RouteSegment>,
        projection: RouteProjection,
        currentFloor: Int
    ): Float? {
        val transition = firstTransitionAfter(segments, projection, currentFloor)
        val current = segments.firstOrNull { it.index == projection.segmentIndex }
        val distanceToSegmentEnd = current
            ?.takeIf { it.cumulativeEndMeters > projection.cumulativeMeters }
            ?.let { it.cumulativeEndMeters - projection.cumulativeMeters }
        val distanceToTransition = transition?.let {
            it.cumulativeStartMeters - projection.cumulativeMeters
        }
        return listOfNotNull(distanceToSegmentEnd, distanceToTransition)
            .filter { it >= 0f }
            .minOrNull()
    }

    private fun firstTransitionAfter(
        segments: List<RouteSegment>,
        projection: RouteProjection,
        currentFloor: Int
    ): RouteSegment? =
        segments.firstOrNull { segment ->
            segment.cumulativeEndMeters > projection.cumulativeMeters + MIN_REMAINING_SAMPLE_M &&
                segment.from.floor == currentFloor &&
                segment.isTransition
        }

    private fun segmentAtDistance(
        segments: List<RouteSegment>,
        distanceMeters: Float
    ): RouteSegment? =
        segments.firstOrNull {
            distanceMeters <= it.cumulativeEndMeters + MIN_REMAINING_SAMPLE_M
        } ?: segments.lastOrNull()

    private fun hasUpcomingTurn(
        segments: List<RouteSegment>,
        segment: RouteSegment,
        currentFloor: Int,
        distanceBeforeSegmentEnd: Float
    ): Boolean {
        if (distanceBeforeSegmentEnd > TURN_BOOST_DISTANCE_M) return false
        val next = segments.firstOrNull { it.index == segment.index + 1 } ?: return false
        if (!next.isFloorCorridor(currentFloor)) return false
        return abs(shortestYawDelta(segment.yawDegrees(), next.yawDegrees())) >= TURN_PROMPT_MIN_DEG
    }

    private fun buildSegments(
        nodes: List<RouteSampleNode>,
        edges: List<RouteSampleEdge>
    ): List<RouteSegment> {
        if (nodes.size < 2) return emptyList()
        val result = mutableListOf<RouteSegment>()
        var cumulative = 0f
        for (index in 0 until nodes.lastIndex) {
            val from = nodes[index]
            val to = nodes[index + 1]
            val kind = edges.edgeKindBetween(from.id, to.id) ?: EdgeKind.STAIRS
            val length = if (kind == EdgeKind.CORRIDOR && from.floor == to.floor) {
                horizontalDistanceMeters(from.position, to.position)
            } else {
                distanceMeters(from.position, to.position)
            }
            if (length < MIN_SEGMENT_LENGTH_M) continue
            result += RouteSegment(
                index = index,
                from = from,
                to = to,
                kind = kind,
                lengthMeters = length,
                cumulativeStartMeters = cumulative
            )
            cumulative += length
        }
        return result
    }

    private fun List<RouteSampleEdge>.edgeKindBetween(fromId: String, toId: String): EdgeKind? =
        firstOrNull {
            it.fromId == fromId && it.toId == toId ||
                it.fromId == toId && it.toId == fromId
        }?.kind

    private fun shortestYawDelta(fromDegrees: Float, toDegrees: Float): Float {
        var delta = (toDegrees - fromDegrees) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private data class RouteSegment(
        val index: Int,
        val from: RouteSampleNode,
        val to: RouteSampleNode,
        val kind: EdgeKind,
        val lengthMeters: Float,
        val cumulativeStartMeters: Float
    ) {
        val cumulativeEndMeters: Float = cumulativeStartMeters + lengthMeters
        val isTransition: Boolean =
            kind != EdgeKind.CORRIDOR || from.floor != to.floor

        fun isFloorCorridor(currentFloor: Int): Boolean =
            kind == EdgeKind.CORRIDOR && from.floor == currentFloor && to.floor == currentFloor

        fun positionAt(t: Float, y: Float): Vec3 {
            val interpolated = from.position + (to.position - from.position) * t
            return Vec3(interpolated.x, y, interpolated.z)
        }

        fun yawDegrees(): Float {
            val delta = to.position - from.position
            return Math.toDegrees(atan2(delta.x.toDouble(), delta.z.toDouble())).toFloat()
        }

        fun toCue(floorY: Float): RouteTransitionCue {
            val cuePosition = Vec3(from.position.x, floorY, from.position.z)
            return RouteTransitionCue(
                kind = kind,
                fromFloor = from.floor,
                toFloor = to.floor,
                position = cuePosition
            )
        }
    }

    private const val MIN_SEGMENT_LENGTH_M = 1e-3f
    private const val MIN_REMAINING_SAMPLE_M = 0.05f
    private const val TURN_PROMPT_DISTANCE_M = 1.6f
    private const val TURN_PROMPT_MIN_DEG = 35f
    private const val TURN_BOOST_DISTANCE_M = 0.7f
}
