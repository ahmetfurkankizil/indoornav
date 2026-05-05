package com.VecturAI.core.navigation

import kotlinx.serialization.Serializable

/**
 * Completion status of a navigation session.
 */
@Serializable
enum class CompletionStatus {
    /** User reached the destination. */
    COMPLETED_AT_DESTINATION,
    /** User manually ended navigation before arriving. */
    ENDED_MANUALLY,
    /** User cancelled before marker alignment. */
    CANCELLED_BEFORE_ALIGNMENT,
    /** User cancelled after alignment but before arriving. */
    CANCELLED_AFTER_ALIGNMENT,
    /** Tracking was lost and session ended. */
    LOST_TRACKING_ENDED,
    /** Demo mode completed (simulated). */
    DEMO_COMPLETED,
}

/**
 * Navigation session mode.
 */
@Serializable
enum class SessionMode {
    REAL_SCAN,
    SIMULATED_SCAN,
}
