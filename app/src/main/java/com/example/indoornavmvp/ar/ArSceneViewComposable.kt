package com.example.indoornavmvp.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneView as SceneViewClass
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.Node

/**
 * Compose wrapper for ArSceneView.
 * Provides a declarative way to use AR in Jetpack Compose.
 *
 * @param modifier Modifier for the AR view
 * @param onSessionCreated Called when the AR session is created
 * @param onSessionResumed Called when the AR session resumes
 * @param onFrame Called each frame with the current AR frame (use for pose tracking)
 * @param onTrackingStateChanged Called when tracking state changes
 * @param planeRendererEnabled Whether to show detected planes
 * @param onViewCreated Called with the SceneView instance for advanced configuration
 */
@Composable
fun ARSceneView(
    modifier: Modifier = Modifier,
    onSessionCreated: ((Session) -> Unit)? = null,
    onSessionResumed: ((Session) -> Unit)? = null,
    onFrame: ((Frame) -> Unit)? = null,
    onTrackingStateChanged: ((TrackingState) -> Unit)? = null,
    planeRendererEnabled: Boolean = true,
    onViewCreated: ((SceneViewClass) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Remember the SceneView to avoid recreation
    val arSceneView = remember {
        SceneViewClass(context).apply {
            // Configure the AR session
            configureSession { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.focusMode = Config.FocusMode.AUTO

                // Notify caller when the session is created/configured
                onSessionCreated?.invoke(session)
            }

            // Per-frame callback from SceneView/ARCore
            onSessionUpdated = { session, frame ->
                // Forward frame to caller
                onFrame?.invoke(frame)

                // Forward tracking state changes
                val trackingState = frame.camera.trackingState
                onTrackingStateChanged?.invoke(trackingState)

                // Optional: notify that the session is effectively running
                onSessionResumed?.invoke(session)
            }

            // Configure plane renderer
            planeRenderer.isEnabled = planeRendererEnabled
            planeRenderer.isVisible = planeRendererEnabled
        }
    }

    // Render the ArSceneView
    AndroidView(
        factory = {
            arSceneView.also { view ->
                onViewCreated?.invoke(view)
            }
        },
        modifier = modifier,
        update = { view ->
            // Update plane renderer visibility
            view.planeRenderer.isEnabled = planeRendererEnabled
            view.planeRenderer.isVisible = planeRendererEnabled
        }
    )
}

/**
 * Extension function to add a node to the SceneView's scene.
 */
fun SceneViewClass.addNode(node: Node) {
    addChildNode(node)
}

/**
 * Extension function to remove a node from the SceneView's scene.
 */
fun SceneViewClass.removeNode(node: Node) {
    removeChildNode(node)
}

/**
 * Extension function to clear all nodes from the SceneView's scene.
 */
fun SceneViewClass.clearNodes() {
    childNodes.toList().forEach { node ->
        removeChildNode(node)
        if (node is AnchorNode) {
            node.anchor?.detach()
        }
        node.destroy()
    }
}

/**
 * Extension function to get the current camera pose from a frame.
 * Returns null if not tracking.
 */
fun Frame.getCameraPoseIfTracking() = if (camera.trackingState == TrackingState.TRACKING) {
    camera.displayOrientedPose
} else {
    null
}

