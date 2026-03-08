package com.vecturai.android.ar

import android.os.Bundle
import android.opengl.GLSurfaceView
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native AR navigation screen using ARCore.
 *
 * Hosts the camera-based AR experience with:
 * - ARCore session for camera + world tracking
 * - Augmented image detection for entrance marker
 * - Navigation arrow rendering (placeholder geometry for v1)
 * - Debug overlay showing session state, marker status, arrow count
 */
class ArNavigationActivity : ComponentActivity() {

    private val sessionManager = ArSessionManager()
    private val markerDetector = ArMarkerDetector()
    private val routeRenderer = ArRouteRenderer()

    private lateinit var stateLabel: TextView
    private lateinit var trackingLabel: TextView
    private lateinit var instructionLabel: TextView
    private lateinit var debugPanel: LinearLayout
    private lateinit var arrowCountLabel: TextView
    private lateinit var markerStatusLabel: TextView
    private lateinit var simulateButton: Button

    private var isAligned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Receive destination info via intent
        val buildingId = intent.getStringExtra("buildingId") ?: "demo-office-01"
        val destinationName = intent.getStringExtra("destinationName") ?: ""

        // Configure marker detector
        markerDetector.configure(
            markerId = "marker-main",
            nearestNodeId = "n01",
            buildingX = 0.0,
            buildingY = 1.2,
            buildingZ = 0.0,
            buildingRotationYDeg = 0.0,
        )

        markerDetector.onMarkerDetected = { event ->
            runOnUiThread { handleMarkerDetected(event) }
        }

        // Build UI
        val container = FrameLayout(this)

        // AR camera view placeholder
        // TODO: Replace with GLSurfaceView + ARCore renderer
        val cameraView = FrameLayout(this).apply {
            setBackgroundColor(0xFF1A1A2E.toInt())
        }
        container.addView(cameraView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))

        // Overlay UI
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 48)
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        stateLabel = TextView(this).apply {
            text = "Waiting for Marker"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCCFF9800.toInt())
            setPadding(16, 8, 16, 8)
        }
        topBar.addView(stateLabel)

        val spacer = FrameLayout(this)
        topBar.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        val closeButton = Button(this).apply {
            text = "✕"
            textSize = 18f
            setOnClickListener { finish() }
        }
        topBar.addView(closeButton)
        overlay.addView(topBar)

        trackingLabel = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(0xAAFFFFFF.toInt())
            setPadding(0, 8, 0, 0)
        }
        overlay.addView(trackingLabel)

        // Spacer
        val midSpacer = FrameLayout(this)
        overlay.addView(midSpacer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Bottom section
        instructionLabel = TextView(this).apply {
            text = "Point camera at the entrance marker"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 16, 24, 16)
            setBackgroundColor(0x99000000.toInt())
        }
        overlay.addView(instructionLabel)

        // Destination label
        val destLabel = TextView(this).apply {
            text = if (destinationName.isNotEmpty()) "→ $destinationName" else ""
            textSize = 14f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }
        overlay.addView(destLabel)

        // Debug panel
        debugPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0x99000000.toInt())
            visibility = LinearLayout.GONE
        }

        val debugTitle = TextView(this).apply {
            text = "Debug Info"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
        }
        debugPanel.addView(debugTitle)

        markerStatusLabel = TextView(this).apply {
            text = "Marker: ✗ Waiting"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
        }
        debugPanel.addView(markerStatusLabel)

        arrowCountLabel = TextView(this).apply {
            text = "Arrows: 0"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
        }
        debugPanel.addView(arrowCountLabel)

        overlay.addView(debugPanel)

        // Action buttons
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val debugButton = Button(this).apply {
            text = "🐛 Debug"
            setOnClickListener {
                debugPanel.visibility = if (debugPanel.visibility == LinearLayout.VISIBLE)
                    LinearLayout.GONE else LinearLayout.VISIBLE
            }
        }
        buttonRow.addView(debugButton)

        simulateButton = Button(this).apply {
            text = "⚡ Simulate Scan"
            setOnClickListener { simulateAlignment() }
        }
        buttonRow.addView(simulateButton)

        overlay.addView(buttonRow)
        container.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))

        setContentView(container)
    }

    override fun onResume() {
        super.onResume()
        sessionManager.createSession(this)
        sessionManager.resumeSession()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pauseSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.stopSession()
    }

    private fun simulateAlignment() {
        val event = MarkerDetectionEvent(
            markerId = "marker-main",
            entranceNodeId = "n01",
            markerBuildingX = 0.0,
            markerBuildingY = 1.2,
            markerBuildingZ = 0.0,
            markerArX = 0.0,
            markerArY = 0.0,
            markerArZ = -1.0,
            markerArRotationYDeg = 0.0,
            markerBuildingRotationYDeg = 0.0,
            confidence = 1.0,
        )
        handleMarkerDetected(event)
    }

    private fun handleMarkerDetected(event: MarkerDetectionEvent) {
        isAligned = true
        stateLabel.text = "Aligned"
        stateLabel.setBackgroundColor(0xCC4CAF50.toInt())
        markerStatusLabel.text = "Marker: ✓ Detected"
        instructionLabel.text = "Route loaded — follow the arrows"
        simulateButton.isEnabled = false

        // Compute alignment transform
        val rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        val cosR = cos(Math.toRadians(rotDeg))
        val sinR = sin(Math.toRadians(rotDeg))
        val rotBldgX = event.markerBuildingX * cosR + event.markerBuildingZ * sinR
        val rotBldgZ = -event.markerBuildingX * sinR + event.markerBuildingZ * cosR

        routeRenderer.setAlignmentTransform(
            offsetX = event.markerArX - rotBldgX,
            offsetY = event.markerArY - event.markerBuildingY,
            offsetZ = event.markerArZ - rotBldgZ,
            rotationYDeg = rotDeg,
        )

        // Load demo arrows (same route as iOS)
        val demoArrows = listOf(
            ArrowRenderData("a0", 0.0, 0.05, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a1", 1.5, 0.05, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a2", 3.0, 0.05, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a3", 4.5, 0.05, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a4", 6.0, 0.05, 0.0, 0.0, 0.0, 1.0, ArrowRenderType.TURN_RIGHT, "Turn right"),
            ArrowRenderData("a5", 6.0, 0.05, 1.5, 0.0, 0.0, 1.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a6", 6.0, 0.05, 3.0, 0.0, 0.0, 1.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a7", 6.0, 0.05, 4.0, -1.0, 0.0, 0.0, ArrowRenderType.TURN_LEFT, "Turn left"),
            ArrowRenderData("a8", 4.5, 0.05, 4.0, -1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a9", 3.0, 0.05, 4.0, -1.0, 0.0, 0.0, ArrowRenderType.DESTINATION, "Conference Room"),
        )
        routeRenderer.updateArrows(demoArrows)
        arrowCountLabel.text = "Arrows: ${routeRenderer.renderedArrowCount}"
        stateLabel.text = "Rendering Route"
        stateLabel.setBackgroundColor(0xCC2196F3.toInt())
    }
}
