package com.vecturai.core.repository

import com.vecturai.core.domain.VisitRecord

/**
 * In-memory implementation of [HistoryRepository].
 *
 * Stores visit records in a mutable list, ordered by most recent first.
 * MVP implementation — no disk persistence. Data is lost on app restart.
 *
 * Future: replace with SqlDelight-backed persistent storage.
 */
class InMemoryHistoryRepository : HistoryRepository {

    private val records = mutableListOf<VisitRecord>()

    override suspend fun getRecentVisits(limit: Int): List<VisitRecord> {
        return records.take(limit)
    }

    override suspend fun addVisit(record: VisitRecord) {
        // Add to front (most recent first)
        records.add(0, record)
    }

    override suspend fun deleteVisit(visitId: String) {
        records.removeAll { it.visitId == visitId }
    }

    override suspend fun clearHistory() {
        records.clear()
    }

    /** Get total count of records. */
    fun count(): Int = records.size

    /** Check if a record exists. */
    fun has(visitId: String): Boolean = records.any { it.visitId == visitId }
}
