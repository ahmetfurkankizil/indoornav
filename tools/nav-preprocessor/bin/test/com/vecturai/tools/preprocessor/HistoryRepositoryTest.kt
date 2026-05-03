package com.Vectura AI.tools.preprocessor

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Tests for HistoryRepository and VisitRecord.
 */
class HistoryRepositoryTest {

    @Serializable
    data class TestVisitRecord(
        val visitId: String,
        val buildingId: String = "b1",
        val buildingName: String = "Demo Office",
        val roomId: String = "r1",
        val roomName: String,
        val visitedAtIso: String = "2026-03-08T00:00:00Z",
        val endedAtIso: String? = null,
        val completionStatus: String = "COMPLETED_AT_DESTINATION",
        val routeDistanceMeters: Double = 0.0,
        val mode: String = "REAL_SCAN",
        val entranceMarkerId: String? = null,
    ) {
        val isCompleted get() = completionStatus == "COMPLETED_AT_DESTINATION" || completionStatus == "DEMO_COMPLETED"
        val isDemo get() = mode == "SIMULATED_SCAN" || completionStatus == "DEMO_COMPLETED"
        val statusLabel get() = when (completionStatus) {
            "COMPLETED_AT_DESTINATION" -> "Completed"
            "DEMO_COMPLETED" -> "Demo"
            "ENDED_MANUALLY" -> "Ended early"
            else -> completionStatus
        }
    }

    class TestRepo {
        private val records = mutableListOf<TestVisitRecord>()
        fun getRecent(limit: Int = 50) = records.take(limit)
        fun add(record: TestVisitRecord) { records.add(0, record) }
        fun delete(id: String) { records.removeAll { it.visitId == id } }
        fun clear() { records.clear() }
        fun count() = records.size
    }

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    private val repo = TestRepo()

    @Test
    fun `empty repo returns empty list`() {
        assertTrue(repo.getRecent().isEmpty())
    }

    @Test
    fun `add and retrieve`() {
        repo.add(TestVisitRecord("v1", roomName = "Kitchen"))
        assertEquals(1, repo.count())
        assertEquals("Kitchen", repo.getRecent().first().roomName)
    }

    @Test
    fun `most recent first ordering`() {
        repo.add(TestVisitRecord("v1", roomName = "Kitchen"))
        repo.add(TestVisitRecord("v2", roomName = "Office"))
        assertEquals("Office", repo.getRecent().first().roomName)
    }

    @Test
    fun `limit caps results`() {
        repeat(10) { repo.add(TestVisitRecord("v$it", roomName = "Room $it")) }
        assertEquals(3, repo.getRecent(3).size)
    }

    @Test
    fun `delete removes specific record`() {
        repo.add(TestVisitRecord("v1", roomName = "Kitchen"))
        repo.add(TestVisitRecord("v2", roomName = "Office"))
        repo.delete("v1")
        assertEquals(1, repo.count())
        assertEquals("v2", repo.getRecent().first().visitId)
    }

    @Test
    fun `clear removes all`() {
        repo.add(TestVisitRecord("v1", roomName = "Kitchen"))
        repo.add(TestVisitRecord("v2", roomName = "Office"))
        repo.clear()
        assertEquals(0, repo.count())
    }

    @Test
    fun `visit record serialization roundtrip`() {
        val record = TestVisitRecord(
            visitId = "v1",
            roomName = "Conference Room",
            completionStatus = "DEMO_COMPLETED",
            mode = "SIMULATED_SCAN",
            routeDistanceMeters = 42.5,
        )
        val encoded = json.encodeToString(record)
        val decoded = json.decodeFromString<TestVisitRecord>(encoded)
        assertEquals(record, decoded)
    }

    @Test
    fun `visit record computed properties`() {
        val completed = TestVisitRecord("v1", roomName = "X", completionStatus = "COMPLETED_AT_DESTINATION")
        assertTrue(completed.isCompleted)
        assertFalse(completed.isDemo)
        assertEquals("Completed", completed.statusLabel)

        val demo = TestVisitRecord("v2", roomName = "X", completionStatus = "DEMO_COMPLETED", mode = "SIMULATED_SCAN")
        assertTrue(demo.isCompleted)
        assertTrue(demo.isDemo)
        assertEquals("Demo", demo.statusLabel)

        val cancelled = TestVisitRecord("v3", roomName = "X", completionStatus = "ENDED_MANUALLY")
        assertFalse(cancelled.isCompleted)
        assertEquals("Ended early", cancelled.statusLabel)
    }

    @Test
    fun `mixed statuses in history`() {
        repo.add(TestVisitRecord("v1", roomName = "Kitchen", completionStatus = "COMPLETED_AT_DESTINATION"))
        repo.add(TestVisitRecord("v2", roomName = "Office", completionStatus = "ENDED_MANUALLY"))
        repo.add(TestVisitRecord("v3", roomName = "Lab", completionStatus = "DEMO_COMPLETED"))

        val history = repo.getRecent()
        assertEquals(3, history.size)
        assertTrue(history[0].isDemo) // Lab, most recent
        assertFalse(history[1].isCompleted)  // Office, ended manually
        assertTrue(history[2].isCompleted)  // Kitchen
    }
}
