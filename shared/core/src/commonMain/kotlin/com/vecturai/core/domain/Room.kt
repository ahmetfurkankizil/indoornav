package com.VecturAI.core.domain

import kotlinx.serialization.Serializable

/**
 * A room or point of interest within the building.
 *
 * Rooms are the primary searchable navigation destinations.
 *
 * @property id Unique room identifier
 * @property name Human-readable display name (e.g., "Conference Room B")
 * @property description Optional room description
 * @property category Room category for filtering (e.g., "office", "restroom")
 * @property entryNodeIds IDs of NavNodes that serve as entry points
 * @property keywords Searchable keywords (e.g., ["printer", "copy"])
 * @property aliases Alternate names (e.g., ["HR Office", "Room 203"])
 * @property centerX Center X coordinate for map display (meters, building-local)
 * @property centerY Center Y coordinate for map display (meters, building-local)
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
    val keywords: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val floor: String = "ground",
    val metadata: Map<String, String> = emptyMap(),
)
