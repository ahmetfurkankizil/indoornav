package com.VecturAI.core.navigation

import com.VecturAI.core.ar.ArNavigationCoordinator
import com.VecturAI.core.ar.ArSessionState
import com.VecturAI.core.domain.NavigationState
import com.VecturAI.core.domain.Room
import com.VecturAI.core.domain.BuildingPackage
import com.VecturAI.core.repository.HistoryRepository
import com.VecturAI.core.store.AppStore
import com.VecturAI.core.domain.VisitRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates the full navigation session lifecycle.
 *
 * Owns:
 * - Session creation and tracking
 * - Arrival detection
 * - Session completion/cancellation
 * - History write-through on session end
 *
 * Delegates AR-specific work to [ArNavigationCoordinator].
 */
class NavigationSessionCoordinator(
    private val arCoordinator: ArNavigationCoordinator,
    private val historyRepository: HistoryRepository,
    private val arrivalDetector: ArrivalDetector,
    private val appStore: AppStore,
) {

    private val _currentSession = MutableStateFlow<NavigationSession?>(null)
    val currentSession: StateFlow<NavigationSession?> = _currentSession.asStateFlow()

    private val _arrivalStatus = MutableStateFlow<ArrivalStatus>(ArrivalStatus.NotArrived)
    val arrivalStatus: StateFlow<ArrivalStatus> = _arrivalStatus.asStateFlow()

    private val _sessionSummary = MutableStateFlow<NavigationSession?>(null)
    /** Set after session ends — drives the summary screen. */
    val sessionSummary: StateFlow<NavigationSession?> = _sessionSummary.asStateFlow()

    private var sessionCounter = 0

    /**
     * Start a new navigation session.
     */
    suspend fun startSession(
        buildingPackage: BuildingPackage,
        destination: Room,
        startNodeId: String,
        mode: SessionMode = SessionMode.REAL_SCAN,
    ): Boolean {
        val sessionId = "session-${++sessionCounter}"
        val now = currentIsoTimestamp()

        val session = NavigationSession(
            sessionId = sessionId,
            buildingId = buildingPackage.manifest.buildingId,
            buildingName = buildingPackage.manifest.buildingName,
            destinationRoomId = destination.id,
            destinationDisplayName = destination.name,
            startedAtIso = now,
            mode = mode,
        )

        _currentSession.value = session
        _arrivalStatus.value = ArrivalStatus.NotArrived
        _sessionSummary.value = null

        // Delegate route preparation to AR coordinator
        val prepared = arCoordinator.prepareNavigation(buildingPackage, destination, startNodeId)
        if (prepared) {
            val route = arCoordinator.renderableRoute.value
            _currentSession.value = session.copy(
                routeDistanceMeters = route?.totalDistanceMeters ?: 0.0,
                routeStepCount = route?.arrows?.size ?: 0,
            )
        }

        return prepared
    }

    /**
     * Start the AR session (after route is prepared).
     */
    fun startArSession() {
        arCoordinator.startArSession()
    }

    /**
     * Handle marker alignment from native AR.
     */
    fun onMarkerAligned(markerId: String, entranceNodeId: String) {
        _currentSession.value = _currentSession.value?.copy(entranceMarkerId = markerId)
        // Delegate to AR coordinator (which handles the actual alignment)
    }

    /**
     * Update session progress.
     *
     * @param progressFraction 0.0–1.0 fraction of route completed
     * @param distanceToDestMeters Optional distance to destination
     */
    fun updateProgress(progressFraction: Double, distanceToDestMeters: Double? = null) {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(progressFraction = progressFraction)

        val status = arrivalDetector.check(
            progressFraction = progressFraction,
            distanceToDestMeters = distanceToDestMeters,
            totalRouteDistanceMeters = session.routeDistanceMeters,
        )
        _arrivalStatus.value = status

        // Auto-complete on arrival
        if (status is ArrivalStatus.Arrived) {
            val completionStatus = if (session.mode == SessionMode.SIMULATED_SCAN) {
                CompletionStatus.DEMO_COMPLETED
            } else {
                CompletionStatus.COMPLETED_AT_DESTINATION
            }
            endSession(completionStatus)
        }
    }

    /**
     * End the current session with the given status.
     */
    fun endSession(status: CompletionStatus) {
        val session = _currentSession.value ?: return
        val now = currentIsoTimestamp()

        val completed = session.copy(
            endedAtIso = now,
            completionStatus = status,
        )

        _currentSession.value = null
        _sessionSummary.value = completed
        _arrivalStatus.value = ArrivalStatus.NotArrived

        // Stop AR
        arCoordinator.stopSession()
        appStore.updateNavigationState(
            if (status == CompletionStatus.COMPLETED_AT_DESTINATION || status == CompletionStatus.DEMO_COMPLETED) {
                NavigationState.Arrived(
                    route = arCoordinator.renderableRoute.value?.let { renderable ->
                        com.VecturAI.core.domain.Route(
                            originNodeId = "",
                            destinationNodeId = completed.destinationRoomId,
                            segments = emptyList(),
                            totalDistanceMeters = completed.routeDistanceMeters,
                            estimatedTimeSeconds = 0,
                        )
                    } ?: com.VecturAI.core.domain.Route(
                        originNodeId = "",
                        destinationNodeId = completed.destinationRoomId,
                        segments = emptyList(),
                        totalDistanceMeters = 0.0,
                        estimatedTimeSeconds = 0,
                    ),
                    destinationRoom = com.VecturAI.core.domain.Room(
                        id = completed.destinationRoomId,
                        name = completed.destinationDisplayName,
                    ),
                )
            } else {
                NavigationState.Idle
            }
        )

        // Write to history asynchronously
        persistSession(completed)
    }

    /**
     * Cancel the current session.
     */
    fun cancelSession() {
        val session = _currentSession.value ?: return
        val arState = arCoordinator.sessionState.value
        val status = when (arState) {
            is ArSessionState.Idle, is ArSessionState.WaitingForMarker ->
                CompletionStatus.CANCELLED_BEFORE_ALIGNMENT
            else -> CompletionStatus.CANCELLED_AFTER_ALIGNMENT
        }
        endSession(status)
    }

    /**
     * Clear the session summary (after user dismisses summary screen).
     */
    fun clearSummary() {
        _sessionSummary.value = null
        appStore.updateNavigationState(NavigationState.Idle)
    }

    // ── Demo helpers ────────────────────────────

    /**
     * Simulate full arrival for demo mode.
     */
    fun simulateArrival() {
        _arrivalStatus.value = arrivalDetector.forceArrival()
        val session = _currentSession.value ?: return
        endSession(CompletionStatus.DEMO_COMPLETED)
    }

    /**
     * Advance progress by a fixed step (for demo).
     */
    fun advanceProgress(step: Double = 0.15) {
        val session = _currentSession.value ?: return
        val newProgress = (session.progressFraction + step).coerceAtMost(1.0)
        updateProgress(newProgress)
    }

    // ── Internals ───────────────────────────────

    private fun persistSession(session: NavigationSession) {
        val record = VisitRecord(
            visitId = session.sessionId,
            buildingId = session.buildingId,
            buildingName = session.buildingName,
            roomId = session.destinationRoomId,
            roomName = session.destinationDisplayName,
            visitedAtIso = session.startedAtIso,
            endedAtIso = session.endedAtIso,
            completionStatus = session.completionStatus?.name ?: "UNKNOWN",
            routeDistanceMeters = session.routeDistanceMeters,
            mode = session.mode.name,
            entranceMarkerId = session.entranceMarkerId,
        )
        // Use runBlocking equivalent or fire-and-forget
        // In real KMP, this would use a coroutine scope
        kotlinx.coroutines.runBlocking {
            historyRepository.addVisit(record)
        }
    }

    private fun currentIsoTimestamp(): String {
        // Platform-neutral ISO timestamp
        return kotlinx.datetime.Clock.System.now().toString()
    }
}
