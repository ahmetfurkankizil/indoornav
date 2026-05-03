package com.Vectura AI.feature.history

import com.Vectura AI.core.repository.HistoryRepository

/**
 * Use case for managing visit history.
 *
 * Provides operations for listing, adding, and clearing visit records.
 *
 * TODO: Add visit deduplication (same building+room within N minutes)
 * TODO: Support "favorite" destinations
 */
class HistoryUseCase(
    private val historyRepository: HistoryRepository,
) {

    /**
     * Get recent visit history.
     *
     * @param limit Maximum number of records
     * @return Visit records ordered by most recent first
     */
    suspend fun getRecentVisits(limit: Int = 50): List<VisitRecord> {
        return historyRepository.getRecentVisits(limit)
    }

    /**
     * Record a completed navigation visit.
     */
    suspend fun recordVisit(record: VisitRecord) {
        historyRepository.addVisit(record)
    }

    /**
     * Delete a specific visit from history.
     */
    suspend fun deleteVisit(visitId: String) {
        historyRepository.deleteVisit(visitId)
    }

    /**
     * Clear all visit history.
     */
    suspend fun clearAllHistory() {
        historyRepository.clearHistory()
    }
}
