package com.Vectura AI.tools.preprocessor

import kotlin.test.*

/**
 * Tests for OffRouteDetector — lateral deviation and recovery logic.
 */
class OffRouteDetectorTest {

    enum class Status { ON_ROUTE, MINOR_DRIFT, LOW_CONFIDENCE, LIKELY_OFF_ROUTE, RECOVERY_RECOMMENDED }
    enum class Recommendation { CONTINUE, RESCAN_MARKER, MOVE_TOWARD_ROUTE, USE_DEMO_MODE }

    data class Assessment(val status: Status, val recommendation: Recommendation)

    class TestDetector(
        private val minorThreshold: Double = 2.0,
        private val lowConfThreshold: Double = 4.0,
        private val offRouteThreshold: Double = 6.0,
        private val staleThresholdMs: Long = 3000L,
    ) {
        private var lastPoseTime = 0L
        private var trackingLimited = false
        private var consecutiveLargeCorrections = 0
        private var lastDistance = 0.0

        fun onPoseUpdate(dist: Double, limited: Boolean, timeMs: Long): Assessment {
            lastDistance = dist
            trackingLimited = limited
            lastPoseTime = timeMs
            return assess(timeMs)
        }

        fun onCorrectionApplied(mag: Double) {
            if (mag > 1.5) consecutiveLargeCorrections++ else consecutiveLargeCorrections = 0
        }

        fun onTrackingChanged(limited: Boolean) { trackingLimited = limited }

        fun assess(currentMs: Long): Assessment {
            val staleMs = if (lastPoseTime > 0) currentMs - lastPoseTime else 0L
            val isStale = staleMs > staleThresholdMs

            val status = when {
                lastDistance > offRouteThreshold && (trackingLimited || isStale) -> Status.RECOVERY_RECOMMENDED
                lastDistance > offRouteThreshold && consecutiveLargeCorrections >= 2 -> Status.RECOVERY_RECOMMENDED
                lastDistance > offRouteThreshold -> Status.LIKELY_OFF_ROUTE
                lastDistance > lowConfThreshold || (isStale && trackingLimited) -> Status.LOW_CONFIDENCE
                lastDistance > minorThreshold || trackingLimited -> Status.MINOR_DRIFT
                else -> Status.ON_ROUTE
            }

            val recommendation = when (status) {
                Status.ON_ROUTE, Status.MINOR_DRIFT -> Recommendation.CONTINUE
                Status.LOW_CONFIDENCE -> Recommendation.RESCAN_MARKER
                Status.LIKELY_OFF_ROUTE -> Recommendation.MOVE_TOWARD_ROUTE
                Status.RECOVERY_RECOMMENDED -> Recommendation.RESCAN_MARKER
            }

            return Assessment(status, recommendation)
        }
    }

    @Test
    fun `on-route position returns ON_ROUTE`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(1.0, false, 1000)
        assertEquals(Status.ON_ROUTE, r.status)
        assertEquals(Recommendation.CONTINUE, r.recommendation)
    }

    @Test
    fun `minor drift returns MINOR_DRIFT`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(2.5, false, 1000)
        assertEquals(Status.MINOR_DRIFT, r.status)
        assertEquals(Recommendation.CONTINUE, r.recommendation)
    }

    @Test
    fun `tracking limited returns MINOR_DRIFT`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(1.0, true, 1000)
        assertEquals(Status.MINOR_DRIFT, r.status)
    }

    @Test
    fun `large deviation returns LOW_CONFIDENCE`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(4.5, false, 1000)
        assertEquals(Status.LOW_CONFIDENCE, r.status)
        assertEquals(Recommendation.RESCAN_MARKER, r.recommendation)
    }

    @Test
    fun `likely off-route returns LIKELY_OFF_ROUTE`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(7.0, false, 1000)
        assertEquals(Status.LIKELY_OFF_ROUTE, r.status)
        assertEquals(Recommendation.MOVE_TOWARD_ROUTE, r.recommendation)
    }

    @Test
    fun `off-route with tracking limited returns RECOVERY_RECOMMENDED`() {
        val d = TestDetector()
        val r = d.onPoseUpdate(7.0, true, 1000)
        assertEquals(Status.RECOVERY_RECOMMENDED, r.status)
    }

    @Test
    fun `stale pose with tracking limited returns LOW_CONFIDENCE`() {
        val d = TestDetector()
        d.onPoseUpdate(1.0, false, 1000) // initial
        d.onTrackingChanged(true)
        // Assess at 5 seconds later without new pose
        val r = d.assess(5000)
        assertEquals(Status.LOW_CONFIDENCE, r.status)
    }

    @Test
    fun `repeated large corrections trigger RECOVERY_RECOMMENDED on off-route`() {
        val d = TestDetector()
        d.onCorrectionApplied(2.0) // large
        d.onCorrectionApplied(2.0) // large again
        val r = d.onPoseUpdate(7.0, false, 1000)
        assertEquals(Status.RECOVERY_RECOMMENDED, r.status)
    }

    @Test
    fun `small corrections reset large correction counter`() {
        val d = TestDetector()
        d.onCorrectionApplied(2.0) // large
        d.onCorrectionApplied(0.3) // small — resets counter
        val r = d.onPoseUpdate(7.0, false, 1000)
        // Only one large correction, so just LIKELY_OFF_ROUTE
        assertEquals(Status.LIKELY_OFF_ROUTE, r.status)
    }

    @Test
    fun `false positive control - minor noise stays ON_ROUTE`() {
        val d = TestDetector()
        // Slight noise well within threshold
        d.onPoseUpdate(0.5, false, 1000)
        d.onPoseUpdate(1.0, false, 2000)
        val r = d.onPoseUpdate(0.8, false, 3000)
        assertEquals(Status.ON_ROUTE, r.status)
    }
}
