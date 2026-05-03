package com.Vectura AI.core.repository

import com.Vectura AI.core.domain.VisitRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON-file-backed history repository.
 *
 * Persists visit records to a JSON file on the local filesystem
 * so history survives app restarts. Uses a platform-provided
 * file path for storage.
 *
 * Thread safety: single-writer assumption (MVP, single UI thread).
 *
 * @param readFile Function to read the JSON file contents (empty string if not found)
 * @param writeFile Function to write JSON string to file
 */
class JsonFileHistoryRepository(
    private val readFile: () -> String,
    private val writeFile: (String) -> Unit,
) : HistoryRepository {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private var cache: MutableList<VisitRecord>? = null

    private fun loadCache(): MutableList<VisitRecord> {
        if (cache != null) return cache!!

        val content = try {
            readFile()
        } catch (e: Exception) {
            ""
        }

        cache = if (content.isBlank()) {
            mutableListOf()
        } else {
            try {
                json.decodeFromString<List<VisitRecord>>(content).toMutableList()
            } catch (e: Exception) {
                // Malformed data recovery: discard and start fresh
                println("[History] Failed to parse stored history, starting fresh: ${e.message}")
                mutableListOf()
            }
        }
        return cache!!
    }

    private fun persist() {
        val data = cache ?: return
        try {
            val encoded = json.encodeToString(data.toList())
            writeFile(encoded)
        } catch (e: Exception) {
            println("[History] Failed to persist: ${e.message}")
        }
    }

    override suspend fun getRecentVisits(limit: Int): List<VisitRecord> {
        return loadCache().take(limit)
    }

    override suspend fun addVisit(record: VisitRecord) {
        loadCache().add(0, record)
        persist()
    }

    override suspend fun deleteVisit(visitId: String) {
        loadCache().removeAll { it.visitId == visitId }
        persist()
    }

    override suspend fun clearHistory() {
        loadCache().clear()
        persist()
    }
}
