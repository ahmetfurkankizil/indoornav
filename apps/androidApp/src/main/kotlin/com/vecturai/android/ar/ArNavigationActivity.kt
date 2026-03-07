package com.vecturai.android.ar

import android.os.Bundle
import android.widget.TextView
import android.widget.FrameLayout
import android.view.Gravity
import androidx.activity.ComponentActivity
import com.vecturai.core.domain.NavigationState
import kotlinx.coroutines.flow.StateFlow

/**
 * Native AR navigation screen using ARCore.
 *
 * This activity hosts the camera-based AR experience where:
 * - The entrance marker is detected to establish world alignment
 * - 3D navigation arrows are rendered on top of the camera feed
 * - The shared [NavigationState] drives what is displayed
 *
 * TODO: Integrate ARCore session lifecycle
 * TODO: Implement entrance marker detection (QR + image target)
 * TODO: Render 3D arrow models along route segments
 * TODO: Show textual guidance overlay
 * TODO: Handle AR session errors gracefully
 */
class ArNavigationActivity : ComponentActivity() {

    private val arBridge = ArBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Replace with actual ARCore GLSurfaceView / SceneView
        val placeholder = TextView(this).apply {
            text = "AR Navigation View\n\n" +
                "ARCore session will be initialized here.\n\n" +
                "• Entrance marker scanning\n" +
                "• 3D arrow rendering\n" +
                "• Real-time guidance overlay"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val container = FrameLayout(this).apply {
            addView(placeholder)
        }

        setContentView(container)

        // Observe navigation state from shared layer
        // TODO: Collect arBridge.navigationState and update AR scene accordingly
    }

    override fun onResume() {
        super.onResume()
        // TODO: Resume ARCore session
    }

    override fun onPause() {
        super.onPause()
        // TODO: Pause ARCore session
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: Clean up ARCore resources
    }
}
