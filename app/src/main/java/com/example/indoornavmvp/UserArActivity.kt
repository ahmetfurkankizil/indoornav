package com.example.indoornavmvp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.indoornavmvp.ar.ARSceneView
import com.example.indoornavmvp.ar.ArNodeFactory
import com.example.indoornavmvp.ar.getCameraPoseIfTracking
import com.example.indoornavmvp.data.MapRepository
import com.example.indoornavmvp.data.NavGraph
import com.example.indoornavmvp.data.NodeDto
import com.example.indoornavmvp.data.NodeId
import com.example.indoornavmvp.data.PathFinder
import com.example.indoornavmvp.data.Room
import com.example.indoornavmvp.ui.theme.IndoorNavMvpTheme
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView as SceneViewARSceneView
import io.github.sceneview.ar.node.AnchorNode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * User activity for AR navigation with path guidance using SceneView.
 */
class UserArActivity : ComponentActivity() {

    // Map data
    private var nodeDtos: List<NodeDto> = emptyList()
    private var navGraph: NavGraph? = null

    // Navigation state
    private var currentPath: List<NodeId> = emptyList()
    private val currentStepIndexState = mutableIntStateOf(0)
    private val isNavigatingState = mutableStateOf(false)
    private val isTrackingState = mutableStateOf(false)

    // AR references
    private var arSceneView: SceneViewARSceneView? = null
    private var arSession: Session? = null
    private var latestPose: Pose? = null

    // Node references for updating
    private val nodeAnchors = mutableMapOf<NodeId, AnchorNode>()
    private var arrowAnchorNode: AnchorNode? = null
    private var nodesCreated = false

    // Distance threshold for advancing to next node (in meters)
    private val proximityThreshold = 0.7f

    // Permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check camera permission
        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Load map data
        loadMapData()

        setContent {
            IndoorNavMvpTheme {
                val currentStepIndex by currentStepIndexState
                val isNavigating by isNavigatingState
                val isTracking by isTrackingState

                UserArScreen(
                    rooms = navGraph?.rooms ?: emptyList(),
                    isNavigating = isNavigating,
                    isTracking = isTracking,
                    currentStep = currentStepIndex,
                    totalSteps = currentPath.size,
                    onViewCreated = { view ->
                        arSceneView = view
                    },
                    onSessionCreated = { session ->
                        arSession = session
                    },
                    onFrame = { frame ->
                        latestPose = frame.getCameraPoseIfTracking()
                        
                        // Create nodes once we have tracking
                        if (!nodesCreated && latestPose != null && arSession != null) {
                            createMapNodes()
                        }
                        
                        // Check proximity during navigation
                        if (isNavigatingState.value && latestPose != null) {
                            checkProximityAndAdvance()
                        }
                    },
                    onTrackingStateChanged = { state ->
                        isTrackingState.value = state == TrackingState.TRACKING
                    },
                    onStartNavigation = { room -> startNavigation(room) },
                    onStopNavigation = { stopNavigation() }
                )
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun loadMapData() {
        nodeDtos = MapRepository.loadNodeDtos(this)
        navGraph = MapRepository.buildNavGraphFromDtos(nodeDtos)

        if (nodeDtos.isEmpty()) {
            Toast.makeText(this, "No map data found. Please run Admin mode first.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createMapNodes() {
        val session = arSession ?: return
        val sceneView = arSceneView ?: return
        
        if (nodeDtos.isEmpty() || nodesCreated) return

        MainScope().launch {
            try {
                nodeDtos.forEach { dto ->
                    // Extract translation from 4x4 matrix (indices 12, 13, 14)
                    val matrix = dto.pose.toFloatArray()
                    val pose = Pose.makeTranslation(matrix[12], matrix[13], matrix[14])
                    val nodeId = NodeId("NODE_${dto.id}")

                    val anchorNode = ArNodeFactory.createSphereAnchorNode(
                        arSceneView = sceneView,
                        session = session,
                        pose = pose,
                        color = ArNodeFactory.Colors.CYAN,
                        radius = 0.04f
                    )

                    anchorNode?.let {
                        sceneView.addChildNode(it)
                        nodeAnchors[nodeId] = it
                    }
                }
                nodesCreated = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startNavigation(room: Room) {
        val graph = navGraph ?: return

        if (graph.nodes.isEmpty()) {
            Toast.makeText(this, "No nodes in navigation graph", Toast.LENGTH_SHORT).show()
            return
        }

        val startNode = graph.nodes.first().id
        val goalNode = room.doorNode

        currentPath = PathFinder.shortestPath(graph, startNode, goalNode)

        if (currentPath.isEmpty()) {
            Toast.makeText(this, "No path found to ${room.name}", Toast.LENGTH_SHORT).show()
            return
        }

        currentStepIndexState.intValue = 0
        isNavigatingState.value = true

        // Create arrow at first target
        updateArrowPosition()

        Toast.makeText(
            this,
            "Navigating to ${room.name} (${currentPath.size} nodes)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopNavigation() {
        isNavigatingState.value = false
        currentPath = emptyList()
        currentStepIndexState.intValue = 0

        // Remove arrow
        arrowAnchorNode?.let { arrow ->
            arSceneView?.removeChildNode(arrow)
            arrow.anchor?.detach()
            arrow.destroy()
        }
        arrowAnchorNode = null
    }

    private fun updateArrowPosition() {
        val sceneView = arSceneView ?: return
        val session = arSession ?: return

        if (currentPath.isEmpty()) return

        val currentIndex = currentStepIndexState.intValue.coerceIn(0, currentPath.size - 1)
        val targetNodeId = currentPath[currentIndex]

        // Find the pose for this node
        val targetDto = nodeDtos.find { NodeId("NODE_${it.id}") == targetNodeId }
        val targetPose = targetDto?.let { 
            val matrix = it.pose.toFloatArray()
            Pose.makeTranslation(matrix[12], matrix[13], matrix[14])
        } ?: return

        MainScope().launch {
            try {
                // Remove old arrow
                arrowAnchorNode?.let { arrow ->
                    sceneView.removeChildNode(arrow)
                    arrow.anchor?.detach()
                    arrow.destroy()
                }

                // Create new arrow at target position
                arrowAnchorNode = ArNodeFactory.createArrowAnchorNode(
                    arSceneView = sceneView,
                    session = session,
                    pose = targetPose,
                    color = ArNodeFactory.Colors.ORANGE,
                    scale = 1.5f
                )

                arrowAnchorNode?.let {
                    sceneView.addChildNode(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkProximityAndAdvance() {
        val pose = latestPose ?: return
        if (currentPath.isEmpty()) return

        val currentIndex = currentStepIndexState.intValue
        if (currentIndex >= currentPath.size) return

        val targetNodeId = currentPath[currentIndex]
        val targetDto = nodeDtos.find { NodeId("NODE_${it.id}") == targetNodeId } ?: return
        
        val matrix = targetDto.pose.toFloatArray()
        val targetPose = Pose.makeTranslation(matrix[12], matrix[13], matrix[14])

        // Calculate distance
        val dx = pose.tx() - targetPose.tx()
        val dy = pose.ty() - targetPose.ty()
        val dz = pose.tz() - targetPose.tz()
        val distance = sqrt(dx * dx + dy * dy + dz * dz)

        if (distance < proximityThreshold) {
            if (currentIndex < currentPath.size - 1) {
                // Advance to next node
                currentStepIndexState.intValue = currentIndex + 1
                updateArrowPosition()
            } else {
                // Reached destination
                runOnUiThread {
                    Toast.makeText(this, "🎉 Arrived at destination!", Toast.LENGTH_LONG).show()
                    stopNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserArScreen(
    rooms: List<Room>,
    isNavigating: Boolean,
    isTracking: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onViewCreated: (SceneViewARSceneView) -> Unit,
    onSessionCreated: (Session) -> Unit,
    onFrame: (Frame) -> Unit,
    onTrackingStateChanged: (TrackingState) -> Unit,
    onStartNavigation: (Room) -> Unit,
    onStopNavigation: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf(rooms.firstOrNull()) }

    // Update selected room when rooms list changes
    if (selectedRoom == null && rooms.isNotEmpty()) {
        selectedRoom = rooms.first()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // AR Scene View
        ARSceneView(
            modifier = Modifier.fillMaxSize(),
            onViewCreated = onViewCreated,
            onSessionCreated = onSessionCreated,
            onFrame = onFrame,
            onTrackingStateChanged = onTrackingStateChanged,
            planeRendererEnabled = true
        )

        // Top control bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isTracking) "✓ Tracking" else "⏳ Initializing...",
                    fontSize = 12.sp,
                    color = if (isTracking) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isNavigating) {
                // Navigation status
                Text(
                    text = "Navigating: Step ${currentStep + 1} of $totalSteps",
                    fontSize = 14.sp,
                    color = Color(0xFF00D9FF)
                )
                Text(
                    text = "Follow the orange arrow",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onStopNavigation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop Navigation")
                }
            } else {
                // Room selection
                if (rooms.isEmpty()) {
                    Text(
                        text = "No rooms available. Run Admin mode first.",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Room dropdown
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            TextField(
                                value = selectedRoom?.name ?: "Select room",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                rooms.forEach { room ->
                                    DropdownMenuItem(
                                        text = { Text(room.name) },
                                        onClick = {
                                            selectedRoom = room
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Start button
                        Button(
                            onClick = { selectedRoom?.let { onStartNavigation(it) } },
                            enabled = selectedRoom != null && isTracking,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Start")
                        }
                    }
                }
            }
        }

        // Bottom info bar
        if (isNavigating) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Walk towards the orange arrow",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "It will advance when you get close (< 0.7m)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
