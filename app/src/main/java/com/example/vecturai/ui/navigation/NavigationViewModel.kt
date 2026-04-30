package com.example.vecturai.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vecturai.ar.ArrowPose
import com.example.vecturai.ar.ArrowRenderer
import com.example.vecturai.ar.CloudAnchorHelper
import com.example.vecturai.ar.distanceMeters
import com.example.vecturai.ar.estimateSessionPose
import com.example.vecturai.ar.horizontalDistanceMeters
import com.example.vecturai.ar.sessionFromGraphPose
import com.example.vecturai.ar.translationVec
import com.example.vecturai.graph.MapGraph
import com.example.vecturai.graph.MapNode
import com.example.vecturai.graph.Pathfinder
import com.example.vecturai.persistence.GraphRepository
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class NavigationPhase {
    Loading,
    NoGraphs,
    SelectBuilding,
    Localizing,
    PickingDestination,
    Navigating,
    Arrived
}

data class SessionNodePose(
    val nodeId: String,
    val label: String?,
    val pose: Pose,
    val isResolved: Boolean
)

data class NavigationUiState(
    val phase: NavigationPhase = NavigationPhase.Loading,
    val availableBuildings: List<String> = emptyList(),
    val selectedBuilding: String? = null,
    val graph: MapGraph? = null,
    val trackingState: String = "Waiting for AR session",
    val resolvedAnchorCount: Int = 0,
    val currentNodeId: String? = null,
    val selectedDestinationId: String? = null,
    val path: List<MapNode> = emptyList(),
    val currentWaypointIndex: Int = 0,
    val nodePoses: List<SessionNodePose> = emptyList(),
    val resolveAttemptCount: Int = 0,
    val lastResolveError: String? = null,
    val arrowPose: ArrowPose? = null,
    val distanceToNextMeters: Float? = null,
    val distanceToDestinationMeters: Float? = null,
    val statusMessage: String = "Loading saved maps...",
    val errorMessage: String? = null
) {
    val destinationNodes: List<MapNode>
        get() = graph?.labeledNodes.orEmpty()

    val currentWaypoint: MapNode?
        get() = path.getOrNull(currentWaypointIndex)
}

class NavigationViewModel(
    private val graphRepository: GraphRepository
) : ViewModel() {
    private var session: Session? = null
    private var latestCameraPose: Pose? = null
    private var resolveJob: Job? = null
    private var graphToSessionPose: Pose? = null
    private var hasReceivedArFrame = false
    private val resolvedAnchors = mutableMapOf<String, Anchor>()

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    init {
        refreshBuildings()
    }

    fun refreshBuildings() {
        viewModelScope.launch {
            _uiState.update { it.copy(phase = NavigationPhase.Loading) }
            val buildings = graphRepository.listBuildings()
            _uiState.update {
                it.copy(
                    availableBuildings = buildings,
                    phase = if (buildings.isEmpty()) NavigationPhase.NoGraphs else NavigationPhase.SelectBuilding,
                    statusMessage = if (buildings.isEmpty()) {
                        "No saved building graphs yet. Map a building first."
                    } else {
                        "Choose a saved building graph."
                    }
                )
            }
            if (buildings.size == 1) {
                selectBuilding(buildings.first())
            }
        }
    }

    fun selectBuilding(buildingName: String) {
        viewModelScope.launch {
            resolveJob?.cancel()
            resolvedAnchors.values.forEach { it.detach() }
            resolvedAnchors.clear()
            graphToSessionPose = null

            _uiState.update {
                it.copy(
                    selectedBuilding = buildingName,
                    phase = NavigationPhase.Loading,
                    graph = null,
                    nodePoses = emptyList(),
                    path = emptyList(),
                    arrowPose = null,
                    distanceToNextMeters = null,
                    distanceToDestinationMeters = null,
                    resolvedAnchorCount = 0,
                    resolveAttemptCount = 0,
                    lastResolveError = null,
                    statusMessage = "Loading $buildingName..."
                )
            }

            val graph = graphRepository.load(buildingName)
            if (graph == null) {
                _uiState.update {
                    it.copy(
                        phase = NavigationPhase.SelectBuilding,
                        errorMessage = "Could not load $buildingName."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    graph = graph,
                    phase = NavigationPhase.Localizing,
                    statusMessage = "Look around slowly while anchors resolve.",
                    errorMessage = null
                )
            }
            startResolveLoopIfReady()
        }
    }

    fun onSessionCreated(session: Session) {
        this.session = session
        _uiState.update {
            it.copy(statusMessage = "AR session created. Waiting for camera frame before resolving anchors.")
        }
    }

    fun onSessionFailed(error: Throwable) {
        resolveJob?.cancel()
        resolveJob = null
        _uiState.update {
            it.copy(
                trackingState = "AR session failed",
                arrowPose = null,
                statusMessage = "AR session could not start.",
                errorMessage = error.message ?: error::class.java.simpleName
            )
        }
    }

    fun onSessionUpdated(session: Session, frame: Frame) {
        this.session = session
        if (!hasReceivedArFrame) {
            hasReceivedArFrame = true
            startResolveLoopIfReady()
        }
        val cameraPose = frame.camera.pose
        latestCameraPose = cameraPose

        val trackingState = frame.camera.trackingState
        _uiState.update { it.copy(trackingState = trackingState.name) }
        updateCurrentNode(cameraPose)

        if (trackingState == TrackingState.TRACKING) {
            updateNavigationProgress(cameraPose)
        } else if (_uiState.value.phase == NavigationPhase.Navigating) {
            _uiState.update {
                it.copy(
                    arrowPose = null,
                    statusMessage = "Move slowly to recover AR tracking."
                )
            }
        }
    }

    fun selectDestination(destinationNodeId: String) {
        val state = _uiState.value
        val graph = state.graph ?: return
        val startNodeId = state.currentNodeId
            ?: resolvedAnchors.keys.firstOrNull()
            ?: graph.nodes.firstOrNull()?.id
            ?: return

        val path = Pathfinder(graph).shortestPath(startNodeId, destinationNodeId)
        if (path.isNullOrEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "No route found to the selected room.")
            }
            return
        }

        val initialWaypointIndex = if (path.size > 1) 1 else 0
        _uiState.update {
            it.copy(
                phase = if (path.size == 1) NavigationPhase.Arrived else NavigationPhase.Navigating,
                selectedDestinationId = destinationNodeId,
                path = path,
                currentWaypointIndex = initialWaypointIndex,
                distanceToNextMeters = if (path.size == 1) 0f else null,
                distanceToDestinationMeters = if (path.size == 1) 0f else null,
                statusMessage = if (path.size == 1) "You are already at the destination." else "Follow the floating arrow.",
                errorMessage = null
            )
        }
        latestCameraPose?.let { updateNavigationProgress(it) }
    }

    fun pickAnotherDestination() {
        _uiState.update {
            it.copy(
                phase = NavigationPhase.PickingDestination,
                selectedDestinationId = null,
                path = emptyList(),
                currentWaypointIndex = 0,
                arrowPose = null,
                distanceToNextMeters = null,
                distanceToDestinationMeters = null,
                statusMessage = "Pick another room."
            )
        }
    }

    fun relocalizeNow() {
        resolveJob?.cancel()
        resolveJob = null
        _uiState.update {
            it.copy(
                phase = if (it.resolvedAnchorCount == 0) NavigationPhase.Localizing else it.phase,
                statusMessage = "Resolving anchors again..."
            )
        }
        startResolveLoopIfReady()
    }

    private fun startResolveLoopIfReady() {
        val activeSession = session ?: return
        val graph = _uiState.value.graph ?: return
        if (!hasReceivedArFrame) return
        if (resolveJob?.isActive == true || graph.nodes.isEmpty()) return

        resolveJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(
                        statusMessage = if (resolvedAnchors.isEmpty()) {
                            "Look around slowly while anchors resolve."
                        } else {
                            "Keeping anchors resolving for drift correction."
                        }
                    )
                }

                for (batch in graph.nodes.chunked(MAX_PARALLEL_RESOLVES)) {
                    coroutineScope {
                        batch.map { node ->
                            async { resolveNode(activeSession, node) }
                        }.awaitAll()
                    }
                }

                delay(RELOCALIZE_INTERVAL_MS)
            }
        }
    }

    private suspend fun resolveNode(session: Session, node: MapNode) {
        _uiState.update {
            it.copy(resolveAttemptCount = it.resolveAttemptCount + 1)
        }

        val result = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
            CloudAnchorHelper.resolveAnchor(session, node.cloudAnchorId)
        } ?: Result.failure(IllegalStateException("Resolve timed out for ${node.id.take(8)}"))

        result
            .onSuccess { anchor ->
                resolvedAnchors[node.id]?.detach()
                resolvedAnchors[node.id] = anchor
                graphToSessionPose = sessionFromGraphPose(node, anchor.pose)
                publishNodeEstimates("Re-localized from ${node.label ?: node.id.take(8)}.")
            }
            .onFailure { error ->
                val message = error.message ?: "Anchor resolve failed."
                if (resolvedAnchors.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            lastResolveError = message,
                            errorMessage = message
                        )
                    }
                } else {
                    _uiState.update { it.copy(lastResolveError = message) }
                }
            }
    }

    private fun publishNodeEstimates(statusMessage: String) {
        val graph = _uiState.value.graph ?: return
        val transform = graphToSessionPose ?: return
        val estimates = graph.nodes.map { node ->
            val resolvedPose = resolvedAnchors[node.id]?.pose
            SessionNodePose(
                nodeId = node.id,
                label = node.label,
                pose = resolvedPose ?: estimateSessionPose(transform, node),
                isResolved = resolvedPose != null
            )
        }

        _uiState.update { state ->
            val nextPhase = if (state.phase == NavigationPhase.Localizing && resolvedAnchors.isNotEmpty()) {
                NavigationPhase.PickingDestination
            } else {
                state.phase
            }
            state.copy(
                phase = nextPhase,
                nodePoses = estimates,
                resolvedAnchorCount = resolvedAnchors.size,
                lastResolveError = null,
                statusMessage = statusMessage,
                errorMessage = null
            )
        }
        latestCameraPose?.let {
            updateCurrentNode(it)
            updateNavigationProgress(it)
        }
    }

    private fun updateCurrentNode(cameraPose: Pose) {
        val poses = _uiState.value.nodePoses
        if (poses.isEmpty()) return
        val cameraPosition = cameraPose.translationVec()
        val closest = poses.minByOrNull { horizontalDistanceMeters(cameraPosition, it.pose.translationVec()) }
        _uiState.update { it.copy(currentNodeId = closest?.nodeId) }
    }

    private fun updateNavigationProgress(cameraPose: Pose) {
        val state = _uiState.value
        if (state.phase != NavigationPhase.Navigating) return
        val waypoint = state.currentWaypoint ?: return
        val waypointPose = state.nodePoses.firstOrNull { it.nodeId == waypoint.id }?.pose ?: return
        val cameraPosition = cameraPose.translationVec()
        val waypointPosition = waypointPose.translationVec()
        val distance = distanceMeters(cameraPosition, waypointPosition)

        if (distance <= ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M) {
            val nextIndex = state.currentWaypointIndex + 1
            if (nextIndex > state.path.lastIndex) {
                _uiState.update {
                    it.copy(
                        phase = NavigationPhase.Arrived,
                        arrowPose = null,
                        distanceToNextMeters = 0f,
                        distanceToDestinationMeters = 0f,
                        statusMessage = "You have arrived."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentWaypointIndex = nextIndex,
                        statusMessage = "Waypoint reached. Continue forward."
                    )
                }
            }
            return
        }

        _uiState.update {
            it.copy(
                arrowPose = ArrowRenderer.floatingArrowPose(cameraPose, waypointPosition),
                distanceToNextMeters = distance,
                distanceToDestinationMeters = distanceToDestination(cameraPose, state),
                statusMessage = "Follow the floating arrow."
            )
        }
    }

    private fun distanceToDestination(cameraPose: Pose, state: NavigationUiState): Float? {
        val destination = state.path.lastOrNull() ?: return null
        val destinationPose = state.nodePoses.firstOrNull { it.nodeId == destination.id }?.pose
            ?: return null
        return distanceMeters(cameraPose, destinationPose)
    }

    override fun onCleared() {
        resolveJob?.cancel()
        resolvedAnchors.values.forEach { it.detach() }
        resolvedAnchors.clear()
        super.onCleared()
    }

    class Factory(
        private val graphRepository: GraphRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NavigationViewModel(graphRepository) as T
        }
    }

    companion object {
        private const val MAX_PARALLEL_RESOLVES = 40
        private const val RESOLVE_TIMEOUT_MS = 20_000L
        private const val RELOCALIZE_INTERVAL_MS = 15_000L
    }
}
