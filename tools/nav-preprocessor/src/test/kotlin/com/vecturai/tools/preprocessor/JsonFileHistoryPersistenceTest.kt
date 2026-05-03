package com.Vectura AI.tools.preprocessor

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for JSON-file-backed history persistence.
 *
 * Verifies persist/load cycle, malformed data recovery,
 * and ordering consistency.
 */
class JsonFileHistoryPersistenceTest {

    @Serializable
    data class TestRecord(
        val visitId: String,
        val roomName: String,
        val completionStatus: String = "COMPLETED_AT_DESTINATION",
    )

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    class InMemoryStore {
        var content: String = ""
    }

    class TestFileRepo(private val store: InMemoryStore) {
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        private var cache: MutableList<TestRecord>? = null

        private fun load(): MutableList<TestRecord> {
            if (cache != null) return cache!!
            cache = if (store.content.isBlank()) {
                mutableListOf()
            } else {
                try { json.decodeFromString<List<TestRecord>>(store.content).toMutableList() }
                catch (e: Exception) { mutableListOf() }
            }
            return cache!!
        }

        private fun save() {
            cache?.let { store.content = json.encodeToString(it.toList()) }
        }

        fun add(record: TestRecord) { load().add(0, record); save() }
        fun getAll() = load().toList()
        fun clear() { load().clear(); save() }
        fun count() = load().size
    }

    @Test
    fun `persist and reload survives simulated restart`() {
        val store = InMemoryStore()

        // Session 1: write
        val repo1 = TestFileRepo(store)
        repo1.add(TestRecord("v1", "Kitchen"))
        repo1.add(TestRecord("v2", "Office"))

        // Session 2: new repo instance (simulates restart)
        val repo2 = TestFileRepo(store)
        assertEquals(2, repo2.count())
        assertEquals("Office", repo2.getAll()[0].roomName) // most recent first
    }

    @Test
    fun `empty file produces empty list`() {
        val store = InMemoryStore()
        val repo = TestFileRepo(store)
        assertEquals(0, repo.count())
    }

    @Test
    fun `malformed data recovers to empty`() {
        val store = InMemoryStore()
        store.content = "not valid json {{{ }"
        val repo = TestFileRepo(store)
        assertEquals(0, repo.count()) // recovered gracefully
    }

    @Test
    fun `clear persists empty state`() {
        val store = InMemoryStore()
        val repo1 = TestFileRepo(store)
        repo1.add(TestRecord("v1", "Kitchen"))
        repo1.clear()

        val repo2 = TestFileRepo(store)
        assertEquals(0, repo2.count())
    }

    @Test
    fun `ordering preserved across restart`() {
        val store = InMemoryStore()
        val repo1 = TestFileRepo(store)
        repo1.add(TestRecord("v1", "First"))
        repo1.add(TestRecord("v2", "Second"))
        repo1.add(TestRecord("v3", "Third"))

        val repo2 = TestFileRepo(store)
        val records = repo2.getAll()
        assertEquals("Third", records[0].roomName)
        assertEquals("Second", records[1].roomName)
        assertEquals("First", records[2].roomName)
    }
}
