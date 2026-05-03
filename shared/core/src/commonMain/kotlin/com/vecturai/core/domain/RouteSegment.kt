package com.Vectura AI.core.domain

import kotlinx.serialization.Serializable

/**
 * A single segment of a computed navigation route.
 *
 * A complete route from origin to destination is composed of an ordered
 * list of [RouteSegment]s. Each segment represents movement from one
 * nav node to the next, along with the instruction to display.
 *
 * @property fromNodeId Starting node of this segment
 * @property toNodeId Ending node of this segment
 * @property distanceMeters Distance of this segment in meters
 * @property instruction Human-readable instruction (e.g., "Turn left", "Continue straight")
 * @property headingDegrees Direction of travel in degrees (0 = north in nav-graph space)
 */
@Serializable
data class RouteSegment(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val instruction: String = "",
    val headingDegrees: Double = 0.0,
)

/**
 * A computed route from origin to destination within the building.
 *
 * @property originNodeId Starting node ID
 * @property destinationNodeId Ending node ID
 * @property destinationRoom Target room information
 * @property segments Ordered list of route segments
 * @property totalDistanceMeters Total distance of the entire route
 * @property estimatedTimeSeconds Estimated walking time in seconds
 */
@Serializable
data class Route(
    val originNodeId: String,
    val destinationNodeId: String,
    val destinationRoom: Room? = null,
    val segments: List<RouteSegment>,
    val totalDistanceMeters: Double,
    val estimatedTimeSeconds: Int,
)
