package com.example.vecturai.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.google.ar.core.Plane
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
    val routeVisualState: RouteVisualState = RouteVisualState.Empty,
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
    private var lastLocalizationHint: LocalizationHint? = null
    private var lastRerouteAtMs = 0L
    private var lastHintSavedAtMs = 0L
    private var lastFitAtMs = 0L
    private var lastDetectedFloorY: Float? = null
    private var lastRouteSignature: String? = null
    private var lastRouteVisualFloor: Int? = null
    private var lastRouteProjectionNanos: Long? = null
    private var lastSmoothedRouteCumulativeMeters: Float? = null
    private var lastTransitionCueKey: String? = null
    private val resolvedAnchors = mutableMapOf<String, Anchor>()
    private val displayedNodePoses = mutableMapOf<String, Pose>()
    private val lastFitAnchorPositions = mutableMapOf<String, Vec3>()
    private val nodeFailureCount = mutableMapOf<String, Int>()
    private val nodeLastAttemptAtMs = mutableMapOf<String, Long>()
    private val anchorResolvedAtMs = mutableMapOf<String, Long>()
    private val anchorPausedSinceMs = mutableMapOf<String, Long>()
    private val floorHeightEstimator = FloorHeightEstimator()

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
                    routeVisualState = RouteVisualState.Empty,
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
                routeVisualState = RouteVisualState.Empty,
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
        lastDetectedFloorY = if (trackingState == TrackingState.TRACKING) {
            detectFloorY(session, cameraPose)
        } else {
            null
        }
        updateAnchorTrackingStates()
        var state = _uiState.value.copy(trackingState = trackingState.name)

        if (trackingState == TrackingState.TRACKING) {
            val now = System.currentTimeMillis()
            val anchorsMoved = resolvedAnchors.any { (id, anchor) ->
                if (anchor.trackingState != TrackingState.TRACKING) return@any false
                val previous = lastFitAnchorPositions[id] ?: return@any true
                distanceMeters(anchor.pose.translationVec(), previous) > GRAPH_FIT_LIVE_ANCHOR_MOVE_M
            }
            val due = now - lastFitAtMs >= GRAPH_FIT_MIN_INTERVAL_MS
            val changed = if (due || anchorsMoved) {
                recomputeGraphToSession().also { lastFitAtMs = now }
            } else {
                false
            }
            if (changed || due || anchorsMoved) {
                buildDisplayedNodePoses(state.phase)?.let { estimates ->
                    state = state.withDisplayedNodePoses(estimates)
                }
            }
        }

        computeCurrentNodeId(state, cameraPose)?.let { currentNodeId ->
            state = state.copy(currentNodeId = currentNodeId)
        }

        if (trackingState == TrackingState.TRACKING) {
            state = updatedNavigationProgressState(state, cameraPose)
        } else if (state.phase == NavigationPhase.Navigating) {
            resetNavigationSmoothing()
            state = state.copy(
                routeVisualState = RouteVisualState.Empty,
                statusMessage = "Move slowly to recover AR tracking."
            )
        }

        _uiState.value = state
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
                it.copy(
                    routeVisualState = RouteVisualState.Empty,
                    distanceToNextMeters = null,
                    distanceToDestinationMeters = null,
                    errorMessage = "No route found to the selected room."
                )
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
                routeVisualState = RouteVisualState.Empty,
                distanceToNextMeters = if (path.size == 1) 0f else null,
                distanceToDestinationMeters = if (path.size == 1) 0f else null,
                statusMessage = if (path.size == 1) "You are already at the destination." else "Follow the blue floor arrows.",
                errorMessage = null
            )
        }
        latestCameraPose?.let { cameraPose ->
            if (path.size > 1) updateNavigationProgress(cameraPose)
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
                routeVisualState = RouteVisualState.Empty,
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
        lastFitAtMs = System.currentTimeMillis()
        if (!refreshDisplayedNodePoses(statusMessage)) return
        latestCameraPose?.let { updateCurrentNode(it) }
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
        var outlierPass = 0
        while (outlierPass < 2) {
            outlierPass++
            val pruned = Relocalizer.rejectOutliers(correspondences, fit)
            if (pruned.isEmpty() || pruned.size == correspondences.size) break
            correspondences = pruned
            fit = Relocalizer.fitGraphToSession(pruned) ?: fit
        }

        val previous = graphToSessionPose
        graphToSessionPose = fit
        lastFitAnchorPositions.clear()
        resolvedAnchors.forEach { (id, anchor) ->
            if (anchor.trackingState == TrackingState.TRACKING) {
                lastFitAnchorPositions[id] = anchor.pose.translationVec()
            }
        }
        if (previous == null) {
            lastDisplayedPoseUpdateNanos = null
        }
        persistLocalizationHintIfNeeded()
        return previous == null || distanceMeters(previous, fit) > GRAPH_FIT_CHANGE_EPSILON_M
    }

    private fun refreshDisplayedNodePoses(statusMessage: String? = null): Boolean {
        val state = _uiState.value
        val estimates = buildDisplayedNodePoses(state.phase) ?: return false
        _uiState.value = state
            .withDisplayedNodePoses(estimates)
            .let { current ->
                current.copy(
                    lastResolveError = if (statusMessage != null) null else current.lastResolveError,
                    statusMessage = statusMessage ?: current.statusMessage,
                    errorMessage = if (statusMessage != null) null else current.errorMessage
                )
            }
        return true
    }

    private fun buildDisplayedNodePoses(phase: NavigationPhase): List<SessionNodePose>? {
        val graph = _uiState.value.graph ?: return null
        val transform = graphToSessionPose ?: return null
        val alpha = displayedPoseBlendAlpha(phase)
        val activeNodeIds = graph.nodes.mapTo(mutableSetOf()) { it.id }
        displayedNodePoses.keys.retainAll(activeNodeIds)

        return graph.nodes.map { node ->
            val livePose = resolvedAnchors[node.id]
                ?.takeIf { it.trackingState == TrackingState.TRACKING }
                ?.pose
            val targetPose = livePose ?: estimateSessionPose(transform, node)
            val displayedPose = if (livePose != null) {
                targetPose
            } else {
                displayedNodePoses[node.id]?.let { previousPose ->
                    if (alpha >= 1f) targetPose else lerpPose(previousPose, targetPose, alpha)
                } ?: targetPose
            }
            displayedNodePoses[node.id] = displayedPose

            SessionNodePose(
                nodeId = node.id,
                label = node.label,
                pose = displayedPose,
                isResolved = livePose != null,
                confidence = computeConfidence(node, livePose)
            )
        }
    }

    private fun NavigationUiState.withDisplayedNodePoses(estimates: List<SessionNodePose>): NavigationUiState {
        val trackingResolvedCount = trackingResolvedAnchorCount()
        val nextPhase = if (phase == NavigationPhase.Localizing && trackingResolvedCount > 0) {
            NavigationPhase.PickingDestination
        } else {
            phase
        }
        return copy(
            phase = nextPhase,
            nodePoses = estimates,
            resolvedAnchorCount = trackingResolvedCount
        )
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
            lastDisplayedPoseUpdateNanos = null
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
            floatArrayOf(toQx, toQy, toQz, toQw)
        }

        return Pose(translation, rotation)
    }

    private fun lerp(from: Float, to: Float, alpha: Float): Float = from + (to - from) * alpha

    private fun guidancePose(node: MapNode, transform: Pose): Pose {
        val livePose = resolvedAnchors[node.id]
            ?.takeIf { it.trackingState == TrackingState.TRACKING }
            ?.pose
        return livePose ?: estimateSessionPose(transform, node)
    }

    private fun updateCurrentNode(cameraPose: Pose) {
        val currentNodeId = computeCurrentNodeId(_uiState.value, cameraPose) ?: return
        _uiState.update { it.copy(currentNodeId = currentNodeId) }
    }

    private fun computeCurrentNodeId(state: NavigationUiState, cameraPose: Pose): String? {
        val cameraPosition = cameraPose.translationVec()
        if (state.phase == NavigationPhase.Navigating && state.path.size > 1) {
            projectToPath(state, cameraPose)?.let { projection ->
                val pathIndex = if (projection.segmentT > 0.5f) {
                    projection.segmentIndex + 1
                } else {
                    projection.segmentIndex
                }.coerceIn(0, state.path.lastIndex)
                return state.path[pathIndex].id
            }
        }

        val transform = graphToSessionPose
        val graph = state.graph
        if (transform != null && graph != null) {
            val confidenceById = state.nodePoses.associate { it.nodeId to it.confidence }
            return graph.nodes.minByOrNull { node ->
                val position = guidancePose(node, transform).translationVec()
                val confidence = confidenceById[node.id] ?: 1f
                horizontalDistanceMeters(cameraPosition, position) / (0.5f + 0.5f * confidence)
            }?.id
        }

        if (state.nodePoses.isEmpty()) return null
        val candidates = state.nodePoses.filter { it.isResolved }.ifEmpty { state.nodePoses }
        return candidates.minByOrNull {
            horizontalDistanceMeters(cameraPosition, it.pose.translationVec())
        }?.nodeId
    }

    private fun updateNavigationProgress(cameraPose: Pose) {
        _uiState.value = updatedNavigationProgressState(_uiState.value, cameraPose)
    }

    private fun updatedNavigationProgressState(
        inputState: NavigationUiState,
        cameraPose: Pose
    ): NavigationUiState {
        var state = inputState
        if (state.phase != NavigationPhase.Navigating) return state

        reroutedState(state, cameraPose)?.let { rerouted ->
            state = rerouted
        }
        if (state.phase != NavigationPhase.Navigating) return state

        val transform = graphToSessionPose ?: return state.copy(routeVisualState = RouteVisualState.Empty)
        val graph = state.graph ?: return state.copy(routeVisualState = RouteVisualState.Empty)
        val cameraPosition = cameraPose.translationVec()
        val userFloor = inferUserFloor(state, cameraPose)
        val routeNodes = buildRouteSampleNodes(state, transform)
        val routeEdges = buildRouteSampleEdges(state, graph)
        val projection = RouteVisualSampler.projectToPath(
            nodes = routeNodes,
            edges = routeEdges,
            userPosition = cameraPosition,
            currentFloor = userFloor
        )
        val projectedWaypointIndex = projection?.let {
            (it.segmentIndex + 1).coerceAtMost(state.path.lastIndex)
        }
        val targetIndex = maxOf(
            state.currentWaypointIndex,
            projectedWaypointIndex ?: state.currentWaypointIndex
        ).coerceIn(0, state.path.lastIndex)

        if (targetIndex != state.currentWaypointIndex) {
            resetNavigationSmoothing()
            state = state.copy(currentWaypointIndex = targetIndex)
        }

        val waypoint = state.currentWaypoint ?: return state
        val targetWaypointPose = guidancePose(waypoint, transform)
        val waypointPosition = targetWaypointPose.translationVec()
        val horizontalDistance = horizontalDistanceMeters(cameraPosition, waypointPosition)
        val previousNode = state.path.getOrNull(state.currentWaypointIndex - 1)
        val isVertical = isVerticalEdgeBetween(graph, previousNode, waypoint) ||
            (previousNode != null && previousNode.floor != waypoint.floor)
        val passedWaypointByProjection = projection?.let {
            it.segmentIndex >= state.currentWaypointIndex
        } == true
        val shouldAdvance = (passedWaypointByProjection || horizontalDistance <= WAYPOINT_ADVANCE_DISTANCE_M) &&
            (!isVertical || waypoint.floor == userFloor)

        if (shouldAdvance) {
            return advancedFromWaypointState(state)
        }

        val smoothedProjection = projection?.let {
            it.copy(
                cumulativeMeters = smoothRouteProjectionMeters(
                    rawMeters = it.cumulativeMeters,
                    routeSignature = routeSignature(state.path),
                    floor = userFloor
                )
            )
        }
        val floorEstimate = smoothedProjection?.let {
            floorHeightEstimator.estimate(
                floor = userFloor,
                routeCameraHeightY = routeCameraHeightAtProjection(routeNodes, it) ?: cameraPosition.y,
                cameraY = cameraPosition.y,
                detectedFloorY = lastDetectedFloorY
            )
        }
        val routeVisualState = if (smoothedProjection != null && floorEstimate != null) {
            RouteVisualSampler.sample(
                nodes = routeNodes,
                edges = routeEdges,
                projection = smoothedProjection,
                currentFloor = userFloor,
                floorHeightEstimate = floorEstimate
            )
        } else {
            RouteVisualState.Empty
        }
        resetRouteProjectionIfTransitionChanged(routeVisualState.transitionCue)

        val visualDistances = smoothedProjection?.let {
            RouteVisualSampler.distances(
                nodes = routeNodes,
                edges = routeEdges,
                projection = it,
                currentFloor = userFloor
            )
        }
        val distanceToNext = visualDistances?.distanceToNextMeters ?: if (isVertical) {
            distanceMeters(cameraPosition, waypointPosition)
        } else {
            horizontalDistance
        }
        return state.copy(
            routeVisualState = routeVisualState,
            distanceToNextMeters = distanceToNext,
            distanceToDestinationMeters = visualDistances?.distanceToDestinationMeters
                ?: distanceToDestination(cameraPose, state),
            statusMessage = navigationStatusMessage(
                state = state,
                waypoint = waypoint,
                routeNodes = routeNodes,
                routeEdges = routeEdges,
                projection = smoothedProjection,
                currentFloor = userFloor,
                routeVisualState = routeVisualState
            )
        )
    }

    private fun reroutedState(state: NavigationUiState, cameraPose: Pose): NavigationUiState? {
        val now = System.currentTimeMillis()
        if (now - lastRerouteAtMs < REROUTE_INTERVAL_MS) return null
        val projection = projectToPath(state, cameraPose) ?: return null
        if (projection.perpDist <= REROUTE_DEVIATION_M) return null

        val graph = state.graph ?: return null
        val destinationId = state.selectedDestinationId ?: return null
        val startId = closestNodeIdToCamera(state, cameraPose) ?: return null
        val newPath = Pathfinder(graph).shortestPath(startId, destinationId) ?: return null
        if (newPath.map { it.id } == state.path.map { it.id }) {
            lastRerouteAtMs = now
            return null
        }

        lastRerouteAtMs = now
        resetNavigationSmoothing()
        return state.copy(
            path = newPath,
            currentWaypointIndex = initialWaypointIndex(newPath),
            phase = if (newPath.size == 1) NavigationPhase.Arrived else NavigationPhase.Navigating,
            routeVisualState = RouteVisualState.Empty,
            distanceToNextMeters = if (newPath.size == 1) 0f else state.distanceToNextMeters,
            distanceToDestinationMeters = if (newPath.size == 1) 0f else state.distanceToDestinationMeters,
            statusMessage = if (newPath.size == 1) "You are already at the destination." else "Route adjusted."
        )
    }

    private fun projectToPath(state: NavigationUiState, cameraPose: Pose): RouteProjection? {
        val transform = graphToSessionPose ?: return null
        val graph = state.graph ?: return null
        if (state.path.size < 2) return null
        val userFloor = inferUserFloor(state, cameraPose)
        return RouteVisualSampler.projectToPath(
            nodes = buildRouteSampleNodes(state, transform),
            edges = buildRouteSampleEdges(state, graph),
            userPosition = cameraPose.translationVec(),
            currentFloor = userFloor
        )
    }

    private fun buildRouteSampleNodes(
        state: NavigationUiState,
        transform: Pose
    ): List<RouteSampleNode> =
        state.path.map { node ->
            RouteSampleNode(
                id = node.id,
                position = guidancePose(node, transform).translationVec(),
                floor = node.floor
            )
        }

    private fun buildRouteSampleEdges(
        state: NavigationUiState,
        graph: MapGraph
    ): List<RouteSampleEdge> =
        state.path.zipWithNext().map { (from, to) ->
            RouteSampleEdge(
                fromId = from.id,
                toId = to.id,
                kind = graph.edgeBetween(from.id, to.id)?.kind ?: EdgeKind.STAIRS
            )
        }

    private fun routeCameraHeightAtProjection(
        routeNodes: List<RouteSampleNode>,
        projection: RouteProjection
    ): Float? {
        val from = routeNodes.getOrNull(projection.segmentIndex) ?: return null
        val to = routeNodes.getOrNull(projection.segmentIndex + 1) ?: return from.position.y
        return lerp(from.position.y, to.position.y, projection.segmentT.coerceIn(0f, 1f))
    }

    private fun smoothRouteProjectionMeters(
        rawMeters: Float,
        routeSignature: String,
        floor: Int
    ): Float {
        val now = System.nanoTime()
        if (lastRouteSignature != routeSignature || lastRouteVisualFloor != floor) {
            lastRouteSignature = routeSignature
            lastRouteVisualFloor = floor
            lastRouteProjectionNanos = now
            lastSmoothedRouteCumulativeMeters = rawMeters
            return rawMeters
        }

        val previous = lastSmoothedRouteCumulativeMeters
        val previousNanos = lastRouteProjectionNanos
        lastRouteProjectionNanos = now
        if (previous == null || previousNanos == null) {
            lastSmoothedRouteCumulativeMeters = rawMeters
            return rawMeters
        }

        if (abs(rawMeters - previous) < ROUTE_PROJECTION_DEADBAND_M) {
            return previous
        }

        val elapsedSeconds = ((now - previousNanos) / NANOS_PER_SECOND)
            .coerceIn(0f, MAX_BLEND_DELTA_SECONDS)
        val alpha = (1f - exp((-ROUTE_PROJECTION_SMOOTHING_HZ * elapsedSeconds).toDouble()).toFloat())
            .coerceIn(0f, 1f)
        val smoothed = lerp(previous, rawMeters, alpha)
        lastSmoothedRouteCumulativeMeters = smoothed
        return smoothed
    }

    private fun resetRouteProjectionIfTransitionChanged(cue: RouteTransitionCue?) {
        val cueKey = cue?.let { "${it.kind}:${it.fromFloor}:${it.toFloor}" }
        if (lastTransitionCueKey != cueKey) {
            lastRouteProjectionNanos = null
            lastSmoothedRouteCumulativeMeters = null
        }
        lastTransitionCueKey = cueKey
    }

    private fun routeSignature(path: List<MapNode>): String =
        path.joinToString(separator = ">") { it.id }

    private fun detectFloorY(session: Session, cameraPose: Pose): Float? {
        val cameraY = cameraPose.ty()
        return runCatching { session.getAllTrackables(Plane::class.java) }
            .getOrDefault(emptyList<Plane>())
            .asSequence()
            .filter { it.trackingState == TrackingState.TRACKING }
            .filter { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
            .map { it.centerPose.ty() }
            .filter { floorY -> cameraY - floorY in MIN_CAMERA_TO_FLOOR_M..MAX_CAMERA_TO_FLOOR_M }
            .minByOrNull { floorY -> abs((cameraY - floorY) - DEFAULT_CAMERA_TO_FLOOR_M) }
    }

    private fun closestNodeIdToCamera(state: NavigationUiState, cameraPose: Pose): String? {
        val cameraPosition = cameraPose.translationVec()
        val transform = graphToSessionPose
        val graph = state.graph
        if (transform != null && graph != null) {
            return graph.nodes.minByOrNull {
                horizontalDistanceMeters(cameraPosition, guidancePose(it, transform).translationVec())
            }?.id
        }
        if (state.nodePoses.isEmpty()) return null
        val resolved = state.nodePoses.filter { it.isResolved }
        val candidates = if (resolved.isNotEmpty()) resolved else state.nodePoses
        return candidates.minByOrNull {
            horizontalDistanceMeters(cameraPosition, it.pose.translationVec())
        }?.nodeId
    }

    private fun advancedFromWaypointState(state: NavigationUiState): NavigationUiState {
        val nextIndex = state.currentWaypointIndex + 1
        return if (nextIndex > state.path.lastIndex) {
            resetNavigationSmoothing()
            state.copy(
                phase = NavigationPhase.Arrived,
                routeVisualState = RouteVisualState.Empty,
                distanceToNextMeters = 0f,
                distanceToDestinationMeters = 0f,
                statusMessage = "You have arrived."
            )
        } else {
            resetNavigationSmoothing()
            state.copy(
                currentWaypointIndex = nextIndex,
                routeVisualState = RouteVisualState.Empty,
                statusMessage = "Waypoint reached. Continue forward."
            )
        }
    }

    private fun initialWaypointIndex(path: List<MapNode>): Int = if (path.size > 1) 1 else 0

    private fun navigationStatusMessage(
        state: NavigationUiState,
        waypoint: MapNode,
        routeNodes: List<RouteSampleNode>,
        routeEdges: List<RouteSampleEdge>,
        projection: RouteProjection?,
        currentFloor: Int,
        routeVisualState: RouteVisualState
    ): String {
        routeVisualState.transitionCue?.let { cue ->
            return transitionMessage(cue)
        }
        floorTransitionMessage(state, waypoint)?.let { return it }
        projection?.let {
            when (RouteVisualSampler.turnDirectionNearProjection(routeNodes, routeEdges, it, currentFloor)) {
                RouteTurnDirection.LEFT -> return "Turn left and follow the blue floor arrows."
                RouteTurnDirection.RIGHT -> return "Turn right and follow the blue floor arrows."
                null -> Unit
            }
        }
        return "Follow the blue floor arrows."
    }

    private fun transitionMessage(cue: RouteTransitionCue): String {
        val transport = when (cue.kind) {
            EdgeKind.ELEVATOR -> "elevator"
            else -> "stairs"
        }
        return "Take the $transport to floor ${cue.toFloor}."
    }

    private fun floorTransitionMessage(state: NavigationUiState, waypoint: MapNode): String? {
        if (state.currentWaypointIndex <= 0) return null
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

    private fun isVerticalEdgeBetween(graph: MapGraph, a: MapNode?, b: MapNode?): Boolean {
        if (a == null || b == null) return false
        val edge = graph.edgeBetween(a.id, b.id) ?: return a.floor != b.floor
        return edge.kind == EdgeKind.STAIRS || edge.kind == EdgeKind.ELEVATOR
    }

    private fun inferUserFloor(state: NavigationUiState, cameraPose: Pose): Int {
        val transform = graphToSessionPose ?: return state.currentWaypoint?.floor ?: 0
        val cameraY = cameraPose.translationVec().y
        return state.graph?.nodes
            ?.minByOrNull { abs(guidancePose(it, transform).ty() - cameraY) }
            ?.floor ?: 0
    }

    private fun distanceToDestination(cameraPose: Pose, state: NavigationUiState): Float? {
        val transform = graphToSessionPose ?: return null
        if (state.path.isEmpty()) return null

        val poses = state.path.map { guidancePose(it, transform).translationVec() }
        val projection = projectToPath(state, cameraPose) ?: run {
            val waypointIndex = state.currentWaypointIndex.coerceIn(0, state.path.lastIndex)
            var sum = distanceMeters(cameraPose.translationVec(), poses[waypointIndex])
            for (index in waypointIndex until poses.lastIndex) {
                sum += distanceMeters(poses[index], poses[index + 1])
            }
            return sum
        }
        val segmentIndex = projection.segmentIndex
        val segmentLength = distanceMeters(poses[segmentIndex], poses[segmentIndex + 1])
        var sum = segmentLength * (1f - projection.segmentT.coerceIn(0f, 1f))
        for (index in segmentIndex + 1 until poses.lastIndex) {
            sum += distanceMeters(poses[index], poses[index + 1])
        }
        return sum
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
        lastFitAtMs = 0L
        lastFitAnchorPositions.clear()
        lastDetectedFloorY = null
        floorHeightEstimator.reset()
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
        lastDisplayedPoseUpdateNanos = null
        lastRouteSignature = null
        lastRouteVisualFloor = null
        lastRouteProjectionNanos = null
        lastSmoothedRouteCumulativeMeters = null
        lastTransitionCueKey = null
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
        private const val MAX_PARALLEL_RESOLVES = 8
        private const val RESOLVE_TIMEOUT_MS = 20_000L
        private const val RELOCALIZE_INTERVAL_MS = 15_000L
        private const val FIRST_FIX_CANDIDATE_COUNT = 8
        private const val FIRST_FIX_RETRY_DELAY_MS = 2_000L
        private const val MAX_RESOLVE_FAILURES = 6
        private const val MAX_RETRY_EXPONENT = 4
        private const val PAUSED_ANCHOR_RERESOLVE_MS = 10_000L
        private const val HINT_SAVE_INTERVAL_MS = 10_000L
        private const val GRAPH_FIT_MIN_INTERVAL_MS = 250L
        private const val GRAPH_FIT_LIVE_ANCHOR_MOVE_M = 0.02f
        private const val GRAPH_FIT_CHANGE_EPSILON_M = 0.01f
        private const val MIN_FIT_CONFIDENCE = 0.05f
        private const val CONFIDENCE_DISTANCE_DECAY_M = 8f
        private const val CONFIDENCE_TIME_DECAY_SECONDS = 60f
        private const val REROUTE_INTERVAL_MS = 3_000L
        private const val REROUTE_DEVIATION_M = 3f
        private const val WAYPOINT_ADVANCE_DISTANCE_M = 1.2f
        private const val ROUTE_PROJECTION_SMOOTHING_HZ = 6f
        private const val MIN_CAMERA_TO_FLOOR_M = 0.6f
        private const val MAX_CAMERA_TO_FLOOR_M = 2.2f
        private const val DISPLAY_POSE_CATCHUP_HZ = 4f
        private const val MAX_BLEND_DELTA_SECONDS = 0.25f
        private const val NANOS_PER_SECOND = 1_000_000_000f
    }
}
