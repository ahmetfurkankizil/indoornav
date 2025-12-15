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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.indoornavmvp.data.LocalNode
import com.example.indoornavmvp.data.MapRepository
import com.example.indoornavmvp.ui.theme.IndoorNavMvpTheme
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView as SceneViewARSceneView
import kotlinx.coroutines.launch

/**
 * Admin activity for placing navigation nodes in AR using SceneView.
 */
class AdminActivity : ComponentActivity() {

    private val placedNodes = mutableListOf<LocalNode>()
    private var nextNodeId = 0

    // Compose state
    private val nodeCountState = mutableIntStateOf(0)
    private val isTrackingState = mutableStateOf(false)

    // AR references
    private var arSceneView: SceneViewARSceneView? = null
    private var arSession: Session? = null
    private var latestPose: Pose? = null

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

        setContent {
            IndoorNavMvpTheme {
                val nodeCount by nodeCountState
                val isTracking by isTrackingState

                AdminScreen(
                    nodeCount = nodeCount,
                    isTracking = isTracking,
                    onViewCreated = { view ->
                        arSceneView = view
                    },
                    onSessionCreated = { session ->
                        arSession = session
                    },
                    onFrame = { frame ->
                        latestPose = frame.getCameraPoseIfTracking()
                    },
                    onTrackingStateChanged = { state ->
                        isTrackingState.value = state == TrackingState.TRACKING
                    },
                    onPlaceNode = { placeNode() },
                    onSaveMap = { saveMap() }
                )
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun placeNode() {
        val pose = latestPose
        val session = arSession
        val sceneView = arSceneView

        if (pose == null || session == null || sceneView == null) {
            Toast.makeText(this, "No AR tracking yet - move device around", Toast.LENGTH_SHORT).show()
            return
        }

        // Create anchor and node
        kotlinx.coroutines.MainScope().launch {
            try {
                val anchorNode = ArNodeFactory.createSphereAnchorNode(
                    arSceneView = sceneView,
                    session = session,
                    pose = pose,
                    color = ArNodeFactory.Colors.GREEN,
                    radius = 0.05f
                )

                if (anchorNode != null) {
                    // Add to scene
                    sceneView.addChildNode(anchorNode)

                    // Store the pose data
                    val matrix = ArNodeFactory.poseToMatrix(pose)
                    val label = "NODE_$nextNodeId"
                    val node = LocalNode(
                        id = nextNodeId,
                        label = label,
                        poseMatrix = matrix
                    )

                    placedNodes.add(node)
                    nextNodeId++
                    nodeCountState.intValue = placedNodes.size

                    Toast.makeText(this@AdminActivity, "Placed $label", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AdminActivity, "Failed to create anchor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun saveMap() {
        if (placedNodes.isEmpty()) {
            Toast.makeText(this, "No nodes to save", Toast.LENGTH_SHORT).show()
            return
        }

        val success = MapRepository.saveNodes(this, placedNodes)
        if (success) {
            Toast.makeText(
                this,
                "Saved ${placedNodes.size} nodes to map.json",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Failed to save map", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun AdminScreen(
    nodeCount: Int,
    isTracking: Boolean,
    onViewCreated: (SceneViewARSceneView) -> Unit,
    onSessionCreated: (Session) -> Unit,
    onFrame: (Frame) -> Unit,
    onTrackingStateChanged: (TrackingState) -> Unit,
    onPlaceNode: () -> Unit,
    onSaveMap: () -> Unit
) {
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

        // Top info bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Column {
                Text(
                    text = "Admin Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Nodes placed: $nodeCount",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (isTracking) "✓ Tracking" else "⏳ Initializing...",
                    fontSize = 12.sp,
                    color = if (isTracking) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }

        // Bottom button bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onPlaceNode,
                enabled = isTracking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Place Node",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onSaveMap,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Save Map",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
