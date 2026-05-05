package com.VecturAI.android.ui

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
fun rememberAuroraIntensity(): Float {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    return remember(context, reduceMotion) {
        if (reduceMotion) {
            0f
        } else {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isPowerSaveMode == true) 0.35f else 1f
        }
    }
}
