package com.example.vecturai.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vecturai.ar.ArrowPose
import com.example.vecturai.ar.ArrowRenderer
import com.example.vecturai.ar.CloudAnchorHelper
import com.example.vecturai.ar.Correspondence
import com.example.vecturai.ar.Relocalizer
import com.example.vecturai.ar.Vec3
import com.example.vecturai.ar.distanceMeters
import com.example.vecturai.ar.estimateSessionPose
import com.example.vecturai.ar.horizontalDistanceMeters
import com.example.vecturai.ar.translationVec
import com.example.vecturai.graph.EdgeKind
import com.example.vecturai.graph.MapEdge
import com.example.vecturai.graph.MapGraph
import com.example.vecturai.graph.MapNode
import com.example.vecturai.graph.Pathfinder
import com.example.vecturai.persistence.GraphPoseHint
import com.example.vecturai.persistence.GraphRepository
import com.example.vecturai.persistence.LocalizationHint
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
import kotlin.math.abs
import kotlin.math.exp

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
    val isResolved: Boolean,
    val confidence: Float
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
    private var lastDisplayedPoseUpdateNanos: Long? = null
    private var lastYawUpdateNanos: Long? = null
    private var lastSmoothedYawDegrees: Float? = null
    private var lastLocalizationHint: LocalizationHint? = null
    private var lastRerouteAtMs = 0L
    private var lastHintSavedAtMs = 0L
    private val resolvedAnchors = mutableMapOf<String, Anchor>()
    private val displayedNodePoses = mutableMapOf<String, Pose>()
    private val nodeFailureCount = mutableMapOf<String, Int>()
    private val nodeLastAttemptAtMs = mutableMapOf<String, Long>()
    private val anchorResolvedAtMs = mutableMapOf<String, Long>()
    private val anchorPausedSinceMs = mutableMapOf<String, Long>()

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
            resetLocalizationState()
            resetResolveBookkeeping()
            lastLocalizationHint = null

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
            lastLocalizationHint = graphRepository.loadLocalizationHint(buildingName)

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
        resetNavigationSmoothing()
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
        updateAnchorTrackingStates()

        if (trackingState == TrackingState.TRACKING) {
            recomputeGraphToSession()
            refreshDisplayedNodePoses()
        }

        updateCurrentNode(cameraPose)

        if (trackingState == TrackingState.TRACKING) {
            updateNavigationProgress(cameraPose)
        } else if (_uiState.value.phase == NavigationPhase.Navigating) {
            resetNavigationSmoothing()
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

        val initialWaypointIndex = initialWaypointIndex(path)
        resetNavigationSmoothing()
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
    }

    fun pickAnotherDestination() {
        resetNavigationSmoothing()
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
        nodeFailureCount.clear()
        nodeLastAttemptAtMs.clear()
        _uiState.update {
            it.copy(
                phase = if (trackingResolvedAnchorCount() == 0) NavigationPhase.Localizing else it.phase,
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
            while (isActive && trackingResolvedAnchorCount() == 0) {
                _uiState.update {
                    it.copy(statusMessage = "Looking for a nearby anchor for first fix.")
                }

                var resolvedFirstFix = false
                for (node in pendingNodes(graph).take(FIRST_FIX_CANDIDATE_COUNT)) {
                    if (!isActive) break
                    if (resolveNode(activeSession, node)) {
                        resolvedFirstFix = true
                        break
                    }
                }

                if (!resolvedFirstFix && trackingResolvedAnchorCount() == 0) {
                    delay(FIRST_FIX_RETRY_DELAY_MS)
                }
            }

            while (isActive) {
                _uiState.update {
                    it.copy(statusMessage = "Keeping nearby anchors fresh for drift correction.")
                }

                val pending = pendingNodes(graph)
                for (batch in pending.chunked(MAX_PARALLEL_RESOLVES)) {
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

    private fun pendingNodes(graph: MapGraph): List<MapNode> {
        val now = System.currentTimeMillis()
        return nextResolveOrder(graph)
            .filter { it.cloudAnchorId.isNotBlank() }
            .filter { shouldResolveAnchor(it.id, now) }
            .filter { shouldAttempt(it.id, now) }
    }

    private fun nextResolveOrder(graph: MapGraph): List<MapNode> {
        val userInGraph = currentUserGraphPosition()
        val hintPriority = if (graphToSessionPose == null) {
            lastLocalizationHint?.lastResolvedAnchorIds
                ?.withIndex()
                ?.associate { it.value to it.index }
                .orEmpty()
        } else {
            emptyMap()
        }

        return graph.nodes.sortedWith(
            compareBy<MapNode>(
                { hintPriority[it.id] ?: Int.MAX_VALUE },
                {
                    userInGraph?.let { user ->
                        horizontalDistanceMeters(Vec3(it.xMeters, it.yMeters, it.zMeters), user)
                    } ?: Float.MAX_VALUE
                },
                { it.id }
            )
        )
    }

    private fun currentUserGraphPosition(): Vec3? {
        val transform = graphToSessionPose
        val camera = latestCameraPose
        if (transform != null && camera != null) {
            return transform.inverse().compose(camera).translationVec()
        }
        return lastLocalizationHint?.lastUserGraphPose?.let {
            Vec3(it.xMeters, it.yMeters, it.zMeters)
        }
    }

    private fun shouldResolveAnchor(nodeId: String, nowMs: Long): Boolean {
        val existing = resolvedAnchors[nodeId] ?: return true
        return when (existing.trackingState) {
            TrackingState.TRACKING -> false
            TrackingState.PAUSED -> {
                val pausedSince = anchorPausedSinceMs[nodeId] ?: nowMs.also {
                    anchorPausedSinceMs[nodeId] = it
                }
                nowMs - pausedSince >= PAUSED_ANCHOR_RERESOLVE_MS
            }
            TrackingState.STOPPED -> true
        }
    }

    private fun shouldAttempt(nodeId: String, nowMs: Long): Boolean {
        val failures = nodeFailureCount[nodeId] ?: 0
        if (failures >= MAX_RESOLVE_FAILURES) return false
        val lastAttempt = nodeLastAttemptAtMs[nodeId] ?: return true
        return nowMs - lastAttempt >= retryDelayMs(failures)
    }

    private fun retryDelayMs(failures: Int): Long =
        (1L shl failures.coerceAtMost(MAX_RETRY_EXPONENT)) * 1_000L

    private suspend fun resolveNode(session: Session, node: MapNode): Boolean {
        val now = System.currentTimeMillis()
        if (!shouldAttempt(node.id, now)) return false

        nodeLastAttemptAtMs[node.id] = now
        _uiState.update {
            it.copy(resolveAttemptCount = it.resolveAttemptCount + 1)
        }

        val result = CloudAnchorHelper.resolveAnchor(
            session = session,
            cloudAnchorId = node.cloudAnchorId,
            timeoutMs = RESOLVE_TIMEOUT_MS
        )

        return result.fold(
            onSuccess = { anchor ->
                resolvedAnchors[node.id]?.detach()
                resolvedAnchors[node.id] = anchor
                nodeFailureCount.remove(node.id)
                nodeLastAttemptAtMs.remove(node.id)
                anchorPausedSinceMs.remove(node.id)
                anchorResolvedAtMs[node.id] = System.currentTimeMillis()
                publishNodeEstimates("Updated anchor consensus from ${node.label ?: node.id.take(8)}.")
                true
            },
            onFailure = { error ->
                nodeFailureCount.merge(node.id, 1, Int::plus)
                val message = error.message ?: "Anchor resolve failed."
                if (trackingResolvedAnchorCount() == 0) {
                    _uiState.update {
                        it.copy(
                            lastResolveError = message,
                            errorMessage = message
                        )
                    }
                } else {
                    _uiState.update { it.copy(lastResolveError = message) }
                }
                false
            }
        )
    }

    private fun publishNodeEstimates(statusMessage: String) {
        recomputeGraphToSession()
        if (!refreshDisplayedNodePoses(statusMessage)) return
        latestCameraPose?.let {
            updateCurrentNode(it)
            updateNavigationProgress(it)
        }
    }

    private fun recomputeGraphToSession(): Boolean {
        val graph = _uiState.value.graph ?: return false
        val nodesById = graph.nodeById()
        var correspondences = resolvedAnchors.mapNotNull { (nodeId, anchor) ->
            if (anchor.trackingState != TrackingState.TRACKING) return@mapNotNull null
            val node = nodesById[nodeId] ?: return@mapNotNull null
            Correspondence(
                graphPose = node.graphPose(),
                sessionPose = anchor.pose,
                weight = anchorFitConfidence(nodeId)
            )
        }
        if (correspondences.isEmpty()) return false

        var fit = Relocalizer.fitGraphToSession(correspondences) ?: return false
        repeat(2) {
            val pruned = Relocalizer.rejectOutliers(correspondences, fit)
            if (pruned.isNotEmpty()) {
                correspondences = pruned
                fit = Relocalizer.fitGraphToSession(pruned) ?: fit
            }
        }

        val previous = graphToSessionPose
        graphToSessionPose = fit
        if (previous == null) {
            lastDisplayedPoseUpdateNanos = null
        }
        persistLocalizationHintIfNeeded()
        return previous == null || distanceMeters(previous, fit) > GRAPH_FIT_CHANGE_EPSILON_M
    }

    private fun refreshDisplayedNodePoses(statusMessage: String? = null): Boolean {
        val graph = _uiState.value.graph ?: return false
        val transform = graphToSessionPose ?: return false
        val state = _uiState.value
        val alpha = displayedPoseBlendAlpha(state.phase)
        val activeNodeIds = graph.nodes.mapTo(mutableSetOf()) { it.id }
        displayedNodePoses.keys.retainAll(activeNodeIds)

        val estimates = graph.nodes.map { node ->
            val livePose = resolvedAnchors[node.id]
                ?.takeIf { it.trackingState == TrackingState.TRACKING }
                ?.pose
            val targetPose = livePose ?: estimateSessionPose(transform, node)
            val displayedPose = displayedNodePoses[node.id]?.let { previousPose ->
                if (alpha >= 1f) targetPose else lerpPose(previousPose, targetPose, alpha)
            } ?: targetPose
            displayedNodePoses[node.id] = displayedPose

            SessionNodePose(
                nodeId = node.id,
                label = node.label,
                pose = displayedPose,
                isResolved = livePose != null,
                confidence = computeConfidence(node, livePose)
            )
        }

        _uiState.update { current ->
            val trackingResolvedCount = trackingResolvedAnchorCount()
            val nextPhase = if (current.phase == NavigationPhase.Localizing && trackingResolvedCount > 0) {
                NavigationPhase.PickingDestination
            } else {
                current.phase
            }
            current.copy(
                phase = nextPhase,
                nodePoses = estimates,
                resolvedAnchorCount = trackingResolvedCount,
                lastResolveError = if (statusMessage != null) null else current.lastResolveError,
                statusMessage = statusMessage ?: current.statusMessage,
                errorMessage = if (statusMessage != null) null else current.errorMessage
            )
        }
        return true
    }

    private fun anchorFitConfidence(nodeId: String): Float {
        val resolvedAt = anchorResolvedAtMs[nodeId] ?: return 1f
        val ageSeconds = (System.currentTimeMillis() - resolvedAt).coerceAtLeast(0L) / 1_000f
        return exp((-ageSeconds / CONFIDENCE_TIME_DECAY_SECONDS).toDouble())
            .toFloat()
            .coerceIn(MIN_FIT_CONFIDENCE, 1f)
    }

    private fun computeConfidence(node: MapNode, livePose: Pose?): Float {
        if (livePose != null) return 1f
        val nearest = nearestTrackingAnchorDistanceInGraph(node) ?: return 0f
        val latestResolveAgeSeconds = anchorResolvedAtMs.values.maxOrNull()?.let {
            (System.currentTimeMillis() - it).coerceAtLeast(0L) / 1_000f
        } ?: 0f
        val spatial = exp((-nearest / CONFIDENCE_DISTANCE_DECAY_M).toDouble()).toFloat()
        val decay = exp((-latestResolveAgeSeconds / CONFIDENCE_TIME_DECAY_SECONDS).toDouble()).toFloat()
        return (spatial * decay).coerceIn(0f, 1f)
    }

    private fun nearestTrackingAnchorDistanceInGraph(node: MapNode): Float? {
        val graph = _uiState.value.graph ?: return null
        val nodesById = graph.nodeById()
        return resolvedAnchors.mapNotNull { (nodeId, anchor) ->
            if (anchor.trackingState != TrackingState.TRACKING) return@mapNotNull null
            val anchorNode = nodesById[nodeId] ?: return@mapNotNull null
            horizontalDistanceMeters(
                Vec3(node.xMeters, node.yMeters, node.zMeters),
                Vec3(anchorNode.xMeters, anchorNode.yMeters, anchorNode.zMeters)
            )
        }.minOrNull()
    }

    private fun displayedPoseBlendAlpha(phase: NavigationPhase): Float {
        if (phase != NavigationPhase.Navigating) {
            lastDisplayedPoseUpdateNanos = System.nanoTime()
            return 1f
        }

        val now = System.nanoTime()
        val previous = lastDisplayedPoseUpdateNanos
        lastDisplayedPoseUpdateNanos = now
        val elapsedSeconds = previous?.let {
            ((now - it) / NANOS_PER_SECOND).coerceIn(0f, MAX_BLEND_DELTA_SECONDS)
        } ?: return 1f
        return (1f - exp((-DISPLAY_POSE_CATCHUP_HZ * elapsedSeconds).toDouble()).toFloat())
            .coerceIn(0f, 1f)
    }

    private fun lerpPose(from: Pose, to: Pose, alpha: Float): Pose {
        if (alpha <= 0f) return from
        if (alpha >= 1f) return to

        val translation = floatArrayOf(
            lerp(from.tx(), to.tx(), alpha),
            lerp(from.ty(), to.ty(), alpha),
            lerp(from.tz(), to.tz(), alpha)
        )

        var toQx = to.qx()
        var toQy = to.qy()
        var toQz = to.qz()
        var toQw = to.qw()
        val dot = from.qx() * toQx + from.qy() * toQy + from.qz() * toQz + from.qw() * toQw
        if (dot < 0f) {
            toQx = -toQx
            toQy = -toQy
            toQz = -toQz
            toQw = -toQw
        }

        val qx = lerp(from.qx(), toQx, alpha)
        val qy = lerp(from.qy(), toQy, alpha)
        val qz = lerp(from.qz(), toQz, alpha)
        val qw = lerp(from.qw(), toQw, alpha)
        val length = kotlin.math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
        val rotation = if (length > 0f) {
            floatArrayOf(qx / length, qy / length, qz / length, qw / length)
        } else {
            floatArrayOf(to.qx(), to.qy(), to.qz(), to.qw())
        }

        return Pose(translation, rotation)
    }

    private fun lerp(from: Float, to: Float, alpha: Float): Float = from + (to - from) * alpha

    private fun updateCurrentNode(cameraPose: Pose) {
        val state = _uiState.value
        val poses = state.nodePoses
        if (poses.isEmpty()) return

        val cameraPosition = cameraPose.translationVec()
        if (state.phase == NavigationPhase.Navigating && state.path.size > 1) {
            projectToPath(state, cameraPosition.horizontal())?.let { projection ->
                val pathIndex = if (projection.segmentT > 0.5f) {
                    projection.segmentIndex + 1
                } else {
                    projection.segmentIndex
                }.coerceIn(0, state.path.lastIndex)
                _uiState.update { it.copy(currentNodeId = state.path[pathIndex].id) }
                return
            }
        }

        val candidates = poses.filter { it.isResolved }.ifEmpty { poses }
        val closest = candidates.minByOrNull { horizontalDistanceMeters(cameraPosition, it.pose.translationVec()) }
        _uiState.update { it.copy(currentNodeId = closest?.nodeId) }
    }

    private fun updateNavigationProgress(cameraPose: Pose) {
        var state = _uiState.value
        if (state.phase != NavigationPhase.Navigating) return

        if (maybeReroute(state, cameraPose)) {
            state = _uiState.value
        }
        if (state.phase != NavigationPhase.Navigating) return

        val projection = projectToPath(state, cameraPose.translationVec().horizontal())
        val projectedWaypointIndex = projection?.let {
            (it.segmentIndex + 1).coerceAtMost(state.path.lastIndex)
        }
        val targetIndex = maxOf(
            state.currentWaypointIndex,
            projectedWaypointIndex ?: state.currentWaypointIndex
        ).coerceIn(0, state.path.lastIndex)

        if (targetIndex != state.currentWaypointIndex) {
            resetNavigationSmoothing()
            _uiState.update { it.copy(currentWaypointIndex = targetIndex) }
            state = _uiState.value
        }

        val waypoint = state.currentWaypoint ?: return
        val waypointPose = state.nodePoses.firstOrNull { it.nodeId == waypoint.id }?.pose ?: return
        val cameraPosition = cameraPose.translationVec()
        val waypointPosition = waypointPose.translationVec()
        val distance = horizontalDistanceMeters(cameraPosition, waypointPosition)

        if (distance <= ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M) {
            advanceFromWaypoint(state)
            return
        }

        val arrowPose = ArrowRenderer.floatingArrowPose(cameraPose, waypointPosition)
            ?.let { it.copy(yawDegrees = smoothYawDegrees(it.yawDegrees)) }
        _uiState.update {
            it.copy(
                arrowPose = arrowPose,
                distanceToNextMeters = distance,
                distanceToDestinationMeters = distanceToDestination(cameraPose, state),
                statusMessage = navigationStatusMessage(state, waypoint)
            )
        }
    }

    private fun maybeReroute(state: NavigationUiState, cameraPose: Pose): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRerouteAtMs < REROUTE_INTERVAL_MS) return false
        val projection = projectToPath(state, cameraPose.translationVec().horizontal()) ?: return false
        if (projection.perpDist <= REROUTE_DEVIATION_M) return false

        val graph = state.graph ?: return false
        val destinationId = state.selectedDestinationId ?: return false
        val startId = closestNodeIdToCamera(state, cameraPose) ?: return false
        val newPath = Pathfinder(graph).shortestPath(startId, destinationId) ?: return false
        if (newPath.map { it.id } == state.path.map { it.id }) {
            lastRerouteAtMs = now
            return false
        }

        lastRerouteAtMs = now
        resetNavigationSmoothing()
        _uiState.update {
            it.copy(
                path = newPath,
                currentWaypointIndex = initialWaypointIndex(newPath),
                phase = if (newPath.size == 1) NavigationPhase.Arrived else NavigationPhase.Navigating,
                arrowPose = if (newPath.size == 1) null else it.arrowPose,
                distanceToNextMeters = if (newPath.size == 1) 0f else it.distanceToNextMeters,
                distanceToDestinationMeters = if (newPath.size == 1) 0f else it.distanceToDestinationMeters,
                statusMessage = if (newPath.size == 1) "You are already at the destination." else "Route adjusted."
            )
        }
        return true
    }

    private fun projectToPath(state: NavigationUiState, camHorizontal: Vec3): PathProjection? {
        if (state.path.size < 2) return null
        val poses = state.path.map { node ->
            state.nodePoses.firstOrNull { it.nodeId == node.id }?.pose?.translationVec()?.horizontal()
                ?: return null
        }

        var bestIndex = 0
        var bestT = 0f
        var bestDistance = Float.MAX_VALUE
        for (index in 0 until poses.lastIndex) {
            val a = poses[index]
            val b = poses[index + 1]
            val ab = b - a
            val len2 = ab.x * ab.x + ab.z * ab.z
            if (len2 < 1e-4f) continue
            val fromA = camHorizontal - a
            val t = (fromA.x * ab.x + fromA.z * ab.z) / len2
            val clampedT = t.coerceIn(0f, 1f)
            val projected = a + ab * clampedT
            val distance = (camHorizontal - projected).length()
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
                bestT = clampedT
            }
        }

        if (bestDistance == Float.MAX_VALUE) return null
        return PathProjection(
            segmentIndex = bestIndex,
            segmentT = bestT,
            perpDist = bestDistance
        )
    }

    private fun closestNodeIdToCamera(state: NavigationUiState, cameraPose: Pose): String? {
        val cameraPosition = cameraPose.translationVec()
        return state.nodePoses
            .maxByOrNull { it.confidence }
            ?.let { mostConfident ->
                state.nodePoses
                    .filter { it.confidence >= mostConfident.confidence * 0.5f }
                    .minByOrNull { horizontalDistanceMeters(cameraPosition, it.pose.translationVec()) }
            }
            ?.nodeId
            ?: state.nodePoses.minByOrNull { horizontalDistanceMeters(cameraPosition, it.pose.translationVec()) }?.nodeId
    }

    private fun advanceFromWaypoint(state: NavigationUiState) {
        val nextIndex = state.currentWaypointIndex + 1
        if (nextIndex > state.path.lastIndex) {
            resetNavigationSmoothing()
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
            resetNavigationSmoothing()
            _uiState.update {
                it.copy(
                    currentWaypointIndex = nextIndex,
                    statusMessage = "Waypoint reached. Continue forward."
                )
            }
        }
    }

    private fun initialWaypointIndex(path: List<MapNode>): Int = if (path.size > 1) 1 else 0

    private fun navigationStatusMessage(state: NavigationUiState, waypoint: MapNode): String =
        floorTransitionMessage(state, waypoint) ?: "Follow the floating arrow."

    private fun floorTransitionMessage(state: NavigationUiState, waypoint: MapNode): String? {
        val graph = state.graph ?: return null
        val previousRouteNode = state.path.getOrNull(state.currentWaypointIndex - 1)
        val current = previousRouteNode
            ?: state.currentNodeId?.let { graph.nodeById()[it] }
            ?: return null
        if (current.floor == waypoint.floor) return null

        val transitionEdge = graph.edgeBetween(current.id, waypoint.id)
            ?: state.path.getOrNull(state.currentWaypointIndex - 1)?.let { previous ->
                graph.edgeBetween(previous.id, waypoint.id)
            }
        val transport = when (transitionEdge?.kind) {
            EdgeKind.ELEVATOR -> "elevator"
            else -> "stairs"
        }
        return "Take the $transport to floor ${waypoint.floor}."
    }

    private fun MapGraph.edgeBetween(fromId: String, toId: String): MapEdge? =
        edges.firstOrNull {
            it.fromNodeId == fromId && it.toNodeId == toId ||
                it.bidirectional && it.fromNodeId == toId && it.toNodeId == fromId
        }

    private fun distanceToDestination(cameraPose: Pose, state: NavigationUiState): Float? {
        val destination = state.path.lastOrNull() ?: return null
        val destinationPose = state.nodePoses.firstOrNull { it.nodeId == destination.id }?.pose
            ?: return null
        return horizontalDistanceMeters(cameraPose.translationVec(), destinationPose.translationVec())
    }

    private fun smoothYawDegrees(targetYawDegrees: Float): Float {
        val now = System.nanoTime()
        val previousYaw = lastSmoothedYawDegrees
        val previousNanos = lastYawUpdateNanos
        lastYawUpdateNanos = now

        if (previousYaw == null || previousNanos == null) {
            lastSmoothedYawDegrees = targetYawDegrees
            return targetYawDegrees
        }

        val elapsedSeconds = ((now - previousNanos) / NANOS_PER_SECOND)
            .coerceIn(0f, MAX_BLEND_DELTA_SECONDS)
        val alpha = (1f - exp((-YAW_SMOOTHING_HZ * elapsedSeconds).toDouble()).toFloat())
            .coerceIn(0f, 1f)
        val delta = shortestYawDelta(previousYaw, targetYawDegrees)
        val smoothedYaw = if (abs(delta) < 0.1f) {
            targetYawDegrees
        } else {
            normalizeYawDegrees(previousYaw + delta * alpha)
        }
        lastSmoothedYawDegrees = smoothedYaw
        return smoothedYaw
    }

    private fun shortestYawDelta(fromDegrees: Float, toDegrees: Float): Float {
        var delta = (toDegrees - fromDegrees) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    private fun normalizeYawDegrees(degrees: Float): Float {
        var normalized = degrees % 360f
        if (normalized <= -180f) normalized += 360f
        if (normalized > 180f) normalized -= 360f
        return normalized
    }

    private fun updateAnchorTrackingStates() {
        val now = System.currentTimeMillis()
        val stopped = mutableListOf<String>()
        resolvedAnchors.forEach { (nodeId, anchor) ->
            when (anchor.trackingState) {
                TrackingState.TRACKING -> anchorPausedSinceMs.remove(nodeId)
                TrackingState.PAUSED -> anchorPausedSinceMs.putIfAbsent(nodeId, now)
                TrackingState.STOPPED -> stopped += nodeId
            }
        }
        stopped.forEach { nodeId ->
            resolvedAnchors.remove(nodeId)?.detach()
            anchorPausedSinceMs.remove(nodeId)
            anchorResolvedAtMs.remove(nodeId)
        }
    }

    private fun trackingResolvedAnchorCount(): Int =
        resolvedAnchors.count { it.value.trackingState == TrackingState.TRACKING }

    private fun persistLocalizationHintIfNeeded() {
        val state = _uiState.value
        val buildingName = state.selectedBuilding ?: return
        val graph = state.graph ?: return
        val transform = graphToSessionPose ?: return
        val camera = latestCameraPose ?: return
        val now = System.currentTimeMillis()
        if (now - lastHintSavedAtMs < HINT_SAVE_INTERVAL_MS) return

        val userGraphPose = transform.inverse().compose(camera)
        val userGraphPosition = userGraphPose.translationVec()
        val nodesById = graph.nodeById()
        val trackingAnchorIds = resolvedAnchors
            .filter { it.value.trackingState == TrackingState.TRACKING }
            .keys
            .sortedBy { nodeId ->
                nodesById[nodeId]?.let { node ->
                    horizontalDistanceMeters(
                        userGraphPosition,
                        Vec3(node.xMeters, node.yMeters, node.zMeters)
                    )
                } ?: Float.MAX_VALUE
            }
        if (trackingAnchorIds.isEmpty()) return

        lastHintSavedAtMs = now
        val hint = LocalizationHint(
            lastResolvedAnchorIds = trackingAnchorIds,
            lastFitTimestampMs = now,
            lastUserGraphPose = GraphPoseHint(
                xMeters = userGraphPose.tx(),
                yMeters = userGraphPose.ty(),
                zMeters = userGraphPose.tz(),
                qx = userGraphPose.qx(),
                qy = userGraphPose.qy(),
                qz = userGraphPose.qz(),
                qw = userGraphPose.qw()
            )
        )
        lastLocalizationHint = hint
        viewModelScope.launch {
            runCatching { graphRepository.saveLocalizationHint(buildingName, hint) }
        }
    }

    private fun resetLocalizationState() {
        graphToSessionPose = null
        displayedNodePoses.clear()
        lastDisplayedPoseUpdateNanos = null
        resetNavigationSmoothing()
    }

    private fun resetResolveBookkeeping() {
        nodeFailureCount.clear()
        nodeLastAttemptAtMs.clear()
        anchorResolvedAtMs.clear()
        anchorPausedSinceMs.clear()
        lastRerouteAtMs = 0L
        lastHintSavedAtMs = 0L
    }

    private fun resetNavigationSmoothing() {
        lastYawUpdateNanos = null
        lastSmoothedYawDegrees = null
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

    private data class PathProjection(
        val segmentIndex: Int,
        val segmentT: Float,
        val perpDist: Float
    )

    companion object {
        private const val MAX_PARALLEL_RESOLVES = 8
        private const val RESOLVE_TIMEOUT_MS = 20_000L
        private const val RELOCALIZE_INTERVAL_MS = 15_000L
        private const val FIRST_FIX_CANDIDATE_COUNT = 8
        private const val FIRST_FIX_RETRY_DELAY_MS = 2_000L
        private const val MAX_RESOLVE_FAILURES = 6
        private const val MAX_RETRY_EXPONENT = 4
        private const val PAUSED_ANCHOR_RERESOLVE_MS = 10_000L
        private const val HINT_SAVE_INTERVAL_MS = 10_000L
        private const val GRAPH_FIT_CHANGE_EPSILON_M = 0.01f
        private const val MIN_FIT_CONFIDENCE = 0.05f
        private const val CONFIDENCE_DISTANCE_DECAY_M = 8f
        private const val CONFIDENCE_TIME_DECAY_SECONDS = 60f
        private const val REROUTE_INTERVAL_MS = 3_000L
        private const val REROUTE_DEVIATION_M = 3f
        private const val DISPLAY_POSE_CATCHUP_HZ = 4f
        private const val YAW_SMOOTHING_HZ = 8f
        private const val MAX_BLEND_DELTA_SECONDS = 0.25f
        private const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
