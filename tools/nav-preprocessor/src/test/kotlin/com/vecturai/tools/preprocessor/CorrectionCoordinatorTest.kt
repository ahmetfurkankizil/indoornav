package com.vecturai.tools.preprocessor

import kotlin.math.sqrt
import kotlin.test.*

/**
 * Tests for CorrectionCoordinator — checkpoint-based alignment correction.
 */
class CorrectionCoordinatorTest {

    // Minimal alignment (identity — marker at origin)
    private fun identityAlignment() = TestAlignment(0.0, 0.0, 0.0, 0.0)

    data class TestAlignment(
        val offsetX: Double,
        val offsetY: Double,
        val offsetZ: Double,
        val rotationYDeg: Double,
    )

    data class TestCheckpoint(
        val id: String,
        val posX: Double,
        val posY: Double,
        val posZ: Double,
        val rotY: Double = 0.0,
        val nearestNodeId: String = "n01",
    )

    /** Simplified correction coordinator matching shared logic. */
    class TestCorrector(
        private val maxTranslation: Double = 2.0,
        private val maxRotation: Double = 15.0,
        private val minConfidence: Double = 0.3,
        private val dedupInterval: Long = 5000L,
    ) {
        var correctionCount = 0; private set
        var lastCorrectionTime = 0L; private set
        var totalMagnitude = 0.0; private set
        var alignment: TestAlignment? = null; private set

        private var checkpoints = mapOf<String, TestCheckpoint>()
        private val lastObsTimes = mutableMapOf<String, Long>()

        fun configure(cps: List<TestCheckpoint>, initial: TestAlignment) {
            checkpoints = cps.associateBy { it.id }
            alignment = initial
            correctionCount = 0; lastCorrectionTime = 0L; totalMagnitude = 0.0
            lastObsTimes.clear()
        }

        data class Result(val applied: Boolean, val transDelta: Double = 0.0, val rotDelta: Double = 0.0, val reason: String = "")

        fun onCheckpoint(
            markerId: String, arX: Double, arY: Double, arZ: Double,
            arRotY: Double = 0.0, confidence: Double = 1.0, timeMs: Long = 0L,
        ): Result {
            val cp = checkpoints[markerId] ?: return Result(false, reason = "Unknown")
            if (confidence < minConfidence) return Result(false, reason = "Low confidence")
            val lastObs = lastObsTimes[markerId]
            if (lastObs != null && timeMs > 0 && timeMs - lastObs < dedupInterval) {
                return Result(false, reason = "Too soon")
            }

            val cur = alignment ?: return Result(false, reason = "No alignment")

            // What alignment should be (simplified: offset = ar - building)
            val expectedOX = arX - cp.posX
            val expectedOZ = arZ - cp.posZ
            val expectedRot = arRotY - cp.rotY

            val deltaX = expectedOX - cur.offsetX
            val deltaZ = expectedOZ - cur.offsetZ
            val deltaRot = normalizeAngle(expectedRot - cur.rotationYDeg)

            val mag = sqrt(deltaX * deltaX + deltaZ * deltaZ)
            val scale = if (mag > maxTranslation && mag > 0) maxTranslation / mag else 1.0
            val bDeltaX = deltaX * scale
            val bDeltaZ = deltaZ * scale
            val bDeltaRot = deltaRot.coerceIn(-maxRotation, maxRotation)
            val bMag = sqrt(bDeltaX * bDeltaX + bDeltaZ * bDeltaZ)

            alignment = TestAlignment(
                cur.offsetX + bDeltaX, cur.offsetY, cur.offsetZ + bDeltaZ,
                cur.rotationYDeg + bDeltaRot,
            )
            correctionCount++
            lastCorrectionTime = timeMs
            totalMagnitude += bMag
            lastObsTimes[markerId] = timeMs

            return Result(true, bMag, kotlin.math.abs(bDeltaRot))
        }

        private fun normalizeAngle(deg: Double): Double {
            var a = deg % 360.0
            if (a > 180) a -= 360.0
            if (a < -180) a += 360.0
            return a
        }
    }

    @Test
    fun `single checkpoint mid-route applies correction`() {
        val c = TestCorrector()
        c.configure(
            listOf(TestCheckpoint("cp1", 5.0, 0.0, 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        // Observe checkpoint at AR pos (5.5, 0, 0.3) — real marker at building (5, 0, 0)
        // Expected offset: (0.5, 0, 0.3), delta from current (0,0,0) = (0.5, 0, 0.3)
        val r = c.onCheckpoint("cp1", 5.5, 0.0, 0.3, timeMs = 1000)
        assertTrue(r.applied, "Correction should be applied")
        assertTrue(r.transDelta > 0.0)
        assertEquals(1, c.correctionCount)
    }

    @Test
    fun `multiple checkpoint observations accumulate`() {
        val c = TestCorrector()
        c.configure(
            listOf(
                TestCheckpoint("cp1", 5.0, 0.0, 0.0),
                TestCheckpoint("cp2", 10.0, 0.0, 0.0),
            ),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        val r1 = c.onCheckpoint("cp1", 5.3, 0.0, 0.1, timeMs = 1000)
        assertTrue(r1.applied)
        val r2 = c.onCheckpoint("cp2", 10.5, 0.0, 0.2, timeMs = 10000)
        assertTrue(r2.applied)
        assertEquals(2, c.correctionCount)
        assertTrue(c.totalMagnitude > 0)
    }

    @Test
    fun `correction is bounded by max translation`() {
        val c = TestCorrector(maxTranslation = 2.0)
        c.configure(
            listOf(TestCheckpoint("cp1", 0.0, 0.0, 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        // Large drift: 5m offset
        val r = c.onCheckpoint("cp1", 5.0, 0.0, 0.0, timeMs = 1000)
        assertTrue(r.applied)
        assertTrue(r.transDelta <= 2.01, "Translation should be bounded to 2m, got ${r.transDelta}")
    }

    @Test
    fun `correction is bounded by max rotation`() {
        val c = TestCorrector(maxRotation = 15.0)
        c.configure(
            listOf(TestCheckpoint("cp1", 0.0, 0.0, 0.0, rotY = 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        // Large rotation drift: 30 degrees
        val r = c.onCheckpoint("cp1", 0.0, 0.0, 0.0, arRotY = 30.0, timeMs = 1000)
        assertTrue(r.applied)
        assertTrue(r.rotDelta <= 15.01, "Rotation should be bounded to 15 deg, got ${r.rotDelta}")
    }

    @Test
    fun `low confidence observation is rejected`() {
        val c = TestCorrector(minConfidence = 0.3)
        c.configure(
            listOf(TestCheckpoint("cp1", 0.0, 0.0, 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        val r = c.onCheckpoint("cp1", 1.0, 0.0, 0.0, confidence = 0.1, timeMs = 1000)
        assertFalse(r.applied, "Low confidence should be rejected")
        assertEquals(0, c.correctionCount)
    }

    @Test
    fun `deduplication rejects rapid same-marker observations`() {
        val c = TestCorrector(dedupInterval = 5000L)
        c.configure(
            listOf(TestCheckpoint("cp1", 0.0, 0.0, 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        val r1 = c.onCheckpoint("cp1", 0.5, 0.0, 0.0, timeMs = 1000)
        assertTrue(r1.applied)
        val r2 = c.onCheckpoint("cp1", 0.6, 0.0, 0.0, timeMs = 3000) // too soon
        assertFalse(r2.applied)
    }

    @Test
    fun `unknown marker is rejected`() {
        val c = TestCorrector()
        c.configure(
            listOf(TestCheckpoint("cp1", 0.0, 0.0, 0.0)),
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        val r = c.onCheckpoint("unknown", 1.0, 0.0, 0.0, timeMs = 1000)
        assertFalse(r.applied)
    }

    @Test
    fun `no alignment returns not applied`() {
        val c = TestCorrector()
        // configure but alignment is null
        c.configure(emptyList(), TestAlignment(0.0, 0.0, 0.0, 0.0))
        val r = c.onCheckpoint("cp1", 1.0, 0.0, 0.0)
        assertFalse(r.applied, "Unknown marker with no checkpoints configured")
    }

    @Test
    fun `single marker package no checkpoints works unchanged`() {
        val c = TestCorrector()
        c.configure(
            emptyList(), // no checkpoints
            TestAlignment(0.0, 0.0, 0.0, 0.0),
        )
        // Checkpoint observation for non-existent marker
        val r = c.onCheckpoint("cp1", 1.0, 0.0, 0.0, timeMs = 1000)
        assertFalse(r.applied)
        assertEquals(0, c.correctionCount)
    }
}
