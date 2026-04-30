package com.vecturai.android.ar

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.vecturai.android.data.AndroidReviewedPackageLoader

class UnifiedArSession {
    var session: Session? = null
        private set

    var isRunning: Boolean = false
        private set

    var referenceImageIndex: Int? = null
        private set

    private var installRequested = true
    private var boundTextureId = 0

    fun onActivityResume(
        activity: Activity,
        cameraTextureId: Int,
        marker: AndroidReviewedPackageLoader.PackageMarker?,
        markerBitmap: Bitmap? = null,
    ): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        if (cameraTextureId == 0) {
            finishWithToast(activity, "AR camera texture is not ready.")
            return false
        }

        if (!ensureArCoreInstalled(activity)) {
            return false
        }

        val arSession = session ?: createSession(activity, marker, markerBitmap).getOrElse { error ->
            finishWithToast(activity, detailFor(error, "AR camera could not start."))
            return false
        }

        return try {
            if (boundTextureId != cameraTextureId) {
                arSession.setCameraTextureName(cameraTextureId)
                boundTextureId = cameraTextureId
            }
            arSession.resume()
            isRunning = true
            true
        } catch (t: Throwable) {
            isRunning = false
            finishWithToast(activity, detailFor(t, "AR camera could not resume."))
            false
        }
    }

    fun onActivityPause() {
        try {
            session?.pause()
        } catch (_: Throwable) {
            // The Activity owns this session; teardown remains deterministic on destroy.
        }
        isRunning = false
    }

    fun onActivityDestroy() {
        try {
            session?.close()
        } catch (_: Throwable) {
            // Nothing useful to recover here; the hosting Activity is already ending.
        }
        session = null
        isRunning = false
        referenceImageIndex = null
        boundTextureId = 0
    }

    private fun ensureArCoreInstalled(activity: Activity): Boolean {
        return try {
            when (ArCoreApk.getInstance().requestInstall(activity, installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = false
                    false
                }
                ArCoreApk.InstallStatus.INSTALLED -> true
            }
        } catch (t: Throwable) {
            finishWithToast(activity, detailFor(t, "ARCore is required for navigation."))
            false
        }
    }

    /**
     * Updates the AR session configuration with a new marker image.
     */
    fun reconfigure(
        activity: Activity,
        marker: AndroidReviewedPackageLoader.PackageMarker,
        bitmap: Bitmap
    ): Int {
        val arSession = session ?: return -1
        return try {
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }

            val imageDatabase = AugmentedImageDatabase(arSession)
            val name = marker.referenceImageName ?: "dynamic_marker"
            val imageIndex = imageDatabase.addImage(
                name,
                bitmap,
                marker.physicalWidthMeters.toFloat(),
            )
            
            if (imageIndex != -1) {
                config.augmentedImageDatabase = imageDatabase
                arSession.configure(config)
                referenceImageIndex = imageIndex
                println("[ARDiag] Session reconfigured with dynamic marker '$name' at index $imageIndex")
                imageIndex
            } else {
                -1
            }
        } catch (t: Throwable) {
            println("[ARDiag] Failed to reconfigure session: ${t.message}")
            -1
        }
    }

    private fun createSession(
        activity: Activity,
        marker: AndroidReviewedPackageLoader.PackageMarker?,
        markerBitmap: Bitmap? = null,
    ): Result<Session> {
        return try {
            val arSession = Session(activity)
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }

            val imageDatabase = AugmentedImageDatabase(arSession)
            
            // Only add an image if we actually have one that is likely to be valid
            val bitmap = markerBitmap ?: marker?.let { 
                loadSanitizedBitmap(activity, "ar/${it.referenceImageName}.png").getOrNull()
            }

            if (bitmap != null) {
                val markerWidthMeters = marker?.physicalWidthMeters ?: DEFAULT_MARKER_WIDTH_METERS
                val imageIndex = imageDatabase.addImage(
                    marker?.referenceImageName ?: DEFAULT_MARKER_IMAGE_NAME,
                    bitmap,
                    markerWidthMeters.toFloat(),
                )
                referenceImageIndex = if (imageIndex != -1) imageIndex else null
            } else {
                referenceImageIndex = null
            }

            config.augmentedImageDatabase = imageDatabase
            arSession.configure(config)
            session = arSession
            println("[ARDiag] Session created (database size: ${imageDatabase.numImages})")
            Result.success(arSession)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun loadSanitizedBitmap(activity: Activity, assetPath: String): Result<Bitmap> {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false
        }

        var decoded = try {
            activity.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (t: Throwable) {
            return Result.failure(t)
        } ?: return Result.failure(IllegalStateException("Entrance poster image could not be decoded."))

        if (decoded.config == Bitmap.Config.HARDWARE) {
            val software = decoded.copy(Bitmap.Config.ARGB_8888, false)
            decoded.recycle()
            decoded = software ?: return Result.failure(
                IllegalStateException("Entrance poster image could not be copied for ARCore.")
            )
        }

        val opaque = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(opaque)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(decoded, 0f, 0f, null)
        decoded.recycle()
        return Result.success(opaque)
    }

    private fun finishWithToast(activity: Activity, message: String) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }

    private fun detailFor(error: Throwable, fallback: String): String {
        val type = error::class.java.simpleName.ifBlank { error::class.java.name }
        val message = error.message
        return if (message.isNullOrBlank() || message == "null") {
            "$fallback ($type)"
        } else {
            "$fallback ($type: $message)"
        }
    }

    private companion object {
        private const val DEFAULT_MARKER_IMAGE_NAME = "entrance_marker"
        private const val DEFAULT_MARKER_WIDTH_METERS = 0.15
    }
}
