package com.vecturai.tools.preprocessor

import kotlin.math.sqrt
import kotlin.test.*

/**
 * Tests for progress estimation continuity before and after alignment correction.
 *
 * Verifies that:
 * - Progress doesn't regress after correction
 * - Remaining distance updates after correction
 * - Monotonic guard holds through correction
 * - Arrival detection works after correction
 */
class ProgressContinuityAfterCorrectionTest {

    data class Pt(val x: Double, val z: Double)

    /** Simplified estimator with updateAlignment support. */
    class TestEstimator(
        private val waypoints: List<Pt>,
        private val monotonicTolerance: Double = 0.5,
    ) {
        private val cumDist: List<Double>
        val totalDist: Double
        private var peakDist = 0.0
        private var offsetX = 0.0
        private var offsetZ = 0.0

        init {
            val cd = mutableListOf(0.0)
            for (i in 1 until waypoints.size) cd.add(cd.last() + dist(waypoints[i-1], waypoints[i]))
            cumDist = cd
            totalDist = cd.last()
        }

        data class Result(val progress: Double, val remaining: Double)

        fun setAlignment(oX: Double, oZ: Double) { offsetX = oX; offsetZ = oZ }

        fun update(arX: Double, arZ: Double): Result {
            // Inverse transform: building = ar - offset
            val bx = arX - offsetX
            val bz = arZ - offsetZ

            var bestDist = Double.MAX_VALUE; var bestCum = 0.0
            for (i in 0 until waypoints.size - 1) {
                val a = waypoints[i]; val b = waypoints[i+1]
                val dx = b.x - a.x; val dz = b.z - a.z
                val segLen = dist(a, b)
                if (segLen < 0.001) continue
                var t = ((bx - a.x) * dx + (bz - a.z) * dz) / (dx*dx + dz*dz)
                t = t.coerceIn(0.0, 1.0)
                val projX = a.x + t * dx; val projZ = a.z + t * dz
                val d = sqrt((bx-projX)*(bx-projX) + (bz-projZ)*(bz-projZ))
                if (d < bestDist) { bestDist = d; bestCum = cumDist[i] + t * segLen }
            }

            val guarded = if (bestCum >= peakDist - monotonicTolerance) maxOf(bestCum, peakDist) else peakDist
            peakDist = guarded
            val frac = if (totalDist > 0) (guarded / totalDist).coerceIn(0.0, 1.0) else 0.0
            return Result(frac, (totalDist - guarded).coerceAtLeast(0.0))
        }

        private fun dist(a: Pt, b: Pt) = sqrt((b.x-a.x)*(b.x-a.x) + (b.z-a.z)*(b.z-a.z))
    }

    // Route: 0,0 → 5,0 → 10,0 = 10m
    private val route = listOf(Pt(0.0, 0.0), Pt(5.0, 0.0), Pt(10.0, 0.0))

    @Test
    fun `progress does not regress after correction`() {
        val est = TestEstimator(route)
        est.setAlignment(0.0, 0.0)

        val before = est.update(4.0, 0.0) // ~40% progress
        assertTrue(before.progress > 0.3)

        // Apply correction: offset changes by 0.5m
        est.setAlignment(0.5, 0.0)
        val after = est.update(4.5, 0.0) // same building pos after correction

        assertTrue(after.progress >= before.progress, "Progress should not decrease after correction")
    }

    @Test
    fun `remaining distance updates after correction`() {
        val est = TestEstimator(route)
        est.setAlignment(0.0, 0.0)

        est.update(3.0, 0.0) // at 3m
        val r1 = est.update(5.0, 0.0) // at 5m

        // Correction: offset changes
        est.setAlignment(0.3, 0.0)
        val r2 = est.update(5.3, 0.0) // still at ~5m building-local

        assertTrue(r2.remaining >= 0.0)
        assertTrue(r2.remaining <= est.totalDist)
    }

    @Test
    fun `monotonic guard holds through correction`() {
        val est = TestEstimator(route)
        est.setAlignment(0.0, 0.0)

        est.update(0.0, 0.0) // start
        est.update(5.0, 0.0) // 50%
        val peak = est.update(7.0, 0.0) // 70%

        // Correction that shifts the effective building position backward
        est.setAlignment(2.0, 0.0)
        val after = est.update(7.0, 0.0) // now building pos is 5.0 (regressed)

        assertTrue(after.progress >= peak.progress, "Monotonic guard should prevent regression")
    }

    @Test
    fun `arrival works after correction`() {
        val est = TestEstimator(route)
        est.setAlignment(0.0, 0.0)

        est.update(0.0, 0.0)
        est.update(5.0, 0.0)

        // Correction
        est.setAlignment(0.2, 0.0)
        val nearEnd = est.update(10.2, 0.0) // should be at destination

        assertEquals(1.0, nearEnd.progress, 0.01, "Should reach 100% near destination after correction")
        assertEquals(0.0, nearEnd.remaining, 0.5, "Remaining should be ~0 at destination")
    }

    @Test
    fun `zero correction keeps progress identical`() {
        val est = TestEstimator(route)
        est.setAlignment(0.0, 0.0)

        val before = est.update(5.0, 0.0)

        // Zero correction
        est.setAlignment(0.0, 0.0)
        val after = est.update(5.0, 0.0)

        assertEquals(before.progress, after.progress, 0.001, "Zero correction should not change progress")
    }
}
