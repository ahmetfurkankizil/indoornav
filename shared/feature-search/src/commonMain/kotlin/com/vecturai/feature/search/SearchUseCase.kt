package com.vecturai.feature.search

import com.vecturai.core.domain.Room
import com.vecturai.core.repository.BuildingRepository

/**
 * Use case for searching rooms within a building.
 *
 * Provides room lookup by name, category, or keyword.
 * Results are filtered and ranked for display in the search screen.
 *
 * TODO: Implement fuzzy search / substring matching
 * TODO: Add search result ranking by relevance
 * TODO: Support search by category filter
 * TODO: Add recent search suggestions
 */
class SearchUseCase(
    private val buildingRepository: BuildingRepository,
) {

    /**
     * Search for rooms matching the given query.
     *
     * @param buildingId Building to search within
     * @param query Search text (matched against room name and description)
     * @return List of matching rooms, ordered by relevance
     */
    suspend fun searchRooms(buildingId: String, query: String): List<Room> {
        if (query.isBlank()) return emptyList()

        val allRooms = buildingRepository.getRooms(buildingId)

        // TODO: Replace with proper fuzzy matching / text search
        return allRooms.filter { room ->
            room.name.contains(query, ignoreCase = true) ||
                room.description?.contains(query, ignoreCase = true) == true ||
                room.category?.contains(query, ignoreCase = true) == true
        }
    }

    /**
     * Get all rooms grouped by category.
     *
     * @param buildingId Building to list rooms for
     * @return Map of category name to rooms in that category
     */
    suspend fun getRoomsByCategory(buildingId: String): Map<String, List<Room>> {
        val rooms = buildingRepository.getRooms(buildingId)
        return rooms.groupBy { it.category ?: "Other" }
    }
}
