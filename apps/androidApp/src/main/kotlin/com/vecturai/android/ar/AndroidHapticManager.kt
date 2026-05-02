package com.vecturai.android.ar

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AndroidHapticManager(context: Context) {
    var isEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun routeStarted() = vibrate(35, VibrationEffect.DEFAULT_AMPLITUDE)
    fun turnImminent() = vibrate(90, 180)
    fun recentering() = vibrate(25, 90)
    fun arrived() = vibrate(140, VibrationEffect.DEFAULT_AMPLITUDE)
    fun lightTick() = vibrate(18, 80)
    fun selection() = vibrate(16, 70)
    fun success() = vibrate(45, VibrationEffect.DEFAULT_AMPLITUDE)
    fun warning() = vibrate(70, 160)
    fun error() = vibrate(120, 210)

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (!isEnabled || !HapticsEnabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    companion object {
        var HapticsEnabled: Boolean =
            System.getProperty("vecturai.haptics.enabled")?.toBooleanStrictOrNull()
                ?: System.getenv("VECTURAI_HAPTICS_ENABLED")?.toBooleanStrictOrNull()
                ?: true
    }
}
