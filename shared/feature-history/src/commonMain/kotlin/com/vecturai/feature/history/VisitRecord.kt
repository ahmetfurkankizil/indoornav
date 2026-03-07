package com.vecturai.feature.history

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * A record of a completed navigation visit.
 *
 * Stored persistently so users can quickly re-navigate to
 * previously visited destinations.
 *
 * @property id Unique visit record ID
 * @property buildingId Building where the visit occurred
 * @property buildingName Human-readable building name
 * @property roomId Destination room ID
 * @property roomName Destination room name
 * @property timestamp When the visit was completed (ISO 8601)
 * @property durationSeconds How long the navigation took
 * @property routeDistanceMeters Total route distance
 */
@Serializable
data class VisitRecord(
    val id: String,
    val buildingId: String,
    val buildingName: String,
    val roomId: String,
    val roomName: String,
    val timestamp: String, // ISO 8601 — using String for simplicity in MVP
    val durationSeconds: Int? = null,
    val routeDistanceMeters: Double? = null,
)
