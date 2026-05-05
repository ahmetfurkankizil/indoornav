package com.VecturAI.core.ar

import kotlinx.serialization.Serializable

/**
 * Role of a marker in the AR navigation system.
 */
@Serializable
enum class MarkerRole {
    /** Initializes the AR session and establishes world alignment. */
    ENTRANCE,
    /** Provides mid-route alignment correction without restarting the session. */
    CHECKPOINT,
}

/**
 * Alignment confidence — how trustworthy the current building↔AR transform is.
 */
@Serializable
enum class AlignmentConfidence {
    /** Fresh marker alignment, minimal drift expected. */
    HIGH,
    /** Some time since last alignment; moderate drift possible. */
    MODERATE,
    /** Significant time or distance since alignment; drift likely. */
    LOW,
    /** No alignment established. */
    NONE,
}

/**
 * Progress confidence — how trustworthy the current progress estimate is.
 */
@Serializable
enum class ProgressConfidence {
    /** On-route, tracking normal, recent alignment. */
    RELIABLE,
    /** Tracking normal but alignment aging or minor drift. */
    ESTIMATED,
    /** Tracking limited, off-route, or stale pose data. */
    DEGRADED,
}

/**
 * Off-route status — lateral deviation from the route polyline.
 */
@Serializable
enum class OffRouteStatus {
    /** Within expected corridor (< 2 m). */
    ON_ROUTE,
    /** Drifting slightly (2–4 m). */
    MINOR_DRIFT,
    /** Significant deviation (4–6 m). */
    LOW_CONFIDENCE,
    /** Likely off-route (> 6 m). */
    LIKELY_OFF_ROUTE,
    /** Off-route + additional signals — recovery recommended. */
    RECOVERY_RECOMMENDED,
}

/**
 * User-facing recovery recommendation.
 *
 * Passive guidance — the system never auto-cancels.
 */
@Serializable
enum class RecoveryRecommendation {
    /** Everything is fine, keep walking. */
    CONTINUE,
    /** Suggest user scan the nearest marker. */
    RESCAN_MARKER,
    /** Suggest user walk back toward the route. */
    MOVE_TOWARD_ROUTE,
    /** Debug/demo fallback — switch to simulated mode. */
    USE_DEMO_MODE,
}

/**
 * Composite navigation confidence state.
 *
 * Consumed by UI and diagnostics panel for actionable feedback.
 */
@Serializable
data class NavigationConfidenceState(
    val alignmentConfidence: AlignmentConfidence = AlignmentConfidence.NONE,
    val progressConfidence: ProgressConfidence = ProgressConfidence.DEGRADED,
    val offRouteStatus: OffRouteStatus = OffRouteStatus.ON_ROUTE,
    val recommendation: RecoveryRecommendation = RecoveryRecommendation.CONTINUE,
    val lastCorrectionTimeMs: Long = 0L,
    val correctionCount: Int = 0,
    val correctionMagnitudeMeters: Double = 0.0,
    val stalePoseMs: Long = 0L,
    val lastMarkerIdSeen: String? = null,
    val lastMarkerRole: MarkerRole? = null,
)
