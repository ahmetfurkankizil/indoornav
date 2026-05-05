package com.VecturAI.core.ar

import kotlinx.serialization.Serializable

/**
 * AR session state machine.
 *
 * Observed by both shared UI (for textual feedback) and native AR
 * (for session management decisions). State transitions are driven
 * by [ArNavigationCoordinator].
 */
@Serializable
sealed class ArSessionState {

    /** No AR session active. */
    @Serializable
    data object Idle : ArSessionState()

    /** AR session running, waiting for marker detection. */
    @Serializable
    data class WaitingForMarker(
        val buildingId: String,
        val destinationName: String,
    ) : ArSessionState()

    /** Marker detected but not yet stable/aligned. */
    @Serializable
    data class MarkerDetected(
        val markerId: String,
    ) : ArSessionState()

    /** World alignment established. Ready to render. */
    @Serializable
    data class Aligned(
        val markerId: String,
        val entranceNodeId: String,
    ) : ArSessionState()

    /** Tracking quality degraded (e.g., insufficient features, fast motion). */
    @Serializable
    data class TrackingLimited(
        val reason: String,
    ) : ArSessionState()

    /** Actively rendering the route in AR. */
    @Serializable
    data class RenderingRoute(
        val arrowCount: Int,
        val currentInstruction: String,
        val distanceRemainingMeters: Double,
    ) : ArSessionState()

    /** AR session error. */
    @Serializable
    data class Error(
        val message: String,
        val recoverable: Boolean = true,
    ) : ArSessionState()
}
