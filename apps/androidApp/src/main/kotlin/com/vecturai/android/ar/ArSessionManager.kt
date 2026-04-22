package com.vecturai.android.ar

import android.content.Context
import android.graphics.BitmapFactory
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session

class ArSessionManager {
    var session: Session? = null
        private set

    var isSessionRunning: Boolean = false
        private set

    var trackingStateDescription: String = "Not started"
        private set

    var hasLoadedReferenceImages: Boolean = false
        private set

    var loadedImageNames: List<String> = emptyList()
        private set

    var expectedMarkerName: String? = null
        private set

    fun createSession(
        context: Context,
        markerImageAssetPath: String,
        markerImageName: String,
        markerWidthMeters: Double,
    ): Result<Int> {
        stopSession()
        expectedMarkerName = markerImageName
        loadedImageNames = emptyList()
        hasLoadedReferenceImages = false

        val bitmap = try {
            context.assets.open(markerImageAssetPath).use { BitmapFactory.decodeStream(it) }
        } catch (t: Throwable) {
            trackingStateDescription = "Marker asset missing"
            return Result.failure(t)
        } ?: return Result.failure(IllegalStateException("Marker image could not be decoded"))

        return try {
            val arSession = Session(context)
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                focusMode = Config.FocusMode.AUTO
            }

            val imageDatabase = AugmentedImageDatabase(arSession)
            val imageIndex = imageDatabase.addImage(
                markerImageName,
                bitmap,
                markerWidthMeters.toFloat(),
            )
            bitmap.recycle()

            config.augmentedImageDatabase = imageDatabase
            arSession.configure(config)

            session = arSession
            hasLoadedReferenceImages = true
            loadedImageNames = listOf(markerImageName)
            trackingStateDescription = "Initializing..."
            println("[ArSession] Session created with marker '$markerImageName' ($markerWidthMeters m)")
            Result.success(imageIndex)
        } catch (t: Throwable) {
            bitmap.recycle()
            trackingStateDescription = "Error: ${t.message}"
            Result.failure(t)
        }
    }

    fun setCameraTexture(textureId: Int) {
        session?.setCameraTextureName(textureId)
    }

    fun resumeSession() {
        try {
            session?.resume()
            isSessionRunning = true
            trackingStateDescription = "Running"
        } catch (t: Throwable) {
            trackingStateDescription = "Error: ${t.message}"
        }
    }

    fun pauseSession() {
        session?.pause()
        isSessionRunning = false
        trackingStateDescription = "Paused"
    }

    fun stopSession() {
        session?.close()
        session = null
        isSessionRunning = false
        hasLoadedReferenceImages = false
        trackingStateDescription = "Stopped"
    }
}
