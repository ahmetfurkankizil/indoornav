package com.example.vecturai.ui.navigation

import com.example.vecturai.ar.Vec3
import com.example.vecturai.graph.EdgeKind

const val FLOOR_ARROW_SPACING_M = 1.0f
const val FLOOR_ARROW_START_AHEAD_M = 0.8f
const val FLOOR_ARROW_MAX_DISTANCE_M = 9.0f
const val FLOOR_ARROW_COUNT = 7
const val FLOOR_ARROW_RENDER_POOL_SIZE = FLOOR_ARROW_COUNT
const val FLOOR_ARROW_Y_OFFSET_M = 0.02f
const val DEFAULT_CAMERA_TO_FLOOR_M = 1.35f
const val ROUTE_PROJECTION_DEADBAND_M = 0.08f

data class FloorArrowPose(
    val sampleIndex: Int,
    val position: Vec3,
    val yawDegrees: Float,
    val alpha: Float,
    val scale: Float,
    val distanceAheadMeters: Float,
    val segmentIndex: Int
)

data class FloorPathSegment(
    val from: Vec3,
    val to: Vec3,
    val alpha: Float,
    val segmentIndex: Int
)

data class RouteVisualState(
    val arrows: List<FloorArrowPose>,
    val segments: List<FloorPathSegment>,
    val transitionCue: RouteTransitionCue?
) {
    companion object {
        val Empty = RouteVisualState(
            arrows = emptyList(),
            segments = emptyList(),
            transitionCue = null
        )
    }
}

data class RouteTransitionCue(
    val kind: EdgeKind,
    val fromFloor: Int,
    val toFloor: Int,
    val position: Vec3?
)

data class RouteSampleNode(
    val id: String,
    val position: Vec3,
    val floor: Int
)

data class RouteSampleEdge(
    val fromId: String,
    val toId: String,
    val kind: EdgeKind
)

data class RouteProjection(
    val segmentIndex: Int,
    val segmentT: Float,
    val cumulativeMeters: Float,
    val perpDist: Float
)

data class FloorHeightEstimate(
    val floor: Int,
    val yMeters: Float,
    val confidence: Float
)
