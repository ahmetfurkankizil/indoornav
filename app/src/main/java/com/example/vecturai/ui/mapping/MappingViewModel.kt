package com.example.vecturai.ui.mapping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vecturai.ar.CloudAnchorHelper
import com.example.vecturai.ar.distanceMeters
import com.example.vecturai.ar.relativePose
import com.example.vecturai.ar.toMapNodePoseValues
import com.example.vecturai.ar.withLabel
import com.example.vecturai.graph.MapEdge
import com.example.vecturai.graph.MapGraph
import com.example.vecturai.graph.MapNode
import com.example.vecturai.persistence.GraphRepository
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

data class MappingMarker(
    val nodeId: String,
    val label: String?,
    val pose: Pose
)

data class MappingUiState(
    val buildingName: String = "demo",
    val trackingState: String = "Waiting for AR session",
    val isHosting: Boolean = false,
    val autoDropEnabled: Boolean = true,
    val markers: List<MappingMarker> = emptyList(),
    val edgeCount: Int = 0,
    val statusMessage: String = "Drop the first pin at the entrance.",
    val errorMessage: String? = null
) {
    val nodeCount: Int = markers.size
    val canTagLatest: Boolean = markers.isNotEmpty() && !isHosting
}

class MappingViewModel(
    private val graphRepository: GraphRepository
) : ViewModel() {
    private val nodes = mutableListOf<MapNode>()
    private val edges = mutableListOf<MapEdge>()
    private val markerPoses = mutableMapOf<String, Pose>()

    private var session: Session? = null
    private var latestCameraPose: Pose? = null
    private var latestTrackingState: TrackingState? = null
    private var graphOriginPose: Pose? = null
    private var lastAnchorPose: Pose? = null
    private var lastNodeId: String? = null

    private val _uiState = MutableStateFlow(MappingUiState())
    val uiState: StateFlow<MappingUiState> = _uiState.asStateFlow()

    fun onSessionCreated(session: Session) {
        this.session = session
    }

    fun onSessionFailed(error: Throwable) {
        _uiState.update {
            it.copy(
                trackingState = "AR session failed",
                isHosting = false,
                statusMessage = "AR session could not start.",
                errorMessage = error.message ?: error::class.java.simpleName
            )
        }
    }

    fun onSessionUpdated(session: Session, frame: Frame) {
        this.session = session
        latestCameraPose = frame.camera.pose
        val trackingState = frame.camera.trackingState
        latestTrackingState = trackingState
        _uiState.update { it.copy(trackingState = trackingState.name) }

        val lastPose = lastAnchorPose ?: return
        val cameraPose = latestCameraPose ?: return
        val state = _uiState.value
        val distance = distanceMeters(cameraPose, lastPose)
        if (
            state.autoDropEnabled &&
            trackingState == TrackingState.TRACKING &&
            !state.isHosting &&
            distance >= AUTO_DROP_DISTANCE_M
        ) {
            hostAnchorAt(cameraPose, source = "Auto")
        }
    }

    fun updateBuildingName(value: String) {
        _uiState.update { it.copy(buildingName = value) }
    }

    fun setAutoDropEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoDropEnabled = enabled) }
    }

    fun dropManualAnchor() {
        val pose = latestCameraPose
        if (pose == null) {
            _uiState.update { it.copy(errorMessage = "AR camera pose is not ready yet.") }
            return
        }
        if (latestTrackingState != TrackingState.TRACKING) {
            _uiState.update {
                it.copy(errorMessage = "Wait for AR tracking before dropping an anchor.")
            }
            return
        }
        hostAnchorAt(pose, source = "Manual")
    }

    fun tagLatest(label: String) {
        val nodeId = lastNodeId ?: return
        val index = nodes.indexOfFirst { it.id == nodeId }
        if (index == -1) return

        nodes[index] = nodes[index].withLabel(label)
        publishMarkers(
            statusMessage = "Tagged latest anchor as ${nodes[index].label ?: "waypoint"}."
        )
    }

    fun saveGraph(onSaved: () -> Unit) {
        val state = _uiState.value
        if (nodes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Drop at least one anchor before saving.") }
            return
        }

        val graph = MapGraph(
            buildingName = state.buildingName.ifBlank { "demo" },
            createdAtEpochMs = System.currentTimeMillis(),
            nodes = nodes.toList(),
            edges = edges.toList()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Saving graph...", errorMessage = null) }
            runCatching { graphRepository.save(graph) }
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Saved ${graph.nodes.size} anchors.") }
                    onSaved()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Save failed.") }
                }
        }
    }

    private fun hostAnchorAt(pose: Pose, source: String) {
        val activeSession = session
        if (activeSession == null) {
            _uiState.update { it.copy(errorMessage = "AR session is not ready.") }
            return
        }
        if (_uiState.value.isHosting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isHosting = true,
                    statusMessage = "$source drop: hosting Cloud Anchor...",
                    errorMessage = null
                )
            }

            val result = withTimeoutOrNull(HOST_TIMEOUT_MS) {
                CloudAnchorHelper.hostAnchor(activeSession, pose)
            } ?: Result.failure(
                IllegalStateException("$source Cloud Anchor host timed out.")
            )

            result
                .onSuccess { hosted ->
                    val hostedPose = hosted.anchor.pose
                    val origin = graphOriginPose ?: hostedPose.also { graphOriginPose = it }
                    val poseValues = relativePose(origin, hostedPose).toMapNodePoseValues()
                    val node = MapNode(
                        id = UUID.randomUUID().toString(),
                        cloudAnchorId = hosted.cloudAnchorId,
                        xMeters = poseValues.x,
                        yMeters = poseValues.y,
                        zMeters = poseValues.z,
                        qx = poseValues.qx,
                        qy = poseValues.qy,
                        qz = poseValues.qz,
                        qw = poseValues.qw
                    )

                    lastNodeId?.let { previousId ->
                        val previousPose = lastAnchorPose
                        if (previousPose != null) {
                            edges += MapEdge(
                                fromNodeId = previousId,
                                toNodeId = node.id,
                                distanceMeters = distanceMeters(previousPose, hostedPose)
                            )
                        }
                    }

                    nodes += node
                    markerPoses[node.id] = hostedPose
                    lastNodeId = node.id
                    lastAnchorPose = hostedPose
                    publishMarkers(
                        statusMessage = "$source anchor hosted. Nodes: ${nodes.size}."
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isHosting = false,
                            errorMessage = error.message ?: "Cloud Anchor hosting failed."
                        )
                    }
                }
        }
    }

    private fun publishMarkers(statusMessage: String) {
        val markers = nodes.mapNotNull { node ->
            markerPoses[node.id]?.let { pose ->
                MappingMarker(
                    nodeId = node.id,
                    label = node.label,
                    pose = pose
                )
            }
        }
        _uiState.update {
            it.copy(
                markers = markers,
                edgeCount = edges.size,
                isHosting = false,
                statusMessage = statusMessage,
                errorMessage = null
            )
        }
    }

    class Factory(
        private val graphRepository: GraphRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MappingViewModel(graphRepository) as T
        }
    }

    companion object {
        const val AUTO_DROP_DISTANCE_M = 4f
        private const val HOST_TIMEOUT_MS = 20_000L
    }
}
