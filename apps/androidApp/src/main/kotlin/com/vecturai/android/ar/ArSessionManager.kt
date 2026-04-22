package com.vecturai.android.ar

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.FatalException

sealed class SessionFailure(open val reason: String, open val detail: String) {
    data class General(override val reason: String, override val detail: String) : SessionFailure(reason, detail)
    data class ArCoreInstallRequired(override val detail: String) : SessionFailure("ArCoreInstallRequired", detail)
    data class DeviceUnsupported(override val detail: String) : SessionFailure("DeviceUnsupported", detail)
    data class InvalidReferenceImage(override val detail: String) : SessionFailure("InvalidReferenceImage", detail)
}

class ArSessionManager {
    var session: Session? = null
        private set

    var isSessionRunning: Boolean = false
        private set

    var trackingStateDescription: String = "Not started"
        private set

    var lastFailure: SessionFailure? = null
        private set

    var hasLoadedReferenceImages: Boolean = false
        private set

    var loadedImageNames: List<String> = emptyList()
        private set

    var expectedMarkerName: String? = null
        private set

    fun createSessionWithoutMarker(activity: Activity): Result<Unit> {
        if (session != null) return Result.success(Unit)
        return try {
            val arSession = Session(activity)
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                focusMode = Config.FocusMode.AUTO
            }
            arSession.configure(config)
            session = arSession
            isSessionRunning = false
            hasLoadedReferenceImages = false
            trackingStateDescription = "Initialized without marker"
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun createSession(
        activity: Activity,
        markerImageAssetPath: String,
        markerImageName: String,
        markerWidthMeters: Double,
    ): Result<Int> {
        if (!ArFeatureFlags.ArUnifiedCameraPipeline || session == null) {
            stopSession()
        }
        expectedMarkerName = markerImageName
        loadedImageNames = emptyList()
        hasLoadedReferenceImages = false

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false
        }

        var bitmap = try {
            activity.assets.open(markerImageAssetPath).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (t: Throwable) {
            trackingStateDescription = "Marker asset missing"
            return Result.failure(t)
        } ?: return Result.failure(IllegalStateException("Marker image could not be decoded"))

        if (bitmap.config == Bitmap.Config.HARDWARE) {
            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            bitmap.recycle()
            if (softwareBitmap == null) {
                val detail = "Failed to copy hardware bitmap."
                lastFailure = SessionFailure.InvalidReferenceImage(detail)
                trackingStateDescription = detail
                return Result.failure(IllegalStateException(detail))
            }
            bitmap = softwareBitmap
        }

        val opaqueBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(opaqueBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        bitmap.recycle()
        bitmap = opaqueBitmap

        return try {
            println("[ARDiag] Before Session(activity)")
            val arSession = session ?: Session(activity)
            println("[ARDiag] After Session(activity)")
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                focusMode = Config.FocusMode.AUTO
            }

            val imageDatabase = AugmentedImageDatabase(arSession)
            println("[ARDiag] Before addImage width=${bitmap.width} height=${bitmap.height} config=${bitmap.config}")
            val imageIndex = imageDatabase.addImage(
                markerImageName,
                bitmap,
                markerWidthMeters.toFloat(),
            )
            println("[ARDiag] After addImage index=$imageIndex")
            bitmap.recycle()

            if (imageIndex == -1) {
                val detail = "marker rejected by ARCore — poor tracking quality"
                lastFailure = SessionFailure.InvalidReferenceImage(detail)
                trackingStateDescription = detail
                return Result.failure(IllegalStateException(detail))
            }

            config.augmentedImageDatabase = imageDatabase
            println("[ARDiag] Before configure(config)")
            arSession.configure(config)
            println("[ARDiag] After configure(config)")

            session = arSession
            hasLoadedReferenceImages = true
            loadedImageNames = listOf(markerImageName)
            trackingStateDescription = "Initializing..."
            println("[ArSession] Session created with marker '$markerImageName' ($markerWidthMeters m)")
            Result.success(imageIndex)
        } catch (t: Throwable) {
            bitmap.recycle()
            println("[ARDiag] createSession caught ${t.javaClass.simpleName}: ${t.message}\n${t.stackTraceToString().take(600)}")
            val type = t::class.java.simpleName.ifBlank { t::class.java.name }
            val msg = t.message
            val isFatal = t is FatalException
            val finalMsg = if (msg.isNullOrBlank() || msg == "null") {
                if (isFatal) "AR couldn't start on this device. Please close other camera apps and try again." else type
            } else {
                if (isFatal) "AR couldn't start on this device. Please close other camera apps and try again. ($type: $msg)" else "$type: $msg"
            }
            trackingStateDescription = finalMsg
            lastFailure = SessionFailure.General(type, finalMsg)
            Result.failure(t)
        }
    }

    fun setCameraTexture(textureId: Int): Boolean {
        val arSession = session ?: return false
        if (textureId == 0) return false
        return try {
            println("[ARDiag] Before setCameraTextureName($textureId)")
            arSession.setCameraTextureName(textureId)
            println("[ARDiag] After setCameraTextureName($textureId)")
            true
        } catch (t: Throwable) {
            println("[ARDiag] setCameraTextureName caught ${t.javaClass.simpleName}: ${t.message}\n${t.stackTraceToString().take(600)}")
            val type = t::class.java.simpleName.ifBlank { t::class.java.name }
            val msg = t.message
            val finalMsg = if (msg.isNullOrBlank() || msg == "null") "Camera texture error: $type" else "Camera texture error: $type: $msg"
            trackingStateDescription = finalMsg
            false
        }
    }

    fun resumeSession(): Boolean {
        val arSession = session
        if (arSession == null) {
            isSessionRunning = false
            trackingStateDescription = "AR session is not ready"
            return false
        }
        return try {
            println("[ARDiag] Before resume()")
            arSession.resume()
            println("[ARDiag] After resume()")
            isSessionRunning = true
            trackingStateDescription = "Running"
            lastFailure = null
            true
        } catch (t: Throwable) {
            println("[ARDiag] resume caught ${t.javaClass.simpleName}: ${t.message}\n${t.stackTraceToString().take(600)}")
            isSessionRunning = false
            val type = t::class.java.simpleName.ifBlank { t::class.java.name }
            val msg = t.message
            val isFatal = t is FatalException
            val finalMsg = if (msg.isNullOrBlank() || msg == "null") {
                if (isFatal) "AR couldn't start on this device. Please close other camera apps and try again." else type
            } else {
                if (isFatal) "AR couldn't start on this device. Please close other camera apps and try again. ($type: $msg)" else "$type: $msg"
            }
            trackingStateDescription = finalMsg
            
            lastFailure = when {
                t is CameraNotAvailableException -> SessionFailure.General("CameraNotAvailableException", finalMsg)
                else -> SessionFailure.General(type, finalMsg)
            }
            false
        }
    }

    fun pauseSession() {
        session?.pause()
        isSessionRunning = false
        trackingStateDescription = "Paused"
    }

    fun stopSession() {
        if (isSessionRunning) {
            session?.pause()
        }
        session?.close()
        session = null
        isSessionRunning = false
        hasLoadedReferenceImages = false
        trackingStateDescription = "Stopped"
        lastFailure = null
    }

    suspend fun awaitClosed() {
        kotlinx.coroutines.delay(150L)
    }
}
