package com.vecturai.core.navigation

import com.vecturai.core.domain.BuildingPackage
import com.vecturai.core.domain.Room

/**
 * Demo mode helper for investor-style presentations.
 *
 * Provides deterministic, one-tap navigation flows using the
 * sample building package and curated demo destinations.
 */
class DemoMode {

    /** Curated demo destination names (from sample building). */
    val demoDestinations = listOf(
        "Conference Room",
        "Kitchen",
        "Office A",
    )

    /** Default demo destination. */
    val defaultDestination = "Conference Room"

    /** Default start node (entrance). */
    val defaultStartNodeId = "n01"

    /**
     * Find a demo destination room in the package.
     * Falls back to the first available room.
     */
    fun findDemoRoom(buildingPackage: BuildingPackage, preferredName: String? = null): Room? {
        val rooms = buildingPackage.rooms
        val name = preferredName ?: defaultDestination

        return rooms.firstOrNull { it.name == name }
            ?: rooms.firstOrNull { it.name in demoDestinations }
            ?: rooms.firstOrNull()
    }

    /**
     * Get the start node for demo navigation.
     * Uses the first entrance marker's nearest node.
     */
    fun getStartNodeId(buildingPackage: BuildingPackage): String {
        return buildingPackage.entranceMarkers.firstOrNull()?.nearestNodeId
            ?: defaultStartNodeId
    }

    /**
     * Create a debug note for demo sessions.
     */
    fun debugNote(): String = "Demo mode — simulated alignment, estimated progress"
}
