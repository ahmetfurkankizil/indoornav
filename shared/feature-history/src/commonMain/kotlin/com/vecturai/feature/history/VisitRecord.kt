package com.vecturai.feature.history

import kotlinx.serialization.Serializable

/**
 * A record of a completed or ended navigation visit.
 *
 * Persisted locally and displayed in the History screen.
 */
@Serializable
data class VisitRecord(
    /** Unique visit/session ID. */
    val visitId: String,
    /** Building identifier. */
    val buildingId: String,
    /** Building display name. */
    val buildingName: String = "",
    /** Destination room ID. */
    val roomId: String,
    /** Destination room display name. */
    val roomName: String,
    /** Visit start timestamp (ISO-8601). */
    val visitedAtIso: String,
    /** Visit end timestamp (ISO-8601). */
    val endedAtIso: String? = null,
    /** Completion status label. */
    val completionStatus: String = "COMPLETED_AT_DESTINATION",
    /** Route distance if available. */
    val routeDistanceMeters: Double = 0.0,
    /** Session mode (REAL_SCAN or SIMULATED_SCAN). */
    val mode: String = "REAL_SCAN",
    /** Entrance marker used, if known. */
    val entranceMarkerId: String? = null,
) {
    /** Whether this visit completed successfully. */
    val isCompleted: Boolean
        get() = completionStatus == "COMPLETED_AT_DESTINATION" || completionStatus == "DEMO_COMPLETED"

    /** Whether this was a demo/simulated session. */
    val isDemo: Boolean
        get() = mode == "SIMULATED_SCAN" || completionStatus == "DEMO_COMPLETED"

    /** Display-friendly status label. */
    val statusLabel: String
        get() = when (completionStatus) {
            "COMPLETED_AT_DESTINATION" -> "Completed"
            "DEMO_COMPLETED" -> "Demo"
            "ENDED_MANUALLY" -> "Ended early"
            "CANCELLED_BEFORE_ALIGNMENT" -> "Cancelled"
            "CANCELLED_AFTER_ALIGNMENT" -> "Cancelled"
            "LOST_TRACKING_ENDED" -> "Lost tracking"
            else -> completionStatus
        }
}
