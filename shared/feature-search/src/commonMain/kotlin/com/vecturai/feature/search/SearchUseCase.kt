package com.VecturAI.feature.search

import com.VecturAI.core.domain.Room
import com.VecturAI.core.repository.BuildingRepository

/**
 * Use case for searching rooms within a building.
 *
 * Searches by name, description, category, keywords, and aliases.
 * Results ranked by match quality (name match > keyword > alias).
 */
class SearchUseCase(
    private val buildingRepository: BuildingRepository,
) {

    /**
     * Search for rooms matching the given query.
     *
     * @param buildingId Building to search within
     * @param query Search text
     * @return List of matching rooms, ordered by relevance
     */
    suspend fun searchRooms(buildingId: String, query: String): List<Room> {
        if (query.isBlank()) return emptyList()

        val allRooms = buildingRepository.getRooms(buildingId)
        val q = query.trim().lowercase()

        return allRooms
            .map { room -> room to scoreMatch(room, q) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Get all rooms grouped by category.
     */
    suspend fun getRoomsByCategory(buildingId: String): Map<String, List<Room>> {
        val rooms = buildingRepository.getRooms(buildingId)
        return rooms.groupBy { it.category ?: "Other" }
    }

    /**
     * Get all rooms (flat list).
     */
    suspend fun getAllRooms(buildingId: String): List<Room> {
        return buildingRepository.getRooms(buildingId)
    }

    /**
     * Score a room against a search query. Higher = better match.
     */
    private fun scoreMatch(room: Room, query: String): Int {
        var score = 0

        // Exact name match (highest priority)
        if (room.name.lowercase() == query) score += 100

        // Name contains query
        if (room.name.lowercase().contains(query)) score += 50

        // Alias match
        if (room.aliases.any { it.lowercase().contains(query) }) score += 30

        // Keyword match
        if (room.keywords.any { it.lowercase().contains(query) }) score += 20

        // Category match
        if (room.category?.lowercase()?.contains(query) == true) score += 10

        // Description match
        if (room.description?.lowercase()?.contains(query) == true) score += 5

        return score
    }
}
