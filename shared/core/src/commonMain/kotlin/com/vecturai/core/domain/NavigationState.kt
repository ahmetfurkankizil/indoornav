package com.Vectura AI.core.domain

/**
 * Represents the current state of the navigation flow.
 *
 * This sealed class defines all possible states the navigation system
 * can be in. It is observed by both the Compose UI (for textual guidance)
 * and the native AR shells (for 3D rendering).
 *
 * State transitions:
 * ```
 * Idle → Scanning → Navigating → Arrived
 *                  ↗ (re-scan)
 *        Scanning ←─── Navigating (if alignment lost)
 * ```
 */
sealed class NavigationState {

    /** No active navigation session. Default state. */
    data object Idle : NavigationState()

    /**
     * Waiting for the user to scan the entrance marker.
     *
     * @property buildingId Building being navigated
     * @property targetRoom The room the user wants to reach
     */
    data class Scanning(
        val buildingId: String,
        val targetRoom: Room,
    ) : NavigationState()

    /**
     * Actively navigating with AR guidance.
     *
     * @property route The computed route being followed
     * @property currentSegmentIndex Index of the current segment in the route
     * @property progressFraction Progress along current segment (0.0 to 1.0)
     */
    data class Navigating(
        val route: Route,
        val currentSegmentIndex: Int = 0,
        val progressFraction: Double = 0.0,
    ) : NavigationState() {
        val currentSegment: RouteSegment?
            get() = route.segments.getOrNull(currentSegmentIndex)

        val isLastSegment: Boolean
            get() = currentSegmentIndex >= route.segments.lastIndex
    }

    /**
     * User has reached the destination.
     *
     * @property route The completed route
     * @property destinationRoom The room that was reached
     */
    data class Arrived(
        val route: Route,
        val destinationRoom: Room,
    ) : NavigationState()

    /**
     * An error occurred during navigation.
     *
     * @property message Error description
     * @property recoverable Whether the user can retry
     */
    data class Error(
        val message: String,
        val recoverable: Boolean = true,
    ) : NavigationState()
}
