package com.vecturai.core.navigation

import com.vecturai.core.ar.AlignmentTransform
import com.vecturai.core.ar.CameraPose
import com.vecturai.core.ar.ProgressUpdate
import com.vecturai.core.ar.TrackingQuality
import kotlin.math.sqrt

/**
 * Estimates navigation progress by projecting the user's position
 * onto the route polyline.
 *
 * Algorithm:
 * 1. Convert AR-world camera position → building-local via inverse alignment
 * 2. Find nearest point on route polyline (list of waypoints)
 * 3. Compute cumulative distance to nearest point / total route distance
 * 4. Apply monotonic guard (progress only increases, with tolerance)
 *
 * Assumptions:
 * - Route is a sequence of waypoints in building-local coords (meters, Y-up)
 * - User walks approximately along the route corridor
 * - VIO drift is small (< 2%) over route length
 */
class ProgressEstimator(
    /** Tolerance for backwards jitter (meters). */
    private val monotonicToleranceMeters: Double = 0.5,
    /** Distance from route beyond which confidence is low (meters). */
    private val offRouteThresholdMeters: Double = 3.0,
) {
    /** Route waypoints in building-local coords. */
    private var routePoints: List<RoutePoint> = emptyList()
    private var cumulativeDistances: List<Double> = emptyList()
    private var totalRouteDistance: Double = 0.0
    private var alignmentTransform: AlignmentTransform? = null

    /** Highest progress seen so far (monotonic guard). */
    private var peakProgressDistance: Double = 0.0

    data class RoutePoint(val x: Double, val y: Double, val z: Double)

    /**
     * Configure the estimator with a route and alignment.
     */
    fun configure(
        waypoints: List<RoutePoint>,
        alignment: AlignmentTransform,
    ) {
        routePoints = waypoints
        alignmentTransform = alignment
        peakProgressDistance = 0.0

        // Precompute cumulative distances along route
        val cumDist = mutableListOf(0.0)
        for (i in 1 until waypoints.size) {
            val prev = waypoints[i - 1]
            val curr = waypoints[i]
            val d = dist(prev.x, prev.z, curr.x, curr.z)
            cumDist.add(cumDist.last() + d)
        }
        cumulativeDistances = cumDist
        totalRouteDistance = cumDist.lastOrNull() ?: 0.0
    }

    /**
     * Update progress from a new camera pose.
     *
     * @param cameraPose Camera position in AR-world coordinates
     * @param trackingQuality Current tracking quality
     * @return Progress update with fraction, remaining distance, etc.
     */
    fun update(
        cameraPose: CameraPose,
        trackingQuality: TrackingQuality = TrackingQuality.NORMAL,
    ): ProgressUpdate {
        val transform = alignmentTransform
        if (transform == null || routePoints.size < 2) {
            return ProgressUpdate(
                progressFraction = 0.0,
                remainingDistanceMeters = totalRouteDistance,
                nearestSegmentIndex = 0,
                distanceFromRouteMeters = 0.0,
                isLowConfidence = true,
                timestampMs = cameraPose.timestampMs,
            )
        }

        // 1. Convert AR-world → building-local
        val (bx, by, bz) = transform.inverseTransformPoint(cameraPose.x, cameraPose.y, cameraPose.z)

        // 2. Find nearest point on route polyline
        val projection = projectOntoPolyline(bx, bz)

        // 3. Monotonic guard
        val guardedDistance = if (projection.cumulativeDistance >= peakProgressDistance - monotonicToleranceMeters) {
            maxOf(projection.cumulativeDistance, peakProgressDistance)
        } else {
            peakProgressDistance // reject backwards movement
        }
        peakProgressDistance = guardedDistance

        // 4. Compute progress fraction
        val fraction = if (totalRouteDistance > 0.0) {
            (guardedDistance / totalRouteDistance).coerceIn(0.0, 1.0)
        } else 0.0

        val remaining = (totalRouteDistance - guardedDistance).coerceAtLeast(0.0)
        val isLow = trackingQuality != TrackingQuality.NORMAL ||
                projection.distanceFromRoute > offRouteThresholdMeters

        return ProgressUpdate(
            progressFraction = fraction,
            remainingDistanceMeters = remaining,
            nearestSegmentIndex = projection.segmentIndex,
            distanceFromRouteMeters = projection.distanceFromRoute,
            isLowConfidence = isLow,
            timestampMs = cameraPose.timestampMs,
        )
    }

    /**
     * Update from a simulated/demo progress fraction directly.
     */
    fun updateFromSimulated(progressFraction: Double): ProgressUpdate {
        val clamped = progressFraction.coerceIn(0.0, 1.0)
        val dist = clamped * totalRouteDistance
        peakProgressDistance = maxOf(dist, peakProgressDistance)
        val remaining = (totalRouteDistance - peakProgressDistance).coerceAtLeast(0.0)
        return ProgressUpdate(
            progressFraction = clamped,
            remainingDistanceMeters = remaining,
            nearestSegmentIndex = 0,
            distanceFromRouteMeters = 0.0,
            isLowConfidence = false,
        )
    }

    /**
     * Update the alignment transform after a checkpoint correction.
     *
     * This replaces the current alignment without resetting peak progress,
     * enabling corrected re-projection while preserving monotonic progress.
     */
    fun updateAlignment(newAlignment: AlignmentTransform) {
        alignmentTransform = newAlignment
    }

    /** Reset estimator state (e.g., on recenter/rescan). */
    fun resetProgress() {
        // Don't regress — keep peak on rescan per ADR-018
    }

    /** Force reset (for new session). */
    fun fullReset() {
        peakProgressDistance = 0.0
        routePoints = emptyList()
        cumulativeDistances = emptyList()
        totalRouteDistance = 0.0
        alignmentTransform = null
    }

    // ── Polyline projection ────────────────────────

    private data class Projection(
        val segmentIndex: Int,
        val cumulativeDistance: Double,
        val distanceFromRoute: Double,
    )

    private fun projectOntoPolyline(px: Double, pz: Double): Projection {
        var bestSegIdx = 0
        var bestCumDist = 0.0
        var bestDistFromRoute = Double.MAX_VALUE

        for (i in 0 until routePoints.size - 1) {
            val a = routePoints[i]
            val b = routePoints[i + 1]
            val segLen = dist(a.x, a.z, b.x, b.z)
            if (segLen < 0.001) continue

            // Project point onto segment [a, b]
            val dx = b.x - a.x
            val dz = b.z - a.z
            var t = ((px - a.x) * dx + (pz - a.z) * dz) / (dx * dx + dz * dz)
            t = t.coerceIn(0.0, 1.0)

            val projX = a.x + t * dx
            val projZ = a.z + t * dz
            val distToProj = dist(px, pz, projX, projZ)

            if (distToProj < bestDistFromRoute) {
                bestDistFromRoute = distToProj
                bestSegIdx = i
                bestCumDist = cumulativeDistances[i] + t * segLen
            }
        }

        return Projection(bestSegIdx, bestCumDist, bestDistFromRoute)
    }

    private fun dist(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dz = z2 - z1
        return sqrt(dx * dx + dz * dz)
    }
}
