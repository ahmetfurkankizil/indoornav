package com.example.vecturai.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vecturai.ar.ArSessionConfig
import com.example.vecturai.ar.ArrowPose
import com.example.vecturai.ar.ArrowRenderer
import com.example.vecturai.persistence.GraphRepository
import com.example.vecturai.ui.CameraPermissionGate
import com.example.vecturai.ui.hasValidGlbAsset
import com.google.ar.core.Pose
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader

@Composable
fun NavigationRoute(
    graphRepository: GraphRepository,
    onExit: () -> Unit
) {
    val viewModel: NavigationViewModel = viewModel(
        factory = NavigationViewModel.Factory(graphRepository)
    )
    NavigationScreen(
        viewModel = viewModel,
        onExit = onExit
    )
}

@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    CameraPermissionGate {
        Box(Modifier.fillMaxSize()) {
            NavigationArScene(
                nodePoses = state.nodePoses,
                arrowPose = state.arrowPose,
                onSessionCreated = viewModel::onSessionCreated,
                onSessionFailed = viewModel::onSessionFailed,
                onSessionUpdated = viewModel::onSessionUpdated
            )

            NavigationDiagnostics(
                modifier = Modifier.align(Alignment.TopCenter),
                state = state,
                onRelocalize = viewModel::relocalizeNow,
                onBack = onExit
            )

            when (state.phase) {
                NavigationPhase.NoGraphs -> EmptyGraphsPanel(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onBack = onExit
                )

                NavigationPhase.SelectBuilding -> BuildingPicker(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    buildings = state.availableBuildings,
                    onSelect = viewModel::selectBuilding,
                    onBack = onExit
                )

                NavigationPhase.PickingDestination -> DestinationPicker(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    state = state,
                    onSelect = viewModel::selectDestination
                )

                NavigationPhase.Arrived -> ArrivedPanel(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onPickAnother = viewModel::pickAnotherDestination,
                    onBack = onExit
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun NavigationArScene(
    nodePoses: List<SessionNodePose>,
    arrowPose: ArrowPose?,
    onSessionCreated: (com.google.ar.core.Session) -> Unit,
    onSessionFailed: (Throwable) -> Unit,
    onSessionUpdated: (com.google.ar.core.Session, com.google.ar.core.Frame) -> Unit
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    SideEffect {
        cameraStream.isDepthOcclusionEnabled = true
    }

    val hasArrowAsset = remember(context) { hasValidGlbAsset(context, "arrow.glb") }
    val arrowModelInstance = remember(modelLoader, hasArrowAsset) {
        if (hasArrowAsset) {
            runCatching { modelLoader.createModelInstance("arrow.glb") }.getOrNull()
        } else {
            null
        }
    }
    val arrowMaterial = remember(materialLoader, arrowModelInstance) {
        if (arrowModelInstance == null) {
            materialLoader.createColorInstance(Color(0xFFFF5722), roughness = 0.45f)
        } else {
            null
        }
    }

    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraStream = cameraStream,
        planeRenderer = false,
        sessionConfiguration = ArSessionConfig::configureIndoorCloudSession,
        onSessionCreated = onSessionCreated,
        onSessionUpdated = onSessionUpdated,
        onSessionFailed = onSessionFailed
    ) {
        nodePoses.forEach { nodePose ->
            key(nodePose.nodeId) {
                val confidence = nodePose.confidence.coerceIn(0f, 1f)
                val material = remember(materialLoader, confidence) {
                    materialLoader.createColorInstance(
                        confidenceColor(confidence),
                        roughness = 0.75f - 0.25f * confidence
                    )
                }
                PoseNode(pose = nodePose.pose) {
                    SphereNode(
                        radius = 0.07f + 0.03f * confidence,
                        center = Position(y = 0.08f),
                        materialInstance = material
                    )
                }
            }
        }

        key("nav-arrow") {
            arrowPose?.let { pose ->
                PoseNode(
                    pose = pose.toArPose(),
                    apply = {
                        isSmoothTransformEnabled = false
                    }
                ) {
                    if (arrowModelInstance != null) {
                        ModelNode(
                            modelInstance = arrowModelInstance,
                            autoAnimate = true,
                            scale = Scale(ARROW_MODEL_SCALE)
                        )
                    } else if (arrowMaterial != null) {
                        CubeNode(
                            size = Size(x = 0.08f, y = 0.05f, z = 0.46f),
                            center = Position(z = 0.18f),
                            materialInstance = arrowMaterial
                        )
                        SphereNode(
                            radius = 0.09f,
                            center = Position(z = 0.50f),
                            materialInstance = arrowMaterial
                        )
                    }
                }
            }
        }
    }
}

// arrow.glb is authored in meter-like units with roughly 0.92m length on +Z.
private const val ARROW_MODEL_SCALE = 0.45f

private fun confidenceColor(confidence: Float): Color {
    val low = Color(0xFFFFC107)
    val high = Color(0xFF00BCD4)
    return Color(
        red = low.red + (high.red - low.red) * confidence,
        green = low.green + (high.green - low.green) * confidence,
        blue = low.blue + (high.blue - low.blue) * confidence,
        alpha = 1f
    )
}

private fun ArrowPose.toArPose(): Pose {
    val yawDegrees = yawDegrees + ArrowRenderer.ARROW_MODEL_YAW_OFFSET_DEG
    val quaternion = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), yawDegrees)
    return Pose(
        floatArrayOf(position.x, position.y, position.z),
        floatArrayOf(quaternion.x, quaternion.y, quaternion.z, quaternion.w)
    )
}

@Composable
private fun NavigationDiagnostics(
    modifier: Modifier,
    state: NavigationUiState,
    onRelocalize: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.phase.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRelocalize) {
                        Text("Relocalize")
                    }
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            }
            Text("Tracking: ${state.trackingState}")
            Text("Resolved: ${state.resolvedAnchorCount} / ${state.graph?.nodes?.size ?: 0}")
            Text("Resolve attempts: ${state.resolveAttemptCount}")
            Text("Current node: ${state.currentNodeId?.take(8) ?: "-"}")
            Text("Waypoint: ${state.currentWaypoint?.label ?: state.currentWaypoint?.id?.take(8) ?: "-"}")
            Text("Next: ${state.distanceToNextMeters.formatMeters()}  Destination: ${state.distanceToDestinationMeters.formatMeters()}")
            Text(state.statusMessage, style = MaterialTheme.typography.bodySmall)
            state.lastResolveError?.let {
                Text(
                    text = "Last resolve: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyGraphsPanel(
    modifier: Modifier,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("No saved graph found.", style = MaterialTheme.typography.titleMedium)
            Text("Map a building and save it before starting navigation.")
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun BuildingPicker(
    modifier: Modifier,
    buildings: List<String>,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Select Building", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(buildings) { building ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(building) }
                    ) {
                        Text(building)
                    }
                }
            }
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ArrivedPanel(
    modifier: Modifier,
    onPickAnother: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onPickAnother
            ) {
                Text("Pick Room")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onBack
            ) {
                Text("Done")
            }
        }
    }
}

private fun Float?.formatMeters(): String =
    this?.let { "%.1f m".format(it) } ?: "-"
