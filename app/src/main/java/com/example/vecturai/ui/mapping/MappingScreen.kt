package com.example.vecturai.ui.mapping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vecturai.ar.ArSessionConfig
import com.example.vecturai.persistence.GraphRepository
import com.example.vecturai.ui.CameraPermissionGate
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader

@Composable
fun MappingRoute(
    graphRepository: GraphRepository,
    onExit: () -> Unit
) {
    val viewModel: MappingViewModel = viewModel(
        factory = MappingViewModel.Factory(graphRepository)
    )
    MappingScreen(
        viewModel = viewModel,
        onExit = onExit
    )
}

@Composable
fun MappingScreen(
    viewModel: MappingViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tagDialogOpen by remember { mutableStateOf(false) }

    CameraPermissionGate {
        Box(Modifier.fillMaxSize()) {
            MappingArScene(
                markers = state.markers,
                onSessionCreated = viewModel::onSessionCreated,
                onSessionFailed = viewModel::onSessionFailed,
                onSessionUpdated = viewModel::onSessionUpdated
            )

            MappingTopPanel(
                state = state,
                onBuildingNameChanged = viewModel::updateBuildingName,
                onAutoDropChanged = viewModel::setAutoDropEnabled
            )

            MappingBottomPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                state = state,
                onDropPin = viewModel::dropManualAnchor,
                onTagRoom = { tagDialogOpen = true },
                onSave = { viewModel.saveGraph(onExit) },
                onBack = onExit
            )

            state.errorMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 6.dp
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    if (tagDialogOpen) {
        TagRoomDialog(
            onDismiss = { tagDialogOpen = false },
            onConfirm = { label ->
                viewModel.tagLatest(label)
                tagDialogOpen = false
            }
        )
    }
}

@Composable
private fun MappingArScene(
    markers: List<MappingMarker>,
    onSessionCreated: (com.google.ar.core.Session) -> Unit,
    onSessionFailed: (Throwable) -> Unit,
    onSessionUpdated: (com.google.ar.core.Session, com.google.ar.core.Frame) -> Unit
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val waypointMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFF1DB954), roughness = 0.65f)
    }
    val roomMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(Color(0xFFE53935), roughness = 0.55f)
    }

    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        materialLoader = materialLoader,
        planeRenderer = true,
        sessionConfiguration = ArSessionConfig::configureIndoorCloudSession,
        onSessionCreated = onSessionCreated,
        onSessionUpdated = onSessionUpdated,
        onSessionFailed = onSessionFailed
    ) {
        markers.forEach { marker ->
            key(marker.nodeId) {
                val isRoom = !marker.label.isNullOrBlank()
                PoseNode(pose = marker.pose) {
                    SphereNode(
                        radius = if (isRoom) 0.16f else 0.11f,
                        center = Position(y = 0.05f),
                        materialInstance = if (isRoom) roomMaterial else waypointMaterial
                    )
                }
            }
        }
    }
}

@Composable
private fun MappingTopPanel(
    state: MappingUiState,
    onBuildingNameChanged: (String) -> Unit,
    onAutoDropChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.buildingName,
                onValueChange = onBuildingNameChanged,
                singleLine = true,
                label = { Text("Building graph") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tracking: ${state.trackingState}")
                Text("Nodes: ${state.nodeCount}  Edges: ${state.edgeCount}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.autoDropEnabled,
                    onCheckedChange = onAutoDropChanged
                )
                Text("Auto-drop every ${MappingViewModel.AUTO_DROP_DISTANCE_M.toInt()}m")
            }
            Text(
                text = "Walk slowly, point at textured surfaces, avoid blank walls, glass, and mirrors.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MappingBottomPanel(
    modifier: Modifier,
    state: MappingUiState,
    onDropPin: () -> Unit,
    onTagRoom: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !state.isHosting,
                    onClick = onDropPin
                ) {
                    Text(if (state.isHosting) "Hosting..." else "Drop Pin")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = state.canTagLatest,
                    onClick = onTagRoom
                ) {
                    Text("Tag Room")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                ) {
                    Text("Back")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = state.nodeCount > 0 && !state.isHosting,
                    onClick = onSave
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun TagRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag Latest Anchor") },
        text = {
            Column {
                Text("Name the room or landmark at the latest anchor.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text("Room name") }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = label.isNotBlank(),
                onClick = { onConfirm(label) }
            ) {
                Text("Save Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
