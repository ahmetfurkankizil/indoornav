package com.vecturai.android.ar

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlin.math.*

/**
 * AR navigation activity. 
 * Allows users to navigate to a destination via AR markers or manual simulation.
 */
class ArNavigationActivity : ComponentActivity() {

    private val sessionManager = ArSessionManager()
    private val markerDetector = ArMarkerDetector()
    private val routeRenderer = ArRouteRenderer()
    private lateinit var cameraRenderer: ArCameraRenderer
    private lateinit var glSurfaceView: GLSurfaceView

    private lateinit var stateLabel: TextView
    private lateinit var instructionLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var simulateButton: Button
    
    private var isAligned = false
    private var isSimulated = false
    private var progress = 0.0
    private var remainingDistance = 0.0
    private var installRequested = false

    private val handler = Handler(Looper.getMainLooper())
    private var poseRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destinationName = intent.getStringExtra("destinationName") ?: "Destination"

        markerDetector.configure(
            markerId = "marker-main", nearestNodeId = "n01",
            buildingX = 0.0, buildingY = 1.2, buildingZ = 0.0, buildingRotationYDeg = 0.0,
        )
        markerDetector.onMarkerDetected = { event ->
            runOnUiThread { handleMarkerDetected(event) }
        }

        val container = FrameLayout(this)

        cameraRenderer = ArCameraRenderer { sessionManager.session }
        glSurfaceView = GLSurfaceView(this).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setRenderer(cameraRenderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        container.addView(glSurfaceView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
        ))

        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 64)
        }

        stateLabel = TextView(this).apply {
            text = "● WAITING FOR MARKER"; textSize = 14f; setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(4f, 0f, 0f, 0xFF000000.toInt())
        }
        overlay.addView(stateLabel)

        instructionLabel = TextView(this).apply {
            text = "Point camera at the entrance marker image"; textSize = 16f
            setTextColor(0xCCFFFFFF.toInt()); setPadding(0, 8, 0, 0)
        }
        overlay.addView(instructionLabel)

        overlay.addView(FrameLayout(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f,
        ))

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = 0; visibility = View.GONE
        }
        overlay.addView(progressBar)

        simulateButton = Button(this).apply {
            text = "⚡ START MANUALLY (SKIP MARKER)"; setPadding(32, 16, 32, 16)
            setBackgroundColor(0xFFFF9800.toInt()); setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener { simulateAlignment() }
        }
        overlay.addView(simulateButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 32 })

        val closeButton = Button(this).apply {
            text = "✕ CLOSE"; setOnClickListener { finish() }
        }
        overlay.addView(closeButton)

        container.addView(overlay)
        setContentView(container)
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            checkArCoreAndResume()
        } else {
            requestCameraPermission()
        }
    }

    private fun checkCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkArCoreAndResume()
        }
    }

    private fun checkArCoreAndResume() {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return
                }
                ArCoreApk.InstallStatus.INSTALLED -> resumeAr()
            }
        } catch (e: Exception) { resumeAr() }
    }

    private fun resumeAr() {
        sessionManager.createSession(this)
        sessionManager.resumeSession()
        glSurfaceView.onResume()
    }

    private fun simulateAlignment() {
        isSimulated = true
        handleMarkerDetected(MarkerDetectionEvent(
            markerId = "marker-main", entranceNodeId = "n01",
            markerBuildingX = 0.0, markerBuildingY = 1.2, markerBuildingZ = 0.0,
            markerArX = 0.0, markerArY = 0.0, markerArZ = -1.0,
            markerArRotationYDeg = 0.0, markerBuildingRotationYDeg = 0.0, confidence = 1.0,
            role = ArMarkerDetector.MarkerDetectionRole.ENTRANCE,
        ))
    }

    private fun handleMarkerDetected(event: MarkerDetectionEvent) {
        isAligned = true
        stateLabel.text = "● NAVIGATING"; stateLabel.setTextColor(0xFF4CAF50.toInt())
        instructionLabel.text = "Follow the AR arrows on the floor"
        simulateButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        
        routeRenderer.setAlignmentTransform(
            event.markerArX - event.markerBuildingX,
            event.markerArY - event.markerBuildingY,
            event.markerArZ - event.markerBuildingZ,
            event.markerArRotationYDeg - event.markerBuildingRotationYDeg
        )

        val demoArrows = listOf(
            ArrowRenderData("a0", 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a1", 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, ArrowRenderType.FOLLOW, null),
            ArrowRenderData("a2", 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, ArrowRenderType.TURN_RIGHT, "Turn Right")
        )
        routeRenderer.updateArrows(demoArrows)

        if (!isSimulated) startPoseUpdates()
    }

    private fun startPoseUpdates() {
        poseRunnable = object : Runnable {
            override fun run() {
                // In real app, update progress based on camera pose
                handler.postDelayed(this, 1000)
            }
        }
        handler.postAtFrontOfQueue(poseRunnable!!)
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        sessionManager.pauseSession()
        poseRunnable?.let { handler.removeCallbacks(it) }
    }
}
