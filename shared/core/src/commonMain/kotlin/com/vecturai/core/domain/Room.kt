package com.vecturai.core.domain

import kotlinx.serialization.Serializable

/**
 * A room or point of interest within the building.
 *
 * Rooms are the primary searchable destinations for users.
 * Each room is associated with one or more [NavNode]s that represent
 * entry/exit points to that room in the navigation graph.
 *
 * @property id Unique room identifier
 * @property name Human-readable display name (e.g., "Conference Room B")
 * @property description Optional description or additional info
 * @property category Room category for filtering (e.g., "office", "restroom", "meeting")
 * @property entryNodeIds IDs of NavNodes that serve as entry points to this room
 * @property centerX Center X coordinate for map display
 * @property centerY Center Y coordinate for map display
 * @property floor Floor identifier (always "ground" for single-floor MVP)
 * @property metadata Arbitrary key-value metadata for extensibility
 */
@Serializable
data class Room(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val entryNodeIds: List<String> = emptyList(),
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val floor: String = "ground",
    val metadata: Map<String, String> = emptyMap(),
)
