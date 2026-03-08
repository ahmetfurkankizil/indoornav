package com.vecturai.android.ar

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native AR navigation screen using ARCore.
 *
 * Full navigation experience:
 * - ARCore session + augmented image detection
 * - Active navigation overlay with progress and instructions
 * - Arrival detection → session completion
 * - Debug controls (simulate scan, advance, arrive)
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
    private lateinit var progressLabel: TextView
    private lateinit var modeLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var simulateButton: Button
    private lateinit var advanceButton: Button
    private lateinit var endButton: Button
    private lateinit var arrivalOverlay: LinearLayout

    private var isAligned = false
    private var isSimulated = false
    private var progress = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buildingId = intent.getStringExtra("buildingId") ?: "demo-office-01"
        val destinationName = intent.getStringExtra("destinationName") ?: "Conference Room"

        markerDetector.configure(
            markerId = "marker-main",
            nearestNodeId = "n01",
            buildingX = 0.0, buildingY = 1.2, buildingZ = 0.0,
            buildingRotationYDeg = 0.0,
        )
        markerDetector.onMarkerDetected = { event ->
            runOnUiThread { handleMarkerDetected(event) }
        }

        val container = FrameLayout(this)

        // AR camera placeholder
        val cameraView = FrameLayout(this).apply { setBackgroundColor(0xFF1A1A2E.toInt()) }
        container.addView(cameraView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
        ))

        // Main overlay
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
            text = "● Waiting for Marker"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(12, 6, 12, 6)
        }
        topBar.addView(stateLabel)

        modeLabel = TextView(this).apply {
            text = ""
            textSize = 10f
            setTextColor(0xFFFF9800.toInt())
            setPadding(8, 0, 0, 0)
            visibility = View.GONE
        }
        topBar.addView(modeLabel)

        topBar.addView(FrameLayout(this), LinearLayout.LayoutParams(0, 1, 1f))

        val closeButton = Button(this).apply {
            text = "✕"
            textSize = 16f
            setOnClickListener {
                sessionManager.stopSession()
                finish()
            }
        }
        topBar.addView(closeButton)
        overlay.addView(topBar)

        trackingLabel = TextView(this).apply {
            text = ""
            textSize = 11f
            setTextColor(0x99FFFFFF.toInt())
            setPadding(0, 4, 0, 0)
        }
        overlay.addView(trackingLabel)

        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            setPadding(0, 16, 0, 0)
        }
        overlay.addView(progressBar)

        // Spacer
        overlay.addView(FrameLayout(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
        ))

        // Arrival overlay (hidden until arrival)
        arrivalOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xDD000000.toInt())
        }
        val arrivalIcon = TextView(this).apply {
            text = "✓"
            textSize = 48f
            setTextColor(0xFF4CAF50.toInt())
            gravity = Gravity.CENTER
        }
        arrivalOverlay.addView(arrivalIcon)
        val arrivalTitle = TextView(this).apply {
            text = "You've arrived!"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        arrivalOverlay.addView(arrivalTitle)
        val arrivalDest = TextView(this).apply {
            text = "→ $destinationName"
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        arrivalOverlay.addView(arrivalDest)

        val doneButton = Button(this).apply {
            text = "Done"
            textSize = 16f
            setPadding(32, 16, 32, 16)
            setOnClickListener { finish() }
        }
        arrivalOverlay.addView(doneButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24; gravity = Gravity.CENTER })
        overlay.addView(arrivalOverlay)

        // Instruction
        instructionLabel = TextView(this).apply {
            text = "Point camera at the entrance marker"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(24, 14, 24, 14)
            setBackgroundColor(0x88000000.toInt())
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
            setPadding(12, 10, 12, 10)
            setBackgroundColor(0xAA000000.toInt())
            visibility = View.GONE
        }
        markerStatusLabel = TextView(this).apply { text = "Marker: ✗ Waiting"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        arrowCountLabel = TextView(this).apply { text = "Arrows: 0"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        progressLabel = TextView(this).apply { text = "Progress: 0%"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        debugPanel.addView(markerStatusLabel)
        debugPanel.addView(arrowCountLabel)
        debugPanel.addView(progressLabel)
        overlay.addView(debugPanel)

        // Action buttons
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        Button(this).apply {
            text = "🐛"
            setOnClickListener {
                debugPanel.visibility = if (debugPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }.also { buttonRow.addView(it) }

        simulateButton = Button(this).apply {
            text = "⚡ Simulate"
            setOnClickListener { simulateAlignment() }
        }
        buttonRow.addView(simulateButton)

        advanceButton = Button(this).apply {
            text = "▶ Advance"
            visibility = View.GONE
            setOnClickListener { advanceProgress() }
        }
        buttonRow.addView(advanceButton)

        endButton = Button(this).apply {
            text = "■ End"
            visibility = View.GONE
            setOnClickListener {
                sessionManager.stopSession()
                finish()
            }
        }
        buttonRow.addView(endButton)

        overlay.addView(buttonRow)
        container.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
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
        isSimulated = true
        modeLabel.text = "DEMO MODE"
        modeLabel.visibility = View.VISIBLE
        val event = MarkerDetectionEvent(
            markerId = "marker-main", entranceNodeId = "n01",
            markerBuildingX = 0.0, markerBuildingY = 1.2, markerBuildingZ = 0.0,
            markerArX = 0.0, markerArY = 0.0, markerArZ = -1.0,
            markerArRotationYDeg = 0.0, markerBuildingRotationYDeg = 0.0,
            confidence = 1.0,
        )
        handleMarkerDetected(event)
    }

    private fun advanceProgress() {
        progress = (progress + 0.15).coerceAtMost(1.0)
        progressBar.progress = (progress * 100).toInt()
        progressLabel.text = "Progress: ${(progress * 100).toInt()}%"

        if (progress >= 0.95) {
            arriveAtDestination()
        } else if (progress >= 0.8) {
            instructionLabel.text = "Approaching destination..."
            stateLabel.text = "● Approaching"
            stateLabel.setTextColor(0xFF4CAF50.toInt())
        }
    }

    private fun arriveAtDestination() {
        arrivalOverlay.visibility = View.VISIBLE
        instructionLabel.visibility = View.GONE
        advanceButton.visibility = View.GONE
        endButton.visibility = View.GONE
        stateLabel.text = "● Arrived"
        stateLabel.setTextColor(0xFF4CAF50.toInt())
    }

    private fun handleMarkerDetected(event: MarkerDetectionEvent) {
        isAligned = true
        progress = 0.0
        stateLabel.text = "● Navigating"
        stateLabel.setTextColor(0xFF2196F3.toInt())
        markerStatusLabel.text = "Marker: ✓ Detected"
        instructionLabel.text = "Follow the arrows"
        simulateButton.visibility = View.GONE
        advanceButton.visibility = View.VISIBLE
        endButton.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

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
    }
}
