package com.Vectura AI.android.ar

import android.app.Activity
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

class UnifiedArSession {
    var session: Session? = null
        private set

    var isRunning: Boolean = false
        private set

    private var installRequested = true
    private var boundTextureId = 0

    fun onActivityResume(
        activity: Activity,
        cameraTextureId: Int,
        // Marker parameters purged as per user request
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

        val arSession = session ?: createSession(activity).getOrElse { error ->
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
        }
        isRunning = false
    }

    fun onActivityDestroy() {
        try {
            session?.close()
        } catch (_: Throwable) {
        }
        session = null
        isRunning = false
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

    private fun createSession(activity: Activity): Result<Session> {
        return try {
            val arSession = Session(activity)
            val config = Config(arSession).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                // AugmentedImageDatabase purged as per user request
            }

            arSession.configure(config)
            session = arSession
            println("[ARDiag] Session created (Marker-free)")
            Result.success(arSession)
        } catch (t: Throwable) {
            Result.failure(t)
        }
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
}
