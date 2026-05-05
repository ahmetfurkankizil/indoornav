package com.VecturAI.core.ar

import com.VecturAI.core.domain.*
import com.VecturAI.core.repository.BuildingRepository
import com.VecturAI.core.routing.RouteEngine
import com.VecturAI.core.store.AppStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared coordinator for AR navigation sessions.
 *
 * Owns the AR session state machine and coordinates between:
 * - Route computation (shared)
 * - Arrow placement generation (shared)
 * - Native AR session management (via [ArNavigationBridge])
 * - App store state updates (for UI)
 *
 * Lifecycle:
 * 1. `prepareNavigation()` — computes route, generates arrows
 * 2. `startArSession()` — transitions to WaitingForMarker
 * 3. `onMarkerAligned()` — called by native on marker detection
 * 4. State transitions to RenderingRoute, arrows are available
 * 5. `stopSession()` — cleanup
 */
class ArNavigationCoordinator(
    private val buildingRepository: BuildingRepository,
    private val routeEngine: RouteEngine,
    private val arrowMapper: RouteToArrowMapper,
    private val appStore: AppStore,
    private val correctionCoordinator: CorrectionCoordinator = CorrectionCoordinator(),
    private val offRouteDetector: OffRouteDetector = OffRouteDetector(),
) {

    // ── State ────────────────────────────────────────────

    private val _sessionState = MutableStateFlow<ArSessionState>(ArSessionState.Idle)
    val sessionState: StateFlow<ArSessionState> = _sessionState.asStateFlow()

    private val _renderableRoute = MutableStateFlow<ArRenderableRoute?>(null)
    val renderableRoute: StateFlow<ArRenderableRoute?> = _renderableRoute.asStateFlow()

    private val _alignmentTransform = MutableStateFlow<AlignmentTransform?>(null)
    val alignmentTransform: StateFlow<AlignmentTransform?> = _alignmentTransform.asStateFlow()

    private val _currentInstruction = MutableStateFlow("")
    val currentInstruction: StateFlow<String> = _currentInstruction.asStateFlow()

    /** Confidence state including correction info, off-route status, recommendations. */
    val confidenceState: StateFlow<NavigationConfidenceState> = correctionCoordinator.confidenceState

    private var bridge: ArNavigationBridge? = null
    private var currentRoute: Route? = null
    private var currentPackage: BuildingPackage? = null
    private var currentDestination: Room? = null

    /** Register the native AR bridge. */
    fun setBridge(bridge: ArNavigationBridge?) {
        this.bridge = bridge
    }

    // ── Lifecycle ────────────────────────────────────────

    /**
     * Prepare navigation to a destination.
     * Computes route and generates arrow placements BEFORE AR alignment.
     *
     * @return true if route was successfully computed
     */
    suspend fun prepareNavigation(
        buildingPackage: BuildingPackage,
        destination: Room,
        startNodeId: String,
    ): Boolean {
        currentPackage = buildingPackage
        currentDestination = destination

        val graph = buildingPackage.navGraph
        val destNodeId = destination.entryNodeIds.firstOrNull() ?: return false
        val route = routeEngine.computeRoute(graph, startNodeId, destNodeId) ?: return false
        val routeWithRoom = route.copy(destinationRoom = destination)
        currentRoute = routeWithRoom

        // Generate arrow placements
        val renderable = arrowMapper.map(routeWithRoom, graph, buildingPackage.renderingConfig)
        _renderableRoute.value = renderable.copy(
            currentInstruction = routeWithRoom.segments.firstOrNull()?.instruction ?: "Head forward",
        )

        _currentInstruction.value = routeWithRoom.segments.firstOrNull()?.instruction ?: "Head to entrance marker"

        // Update app store
        appStore.selectDestination(destination)
        appStore.updateNavigationState(
            NavigationState.Scanning(
                buildingId = buildingPackage.manifest.buildingId,
                targetRoom = destination,
            )
        )

        return true
    }

    /**
     * Start the AR session. Expects route to be prepared first.
     */
    fun startArSession() {
        val pkg = currentPackage ?: return
        val dest = currentDestination ?: return

        _sessionState.value = ArSessionState.WaitingForMarker(
            buildingId = pkg.manifest.buildingId,
            destinationName = dest.name,
        )

        bridge?.startSession(pkg, dest)
        bridge?.onSessionStateChanged(_sessionState.value)
    }

    /**
     * Called by native layer when entrance marker is detected and aligned.
     *
     * Computes alignment transform and transitions to rendering state.
     */
    fun onMarkerAligned(result: MarkerAlignmentResult) {
        _sessionState.value = ArSessionState.MarkerDetected(result.markerId)

        // Compute alignment
        val transform = AlignmentTransform.fromMarkerAlignment(result)
        _alignmentTransform.value = transform

        // Transition to aligned
        _sessionState.value = ArSessionState.Aligned(
            markerId = result.markerId,
            entranceNodeId = result.entranceNodeId,
        )

        // Update app store to navigating
        currentRoute?.let { route ->
            appStore.updateNavigationState(NavigationState.Navigating(route = route))
        }

        // Notify bridge
        bridge?.onAlignmentEstablished(transform)

        // Transition to rendering
        val renderable = _renderableRoute.value ?: return
        _sessionState.value = ArSessionState.RenderingRoute(
            arrowCount = renderable.arrows.size,
            currentInstruction = renderable.currentInstruction,
            distanceRemainingMeters = renderable.totalDistanceMeters,
        )

        bridge?.onRenderableRouteUpdated(renderable)
        bridge?.onSessionStateChanged(_sessionState.value)

        // Configure correction coordinator with checkpoint markers
        val pkg = currentPackage
        if (pkg != null && pkg.checkpointMarkers.isNotEmpty()) {
            correctionCoordinator.configure(pkg.checkpointMarkers, transform)
        }
    }

    /**
     * Called by native when AR tracking quality changes.
     */
    fun updateTrackingState(isLimited: Boolean, reason: String = "") {
        if (isLimited) {
            _sessionState.value = ArSessionState.TrackingLimited(reason)
        } else if (_sessionState.value is ArSessionState.TrackingLimited) {
            // Restore to rendering if we have a route
            val renderable = _renderableRoute.value
            if (renderable != null) {
                _sessionState.value = ArSessionState.RenderingRoute(
                    arrowCount = renderable.arrows.size,
                    currentInstruction = renderable.currentInstruction,
                    distanceRemainingMeters = renderable.totalDistanceMeters,
                )
            }
        }
        bridge?.onSessionStateChanged(_sessionState.value)
    }

    /**
     * Stop the AR session and reset state.
     */
    /**
     * Called by native layer when a checkpoint marker is observed during navigation.
     *
     * Computes and applies bounded alignment correction.
     * Does NOT restart the session or reset progress.
     */
    fun onCheckpointMarkerObserved(event: MarkerObservationEvent) {
        val result = correctionCoordinator.onCheckpointObserved(event)
        if (result.applied && result.newAlignment != null) {
            _alignmentTransform.value = result.newAlignment
            bridge?.onAlignmentEstablished(result.newAlignment)

            // Update renderable route state with correction info
            val renderable = _renderableRoute.value
            if (renderable != null) {
                _sessionState.value = ArSessionState.RenderingRoute(
                    arrowCount = renderable.arrows.size,
                    currentInstruction = renderable.currentInstruction,
                    distanceRemainingMeters = renderable.totalDistanceMeters,
                )
                bridge?.onSessionStateChanged(_sessionState.value)
            }
        }
    }

    fun stopSession() {
        bridge?.stopSession()
        _sessionState.value = ArSessionState.Idle
        _alignmentTransform.value = null
        _renderableRoute.value = null
        _currentInstruction.value = ""
        currentRoute = null
        currentPackage = null
        currentDestination = null
        correctionCoordinator.reset()
        offRouteDetector.reset()
    }

    // ── Debug / Simulation ───────────────────────────────

    /**
     * Simulate marker alignment for development without live AR.
     *
     * Uses the first entrance marker from the loaded package
     * with identity alignment (marker at building origin).
     */
    fun simulateAlignment() {
        val pkg = currentPackage ?: return
        val marker = pkg.entranceMarkers.firstOrNull() ?: return

        val fakeResult = MarkerAlignmentResult(
            markerId = marker.id,
            entranceNodeId = marker.nearestNodeId,
            markerBuildingX = marker.positionX,
            markerBuildingY = marker.positionY,
            markerBuildingZ = marker.positionZ,
            markerArX = marker.positionX,
            markerArY = marker.positionY,
            markerArZ = marker.positionZ,
            markerArRotationYDeg = marker.rotationYDegrees,
            markerBuildingRotationYDeg = marker.rotationYDegrees,
            confidence = 1.0,
        )

        onMarkerAligned(fakeResult)
    }

    /**
     * Get a JSON dump of current renderable route for debugging.
     */
    fun debugDumpRenderableRoute(): String {
        val renderable = _renderableRoute.value ?: return "{}"
        return kotlinx.serialization.json.Json {
            prettyPrint = true
            encodeDefaults = true
        }.encodeToString(ArRenderableRoute.serializer(), renderable)
    }
}
