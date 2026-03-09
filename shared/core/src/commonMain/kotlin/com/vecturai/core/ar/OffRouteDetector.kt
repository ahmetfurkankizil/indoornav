package com.vecturai.core.ar

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects off-route conditions based on multiple signals.
 *
 * Signals:
 * - Lateral deviation from route polyline
 * - Stale pose updates (time since last pose)
 * - Tracking-limited state from native AR
 * - Repeated large correction magnitudes
 *
 * Produces an [OffRouteStatus] and [RecoveryRecommendation].
 * Does NOT auto-cancel sessions — recommendations are passive.
 */
class OffRouteDetector(
    /** Minor drift threshold (meters). */
    private val minorDriftThreshold: Double = 2.0,
    /** Low confidence threshold (meters). */
    private val lowConfidenceThreshold: Double = 4.0,
    /** Likely off-route threshold (meters). */
    private val offRouteThreshold: Double = 6.0,
    /** Stale pose threshold (milliseconds). */
    private val stalePoseThresholdMs: Long = 3000L,
    /** Large correction threshold (meters) — repeated large corrections suggest instability. */
    private val largeCorrectionThreshold: Double = 1.5,
) {

    private var lastPoseTimeMs: Long = 0L
    private var trackingLimited: Boolean = false
    private var consecutiveLargeCorrections: Int = 0
    private var lastDistanceFromRoute: Double = 0.0

    /**
     * Update with a new pose observation.
     */
    fun onPoseUpdate(
        distanceFromRouteMeters: Double,
        isTrackingLimited: Boolean,
        currentTimeMs: Long,
    ): OffRouteAssessment {
        lastDistanceFromRoute = distanceFromRouteMeters
        trackingLimited = isTrackingLimited
        lastPoseTimeMs = currentTimeMs

        return assess(currentTimeMs)
    }

    /**
     * Notify that a correction was applied.
     */
    fun onCorrectionApplied(magnitudeMeters: Double) {
        if (magnitudeMeters > largeCorrectionThreshold) {
            consecutiveLargeCorrections++
        } else {
            consecutiveLargeCorrections = 0
        }
    }

    /**
     * Notify of tracking state change.
     */
    fun onTrackingStateChanged(isLimited: Boolean) {
        trackingLimited = isLimited
    }

    /**
     * Assess the current off-route state.
     */
    fun assess(currentTimeMs: Long): OffRouteAssessment {
        val stalePoseMs = if (lastPoseTimeMs > 0) currentTimeMs - lastPoseTimeMs else 0L
        val isStalePose = stalePoseMs > stalePoseThresholdMs

        // Determine off-route status
        val status = when {
            // Recovery recommended: off-route + additional bad signals
            lastDistanceFromRoute > offRouteThreshold && (trackingLimited || isStalePose) ->
                OffRouteStatus.RECOVERY_RECOMMENDED
            lastDistanceFromRoute > offRouteThreshold && consecutiveLargeCorrections >= 2 ->
                OffRouteStatus.RECOVERY_RECOMMENDED
            // Likely off-route
            lastDistanceFromRoute > offRouteThreshold ->
                OffRouteStatus.LIKELY_OFF_ROUTE
            // Low confidence
            lastDistanceFromRoute > lowConfidenceThreshold || (isStalePose && trackingLimited) ->
                OffRouteStatus.LOW_CONFIDENCE
            // Minor drift
            lastDistanceFromRoute > minorDriftThreshold || trackingLimited ->
                OffRouteStatus.MINOR_DRIFT
            // On route
            else ->
                OffRouteStatus.ON_ROUTE
        }

        // Determine recommendation
        val recommendation = when (status) {
            OffRouteStatus.ON_ROUTE -> RecoveryRecommendation.CONTINUE
            OffRouteStatus.MINOR_DRIFT -> RecoveryRecommendation.CONTINUE
            OffRouteStatus.LOW_CONFIDENCE -> RecoveryRecommendation.RESCAN_MARKER
            OffRouteStatus.LIKELY_OFF_ROUTE -> RecoveryRecommendation.MOVE_TOWARD_ROUTE
            OffRouteStatus.RECOVERY_RECOMMENDED -> RecoveryRecommendation.RESCAN_MARKER
        }

        return OffRouteAssessment(
            status = status,
            recommendation = recommendation,
            distanceFromRouteMeters = lastDistanceFromRoute,
            stalePoseMs = stalePoseMs,
            isTrackingLimited = trackingLimited,
            consecutiveLargeCorrections = consecutiveLargeCorrections,
        )
    }

    /**
     * Reset detector state (e.g., on new session).
     */
    fun reset() {
        lastPoseTimeMs = 0L
        trackingLimited = false
        consecutiveLargeCorrections = 0
        lastDistanceFromRoute = 0.0
    }
}

/**
 * Result of off-route assessment.
 */
data class OffRouteAssessment(
    val status: OffRouteStatus,
    val recommendation: RecoveryRecommendation,
    val distanceFromRouteMeters: Double,
    val stalePoseMs: Long,
    val isTrackingLimited: Boolean,
    val consecutiveLargeCorrections: Int,
)
