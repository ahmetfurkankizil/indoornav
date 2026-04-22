package com.vecturai.android.ar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AR navigation activity with live progress, recenter, and arrival flow.
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
    private lateinit var remainingLabel: TextView
    private lateinit var confidenceLabel: TextView
    private lateinit var modeLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var simulateButton: Button
    private lateinit var advanceButton: Button
    private lateinit var rescanButton: Button
    private lateinit var endButton: Button
    private lateinit var arrivalOverlay: LinearLayout

    private var isAligned = false
    private var isSimulated = false
    private var progress = 0.0
    private var remainingDistance = 0.0
    private var isLowConfidence = false

    // Alignment state
    private var alignOffX = 0.0; private var alignOffY = 0.0; private var alignOffZ = 0.0
    private var alignRotYDeg = 0.0

    private val handler = Handler(Looper.getMainLooper())
    private var poseRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destinationName = intent.getStringExtra("destinationName") ?: "Conference Room"

        markerDetector.configure(
            markerId = "marker-main", nearestNodeId = "n01",
            buildingX = 0.0, buildingY = 1.2, buildingZ = 0.0, buildingRotationYDeg = 0.0,
        )
        markerDetector.onMarkerDetected = { event ->
            runOnUiThread { handleMarkerDetected(event) }
        }

        val container = FrameLayout(this)
        val cameraView = FrameLayout(this).apply { setBackgroundColor(0xFF1A1A2E.toInt()) }
        container.addView(cameraView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
        ))

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
            text = "● Waiting for Marker"; textSize = 13f; setTextColor(0xFFFFFFFF.toInt())
        }
        topBar.addView(stateLabel)
        modeLabel = TextView(this).apply {
            text = "DEMO MODE"; textSize = 10f; setTextColor(0xFFFF9800.toInt()); visibility = View.GONE
        }
        topBar.addView(modeLabel)
        topBar.addView(FrameLayout(this), LinearLayout.LayoutParams(0, 1, 1f))
        Button(this).apply {
            text = "✕"; setOnClickListener { endAndFinish() }
        }.also { topBar.addView(it) }
        overlay.addView(topBar)

        trackingLabel = TextView(this).apply {
            text = ""; textSize = 11f; setTextColor(0x99FFFFFF.toInt())
        }
        overlay.addView(trackingLabel)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0; visibility = View.GONE
        }
        overlay.addView(progressBar)

        // Remaining distance
        remainingLabel = TextView(this).apply {
            text = ""; textSize = 12f; setTextColor(0xCCFFFFFF.toInt()); gravity = Gravity.END; visibility = View.GONE
        }
        overlay.addView(remainingLabel)

        overlay.addView(FrameLayout(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
        ))

        // Arrival overlay
        arrivalOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            visibility = View.GONE; setPadding(32, 32, 32, 32); setBackgroundColor(0xDD000000.toInt())
        }
        val arrivalIcon = TextView(this).apply { text = "✓"; textSize = 48f; setTextColor(0xFF4CAF50.toInt()); gravity = Gravity.CENTER }
        arrivalOverlay.addView(arrivalIcon)
        val arrivalTitle = TextView(this).apply { text = "You've arrived!"; textSize = 24f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER }
        arrivalOverlay.addView(arrivalTitle)
        val arrivalDest = TextView(this).apply { text = "→ $destinationName"; textSize = 16f; setTextColor(0xCCFFFFFF.toInt()); gravity = Gravity.CENTER }
        arrivalOverlay.addView(arrivalDest)
        Button(this).apply {
            text = "Done"; setOnClickListener { finish() }
        }.also { arrivalOverlay.addView(it, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24; gravity = Gravity.CENTER }) }
        overlay.addView(arrivalOverlay)

        // Instruction
        instructionLabel = TextView(this).apply {
            text = "Point camera at the entrance marker"; textSize = 18f
            setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
            setPadding(24, 14, 24, 14); setBackgroundColor(0x88000000.toInt())
        }
        overlay.addView(instructionLabel)

        // Debug panel
        debugPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(12, 10, 12, 10)
            setBackgroundColor(0xAA000000.toInt()); visibility = View.GONE
        }
        markerStatusLabel = TextView(this).apply { text = "Marker: ✗"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        arrowCountLabel = TextView(this).apply { text = "Arrows: 0"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        progressLabel = TextView(this).apply { text = "Progress: 0%"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        confidenceLabel = TextView(this).apply { text = "Confidence: OK"; textSize = 11f; setTextColor(0xFFFFFFFF.toInt()) }
        debugPanel.addView(markerStatusLabel)
        debugPanel.addView(arrowCountLabel)
        debugPanel.addView(progressLabel)
        debugPanel.addView(confidenceLabel)
        overlay.addView(debugPanel)

        // Buttons
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 16, 0, 0)
        }
        Button(this).apply {
            text = "🐛"; setOnClickListener {
                debugPanel.visibility = if (debugPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }.also { buttonRow.addView(it) }

        simulateButton = Button(this).apply {
            text = "⚡ Simulate"; setOnClickListener { simulateAlignment() }
        }
        buttonRow.addView(simulateButton)

        rescanButton = Button(this).apply {
            text = "↻ Rescan"; visibility = View.GONE
            setOnClickListener { rescanMarker() }
        }
        buttonRow.addView(rescanButton)

        advanceButton = Button(this).apply {
            text = "▶ Advance"; visibility = View.GONE
            setOnClickListener { advanceProgress() }
        }
        buttonRow.addView(advanceButton)

        endButton = Button(this).apply {
            text = "■ End"; visibility = View.GONE
            setOnClickListener { endAndFinish() }
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
        stopPoseUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.stopSession()
        stopPoseUpdates()
    }

    private fun endAndFinish() {
        stopPoseUpdates()
        sessionManager.stopSession()
        finish()
    }

    private fun simulateAlignment() {
        isSimulated = true
        modeLabel.visibility = View.VISIBLE
        handleMarkerDetected(MarkerDetectionEvent(
            markerId = "marker-main", entranceNodeId = "n01",
            markerBuildingX = 0.0, markerBuildingY = 1.2, markerBuildingZ = 0.0,
            markerArX = 0.0, markerArY = 0.0, markerArZ = -1.0,
            markerArRotationYDeg = 0.0, markerBuildingRotationYDeg = 0.0, confidence = 1.0,
            role = ArMarkerDetector.MarkerDetectionRole.ENTRANCE,
        ))
    }

    private fun rescanMarker() {
        stateLabel.text = "● Rescanning..."
        stateLabel.setTextColor(0xFFFF9800.toInt())
        instructionLabel.text = "Point camera at marker to recenter"
    }

    private fun advanceProgress() {
        progress = min(progress + 0.15, 1.0)
        remainingDistance = max(0.0, remainingDistance - 2.0)
        updateUI()
        checkArrival()
    }

    private fun handleMarkerDetected(event: MarkerDetectionEvent) {
        isAligned = true
        stateLabel.text = "● Navigating"; stateLabel.setTextColor(0xFF2196F3.toInt())
        markerStatusLabel.text = "Marker: ✓"; instructionLabel.text = "Follow the arrows"
        simulateButton.visibility = View.GONE
        rescanButton.visibility = View.VISIBLE
        endButton.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        remainingLabel.visibility = View.VISIBLE

        if (isSimulated) advanceButton.visibility = View.VISIBLE

        val rotDeg = event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        val cosR = cos(Math.toRadians(rotDeg)); val sinR = sin(Math.toRadians(rotDeg))
        val rbx = event.markerBuildingX * cosR + event.markerBuildingZ * sinR
        val rbz = -event.markerBuildingX * sinR + event.markerBuildingZ * cosR
        alignOffX = event.markerArX - rbx; alignOffY = event.markerArY - event.markerBuildingY
        alignOffZ = event.markerArZ - rbz; alignRotYDeg = rotDeg

        routeRenderer.setAlignmentTransform(alignOffX, alignOffY, alignOffZ, alignRotYDeg)
        remainingDistance = 13.0

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
        updateUI()

        if (!isSimulated) startPoseUpdates()
    }

    private fun startPoseUpdates() {
        stopPoseUpdates()
        poseRunnable = object : Runnable {
            override fun run() {
                sampleCameraPose()
                handler.postDelayed(this, 500)
            }
        }
        handler.postDelayed(poseRunnable!!, 500)
    }

    private fun stopPoseUpdates() {
        poseRunnable?.let { handler.removeCallbacks(it) }
        poseRunnable = null
    }

    private fun sampleCameraPose() {
        if (!isAligned || isSimulated) return
        val session = sessionManager.session ?: return
        val frame = try { session.update() } catch (e: Exception) { null } ?: return
        val pose = frame.camera.pose
        val arX = pose.tx().toDouble(); val arY = pose.ty().toDouble(); val arZ = pose.tz().toDouble()

        // Inverse alignment: AR-world → building-local
        val rad = Math.toRadians(-alignRotYDeg)
        val cosR = cos(rad); val sinR = sin(rad)
        val tx = arX - alignOffX; val tz = arZ - alignOffZ
        val bx = tx * cosR + tz * sinR
        val bz = -tx * sinR + tz * cosR

        // Nearest-segment projection
        val routePoints = arrayOf(
            doubleArrayOf(0.0, 0.0), doubleArrayOf(3.0, 0.0), doubleArrayOf(6.0, 0.0),
            doubleArrayOf(6.0, 4.0), doubleArrayOf(3.0, 4.0),
        )
        val segLengths = doubleArrayOf(3.0, 3.0, 4.0, 3.0)
        val totalDist = 13.0

        var bestDist = Double.MAX_VALUE; var bestCum = 0.0; var cumDist = 0.0
        for (i in 0 until routePoints.size - 1) {
            val ax = routePoints[i][0]; val az = routePoints[i][1]
            val bpx = routePoints[i+1][0]; val bpz = routePoints[i+1][1]
            val dx = bpx - ax; val dz = bpz - az
            var t = ((bx - ax) * dx + (bz - az) * dz) / (dx * dx + dz * dz)
            t = max(0.0, min(1.0, t))
            val px = ax + t * dx; val pz = az + t * dz
            val d = sqrt((bx - px) * (bx - px) + (bz - pz) * (bz - pz))
            if (d < bestDist) { bestDist = d; bestCum = cumDist + t * segLengths[i] }
            cumDist += segLengths[i]
        }

        val newProgress = bestCum / totalDist
        if (newProgress > progress) progress = min(newProgress, 1.0)
        remainingDistance = max(0.0, totalDist - bestCum)
        isLowConfidence = bestDist > 3.0

        runOnUiThread { updateUI(); checkArrival() }
    }

    private fun updateUI() {
        progressBar.progress = (progress * 100).toInt()
        progressLabel.text = "Progress: ${(progress * 100).toInt()}%"
        remainingLabel.text = "${String.format("%.1f", remainingDistance)}m remaining"
        confidenceLabel.text = "Confidence: ${if (isLowConfidence) "⚠ Low" else "OK"}"
    }

    private fun checkArrival() {
        if (progress >= 0.95 || remainingDistance < 1.5) {
            arriveAtDestination()
        } else if (progress >= 0.8) {
            instructionLabel.text = "Approaching destination..."
            stateLabel.text = "● Approaching"; stateLabel.setTextColor(0xFF4CAF50.toInt())
        }
    }

    private fun arriveAtDestination() {
        stopPoseUpdates()
        arrivalOverlay.visibility = View.VISIBLE
        instructionLabel.visibility = View.GONE
        advanceButton.visibility = View.GONE
        rescanButton.visibility = View.GONE
        endButton.visibility = View.GONE
        stateLabel.text = "● Arrived"; stateLabel.setTextColor(0xFF4CAF50.toInt())
    }
}
