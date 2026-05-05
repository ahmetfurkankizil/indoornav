package com.VecturAI.core.navigation

import kotlinx.serialization.Serializable

/**
 * Arrival detection status.
 */
@Serializable
sealed class ArrivalStatus {
    /** Not yet near destination. */
    @Serializable
    data object NotArrived : ArrivalStatus()

    /** Close to destination (within approach threshold). */
    @Serializable
    data class ApproachingDestination(
        val distanceRemainingMeters: Double,
        val progressFraction: Double,
    ) : ArrivalStatus()

    /** At the destination. */
    @Serializable
    data object Arrived : ArrivalStatus()
}

/**
 * Detects arrival at the destination based on route progress.
 *
 * V1 uses arrow-progress-based detection:
 * - ≥ approachThreshold (default 0.80) → ApproachingDestination
 * - ≥ arrivalThreshold (default 0.95)  → Arrived
 *
 * This is honest about v1 limitations: progress is an estimate,
 * not derived from precise camera position. The architecture
 * allows dropping in real VIO-based proximity detection later.
 */
class ArrivalDetector(
    private val approachThreshold: Double = 0.80,
    private val arrivalThreshold: Double = 0.95,
    private val destinationDistanceMeters: Double = 1.5,
) {

    /**
     * Check arrival status given current progress and optional distance to destination.
     *
     * @param progressFraction Fraction of route completed (0.0 to 1.0)
     * @param distanceToDestMeters Optional: estimated distance to destination node
     * @param totalRouteDistanceMeters Total route distance for remaining estimate
     * @return Current arrival status
     */
    fun check(
        progressFraction: Double,
        distanceToDestMeters: Double? = null,
        totalRouteDistanceMeters: Double = 0.0,
    ): ArrivalStatus {
        val clamped = progressFraction.coerceIn(0.0, 1.0)

        // Distance-based arrival (if available)
        if (distanceToDestMeters != null && distanceToDestMeters <= destinationDistanceMeters) {
            return ArrivalStatus.Arrived
        }

        // Progress-based
        if (clamped >= arrivalThreshold) {
            return ArrivalStatus.Arrived
        }

        if (clamped >= approachThreshold) {
            val remaining = totalRouteDistanceMeters * (1.0 - clamped)
            return ArrivalStatus.ApproachingDestination(
                distanceRemainingMeters = remaining,
                progressFraction = clamped,
            )
        }

        return ArrivalStatus.NotArrived
    }

    /**
     * Force arrival (for debug/demo mode).
     */
    fun forceArrival(): ArrivalStatus = ArrivalStatus.Arrived
}
