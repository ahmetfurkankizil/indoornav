package com.vecturai.android.ar

import android.content.Context
import android.graphics.BitmapFactory
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

/**
 * Manages ARCore session lifecycle and configuration.
 *
 * Configures world tracking with augmented image detection
 * for entrance marker recognition.
 */
class ArSessionManager {

    var session: Session? = null
        private set

    var isSessionRunning: Boolean = false
        private set

    var trackingStateDescription: String = "Not started"
        private set

    /**
     * Create and configure an ARCore session with image detection.
     *
     * @param context Android context
     * @param markerImageResId Resource ID of marker reference image (or 0 to skip)
     * @param markerWidthMeters Physical width of the marker
     */
    fun createSession(context: Context, markerImageResId: Int = 0, markerWidthMeters: Float = 0.21f) {
        try {
            val arSession = Session(context)

            val config = Config(arSession)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.focusMode = Config.FocusMode.AUTO

            // Configure augmented image database
            if (markerImageResId != 0) {
                val imageDb = AugmentedImageDatabase(arSession)
                val bitmap = BitmapFactory.decodeResource(context.resources, markerImageResId)
                if (bitmap != null) {
                    imageDb.addImage("entrance_marker_main", bitmap, markerWidthMeters)
                    config.augmentedImageDatabase = imageDb
                    println("[ArSession] Added marker image to detection database")
                }
            } else {
                // Create empty database — marker detection disabled
                config.augmentedImageDatabase = AugmentedImageDatabase(arSession)
                println("[ArSession] No marker image configured")
            }

            arSession.configure(config)
            session = arSession
            println("[ArSession] Session created")

        } catch (e: Exception) {
            println("[ArSession] Failed to create session: ${e.message}")
            trackingStateDescription = "Error: ${e.message}"
        }
    }

    /** Resume the AR session. */
    fun resumeSession() {
        try {
            session?.resume()
            isSessionRunning = true
            trackingStateDescription = "Running"
        } catch (e: Exception) {
            trackingStateDescription = "Error: ${e.message}"
        }
    }

    /** Pause the AR session. */
    fun pauseSession() {
        session?.pause()
        isSessionRunning = false
        trackingStateDescription = "Paused"
    }

    /** Stop and destroy the AR session. */
    fun stopSession() {
        session?.close()
        session = null
        isSessionRunning = false
        trackingStateDescription = "Stopped"
    }

    /** Get a human-readable tracking state from ARCore camera. */
    fun getTrackingStateLabel(): String {
        return when (session?.update()?.camera?.trackingState) {
            TrackingState.TRACKING -> "Normal"
            TrackingState.PAUSED -> "Paused"
            TrackingState.STOPPED -> "Stopped"
            else -> "Unknown"
        }
    }
}
