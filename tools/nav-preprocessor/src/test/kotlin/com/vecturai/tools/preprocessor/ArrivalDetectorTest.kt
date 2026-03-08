package com.vecturai.tools.preprocessor

import kotlin.test.*

/**
 * Tests for the ArrivalDetector logic.
 *
 * Mirrors shared ArrivalDetector to verify arrival detection
 * independently of KMP module compilation.
 */
class ArrivalDetectorTest {

    sealed class TestArrivalStatus {
        data object NotArrived : TestArrivalStatus()
        data class Approaching(val remaining: Double, val progress: Double) : TestArrivalStatus()
        data object Arrived : TestArrivalStatus()
    }

    class TestDetector(
        private val approachThreshold: Double = 0.80,
        private val arrivalThreshold: Double = 0.95,
        private val destDistanceMeters: Double = 1.5,
    ) {
        fun check(progress: Double, distToDest: Double? = null, totalDist: Double = 0.0): TestArrivalStatus {
            val p = progress.coerceIn(0.0, 1.0)
            if (distToDest != null && distToDest <= destDistanceMeters) return TestArrivalStatus.Arrived
            if (p >= arrivalThreshold) return TestArrivalStatus.Arrived
            if (p >= approachThreshold) {
                return TestArrivalStatus.Approaching(totalDist * (1.0 - p), p)
            }
            return TestArrivalStatus.NotArrived
        }
    }

    private val detector = TestDetector()

    @Test
    fun `zero progress is not arrived`() {
        val status = detector.check(0.0)
        assertTrue(status is TestArrivalStatus.NotArrived)
    }

    @Test
    fun `50 percent progress is not arrived`() {
        val status = detector.check(0.5)
        assertTrue(status is TestArrivalStatus.NotArrived)
    }

    @Test
    fun `80 percent is approaching`() {
        val status = detector.check(0.80, totalDist = 100.0)
        assertTrue(status is TestArrivalStatus.Approaching)
        val approaching = status as TestArrivalStatus.Approaching
        assertEquals(20.0, approaching.remaining, 0.01)
    }

    @Test
    fun `85 percent is approaching`() {
        val status = detector.check(0.85, totalDist = 50.0)
        assertTrue(status is TestArrivalStatus.Approaching)
    }

    @Test
    fun `95 percent is arrived`() {
        val status = detector.check(0.95)
        assertTrue(status is TestArrivalStatus.Arrived)
    }

    @Test
    fun `100 percent is arrived`() {
        val status = detector.check(1.0)
        assertTrue(status is TestArrivalStatus.Arrived)
    }

    @Test
    fun `over 1 clamped to arrived`() {
        val status = detector.check(1.5)
        assertTrue(status is TestArrivalStatus.Arrived)
    }

    @Test
    fun `negative clamped to not arrived`() {
        val status = detector.check(-0.5)
        assertTrue(status is TestArrivalStatus.NotArrived)
    }

    @Test
    fun `distance to dest under threshold is arrived regardless of progress`() {
        val status = detector.check(0.3, distToDest = 1.0)
        assertTrue(status is TestArrivalStatus.Arrived)
    }

    @Test
    fun `distance to dest above threshold uses progress`() {
        val status = detector.check(0.3, distToDest = 5.0)
        assertTrue(status is TestArrivalStatus.NotArrived)
    }

    @Test
    fun `custom thresholds work`() {
        val strict = TestDetector(approachThreshold = 0.90, arrivalThreshold = 0.99)
        assertTrue(strict.check(0.85) is TestArrivalStatus.NotArrived)
        assertTrue(strict.check(0.92) is TestArrivalStatus.Approaching)
        assertTrue(strict.check(0.99) is TestArrivalStatus.Arrived)
    }
}
