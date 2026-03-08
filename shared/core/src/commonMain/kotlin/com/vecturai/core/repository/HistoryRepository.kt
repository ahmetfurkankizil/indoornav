package com.vecturai.core.repository

import com.vecturai.core.domain.VisitRecord

/**
 * Repository for user visit history.
 *
 * Persists records of completed navigation sessions so users
 * can quickly re-navigate to previously visited destinations.
 */
interface HistoryRepository {

    /**
     * Get all visit records, ordered by most recent first.
     *
     * @param limit Maximum number of records to return
     * @return List of visit records
     */
    suspend fun getRecentVisits(limit: Int = 50): List<VisitRecord>

    /**
     * Record a completed navigation visit.
     *
     * @param record The visit to persist
     */
    suspend fun addVisit(record: VisitRecord)

    /**
     * Delete a specific visit record.
     *
     * @param visitId ID of the visit to delete
     */
    suspend fun deleteVisit(visitId: String)

    /**
     * Clear all visit history.
     */
    suspend fun clearHistory()
}
