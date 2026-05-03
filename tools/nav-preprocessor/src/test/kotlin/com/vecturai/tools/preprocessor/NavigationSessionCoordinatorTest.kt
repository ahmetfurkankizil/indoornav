package com.Vectura AI.tools.preprocessor

import kotlin.test.*

/**
 * Tests for the NavigationSession coordinator logic.
 *
 * Verifies session lifecycle, progress tracking, and completion.
 */
class NavigationSessionCoordinatorTest {

    data class TestSession(
        val sessionId: String,
        val destinationName: String,
        val mode: String = "REAL_SCAN",
        var progress: Double = 0.0,
        var status: String? = null,
        var endedAt: String? = null,
    ) {
        val isActive get() = status == null
    }

    data class TestVisitRecord(
        val visitId: String,
        val roomName: String,
        val completionStatus: String,
        val mode: String,
    )

    class TestCoordinator {
        var currentSession: TestSession? = null
        var summary: TestSession? = null
        val history = mutableListOf<TestVisitRecord>()

        fun startSession(id: String, destName: String, mode: String = "REAL_SCAN"): Boolean {
            currentSession = TestSession(id, destName, mode)
            summary = null
            return true
        }

        fun updateProgress(progress: Double) {
            val s = currentSession ?: return
            s.progress = progress.coerceIn(0.0, 1.0)
            if (s.progress >= 0.95) {
                endSession(if (s.mode == "SIMULATED_SCAN") "DEMO_COMPLETED" else "COMPLETED_AT_DESTINATION")
            }
        }

        fun endSession(status: String) {
            val s = currentSession ?: return
            s.status = status
            s.endedAt = "2026-01-01T00:00:00Z"
            summary = s
            currentSession = null
            history.add(0, TestVisitRecord(s.sessionId, s.destinationName, status, s.mode))
        }

        fun cancelSession() {
            val s = currentSession ?: return
            endSession("CANCELLED_BEFORE_ALIGNMENT")
        }
    }

    private val coordinator = TestCoordinator()

    @Test
    fun `start session creates active session`() {
        coordinator.startSession("s1", "Kitchen")
        assertNotNull(coordinator.currentSession)
        assertTrue(coordinator.currentSession!!.isActive)
    }

    @Test
    fun `end session clears current and sets summary`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.endSession("ENDED_MANUALLY")
        assertNull(coordinator.currentSession)
        assertNotNull(coordinator.summary)
        assertEquals("ENDED_MANUALLY", coordinator.summary!!.status)
    }

    @Test
    fun `cancel session sets cancelled status`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.cancelSession()
        assertNull(coordinator.currentSession)
        assertEquals("CANCELLED_BEFORE_ALIGNMENT", coordinator.summary!!.status)
    }

    @Test
    fun `progress at 95 auto-completes`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.updateProgress(0.5)
        assertTrue(coordinator.currentSession!!.isActive)
        coordinator.updateProgress(0.95)
        assertNull(coordinator.currentSession)
        assertEquals("COMPLETED_AT_DESTINATION", coordinator.summary!!.status)
    }

    @Test
    fun `simulated mode auto-completes as demo`() {
        coordinator.startSession("s1", "Kitchen", mode = "SIMULATED_SCAN")
        coordinator.updateProgress(0.96)
        assertEquals("DEMO_COMPLETED", coordinator.summary!!.status)
    }

    @Test
    fun `completion persists to history`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.endSession("COMPLETED_AT_DESTINATION")
        assertEquals(1, coordinator.history.size)
        assertEquals("s1", coordinator.history[0].visitId)
        assertEquals("COMPLETED_AT_DESTINATION", coordinator.history[0].completionStatus)
    }

    @Test
    fun `multiple sessions accumulate in history`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.endSession("COMPLETED_AT_DESTINATION")
        coordinator.startSession("s2", "Office A")
        coordinator.cancelSession()
        assertEquals(2, coordinator.history.size)
        assertEquals("s2", coordinator.history[0].visitId) // most recent first
    }

    @Test
    fun `starting new session clears previous summary`() {
        coordinator.startSession("s1", "Kitchen")
        coordinator.endSession("COMPLETED_AT_DESTINATION")
        assertNotNull(coordinator.summary)
        coordinator.startSession("s2", "Office A")
        assertNull(coordinator.summary)
    }
}
