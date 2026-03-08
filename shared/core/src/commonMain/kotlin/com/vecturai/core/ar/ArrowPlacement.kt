package com.vecturai.core.ar

import kotlinx.serialization.Serializable

/**
 * Type of arrow placement in the AR route.
 */
@Serializable
enum class ArrowType {
    /** Regular follow-the-path arrow */
    FOLLOW,
    /** Turn-left indicator */
    TURN_LEFT,
    /** Turn-right indicator */
    TURN_RIGHT,
    /** U-turn indicator */
    U_TURN,
    /** Final destination marker */
    DESTINATION,
}

/**
 * A single arrow/marker to be rendered in the AR scene.
 *
 * All coordinates are in **building-local** space (meters, Y-up).
 * The native layer applies [AlignmentTransform] to get AR-world positions.
 *
 * @property id Unique arrow identifier (for entity management)
 * @property positionX Building-local X
 * @property positionY Building-local Y (height above floor)
 * @property positionZ Building-local Z
 * @property forwardDx Forward direction X component (normalized)
 * @property forwardDy Forward direction Y component
 * @property forwardDz Forward direction Z component (normalized)
 * @property type Arrow/marker type
 * @property label Optional label (e.g., "Turn left", room name)
 * @property distanceFromStartMeters Cumulative distance from route start
 */
@Serializable
data class ArrowPlacement(
    val id: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Double,
    val forwardDx: Double,
    val forwardDy: Double = 0.0,
    val forwardDz: Double,
    val type: ArrowType = ArrowType.FOLLOW,
    val label: String? = null,
    val distanceFromStartMeters: Double = 0.0,
)

/**
 * Complete renderable route for native AR consumption.
 *
 * @property routeId Unique identifier for this renderable route
 * @property arrows Ordered arrow placements (building-local coords)
 * @property totalDistanceMeters Total route distance
 * @property currentInstruction Active textual instruction
 * @property destinationName Target room name
 */
@Serializable
data class ArRenderableRoute(
    val routeId: String,
    val arrows: List<ArrowPlacement>,
    val totalDistanceMeters: Double,
    val currentInstruction: String = "",
    val destinationName: String = "",
)
