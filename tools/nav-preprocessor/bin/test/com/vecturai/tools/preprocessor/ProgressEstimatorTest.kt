package com.vecturai.tools.preprocessor

import kotlin.math.sqrt
import kotlin.test.*

/** Test-only point type (file-private to avoid JUnit5 class discovery). */
private data class Pt(val x: Double, val z: Double)

/** Test-only result type (file-private to avoid JUnit5 class discovery). */
private data class EstimatorResult(
    val progress: Double,
    val remaining: Double,
    val segIndex: Int,
    val distFromRoute: Double,
    val lowConfidence: Boolean,
)

/** Simplified polyline projector matching shared ProgressEstimator logic. */
private class TestEstimator(
    private val waypoints: List<Pt>,
    private val monotonicTolerance: Double = 0.5,
    private val offRouteThreshold: Double = 3.0,
) {
    private val cumDist: List<Double>
    val totalDist: Double
    private var peakDist = 0.0

    init {
        val cd = mutableListOf(0.0)
        for (i in 1 until waypoints.size) {
            cd.add(cd.last() + dist(waypoints[i-1], waypoints[i]))
        }
        cumDist = cd
        totalDist = cd.last()
    }

    fun update(px: Double, pz: Double): EstimatorResult {
        var bestDist = Double.MAX_VALUE
        var bestCum = 0.0
        var bestSeg = 0

        for (i in 0 until waypoints.size - 1) {
            val a = waypoints[i]; val b = waypoints[i+1]
            val dx = b.x - a.x; val dz = b.z - a.z
            val segLen = dist(a, b)
            if (segLen < 0.001) continue
            var t = ((px - a.x) * dx + (pz - a.z) * dz) / (dx*dx + dz*dz)
            t = t.coerceIn(0.0, 1.0)
            val projX = a.x + t * dx; val projZ = a.z + t * dz
            val d = sqrt((px-projX)*(px-projX) + (pz-projZ)*(pz-projZ))
            if (d < bestDist) {
                bestDist = d; bestSeg = i
                bestCum = cumDist[i] + t * segLen
            }
        }

        // Monotonic guard
        val guarded = if (bestCum >= peakDist - monotonicTolerance) {
            maxOf(bestCum, peakDist)
        } else peakDist
        peakDist = guarded

        val frac = if (totalDist > 0) (guarded / totalDist).coerceIn(0.0, 1.0) else 0.0
        val rem = (totalDist - guarded).coerceAtLeast(0.0)

        return EstimatorResult(frac, rem, bestSeg, bestDist, bestDist > offRouteThreshold)
    }

    private fun dist(a: Pt, b: Pt): Double {
        val dx = b.x - a.x; val dz = b.z - a.z
        return sqrt(dx*dx + dz*dz)
    }
}

/**
 * Tests for the ProgressEstimator (route-relative projection).
 */
class ProgressEstimatorTest {

    // Demo route: n01(0,0) → n02(3,0) → n03(6,0) → n04(6,4) → n05(3,4) = 13m
    private val route = listOf(Pt(0.0, 0.0), Pt(3.0, 0.0), Pt(6.0, 0.0), Pt(6.0, 4.0), Pt(3.0, 4.0))

    @Test
    fun `start position gives zero progress`() {
        val est = TestEstimator(route)
        val r = est.update(0.0, 0.0)
        assertEquals(0.0, r.progress, 0.01)
        assertEquals(13.0, r.remaining, 0.1)
    }

    @Test
    fun `midpoint of first segment gives correct progress`() {
        val est = TestEstimator(route)
        val r = est.update(1.5, 0.0)
        assertEquals(1.5 / 13.0, r.progress, 0.02)
    }

    @Test
    fun `end of route gives 100 percent`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0) // start
        val r = est.update(3.0, 4.0) // destination
        assertEquals(1.0, r.progress, 0.01)
        assertEquals(0.0, r.remaining, 0.1)
    }

    @Test
    fun `progress after turn`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0)
        val r = est.update(6.0, 2.0) // halfway through vertical segment
        // Should be at 3 + 3 + 2 = 8m out of 13
        assertEquals(8.0 / 13.0, r.progress, 0.05)
    }

    @Test
    fun `small off-route deviation still progresses`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0)
        val r = est.update(3.0, 0.5) // 0.5m off route
        assertTrue(r.progress > 0.2)
        assertFalse(r.lowConfidence) // within 3m threshold
    }

    @Test
    fun `large off-route triggers low confidence`() {
        val est = TestEstimator(route)
        val r = est.update(3.0, 10.0) // 6m off closest segment (6,4→3,4)
        assertTrue(r.lowConfidence)
    }

    @Test
    fun `backwards movement is suppressed by monotonic guard`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0)
        est.update(3.0, 0.0) // progress to 3m
        val r1 = est.update(3.0, 0.0)
        val r2 = est.update(1.0, 0.0) // walk backwards
        assertTrue(r2.progress >= r1.progress, "Progress should not decrease")
    }

    @Test
    fun `noisy position samples maintain progress`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0)
        est.update(2.0, 0.0)
        est.update(2.5, 0.1) // slight noise
        est.update(2.3, -0.1) // jitter back
        val r = est.update(3.0, 0.0) // forward again
        assertTrue(r.progress >= 3.0 / 13.0 - 0.05)
    }

    @Test
    fun `overshoot near destination clamps to 1`() {
        val est = TestEstimator(route)
        est.update(0.0, 0.0)
        val r = est.update(2.0, 4.0) // past destination
        assertTrue(r.progress <= 1.0)
    }

    @Test
    fun `segment index updates correctly`() {
        val est = TestEstimator(route)
        val r0 = est.update(1.5, 0.0)
        assertEquals(0, r0.segIndex) // first segment
        val r1 = est.update(6.0, 2.0)
        assertEquals(2, r1.segIndex) // third segment (vertical)
    }
}
