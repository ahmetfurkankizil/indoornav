package com.example.vecturai.ar

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class CloudAnchorAuthStatus(
    val isConfigured: Boolean,
    val message: String
) {
    companion object {
        fun from(context: Context): CloudAnchorAuthStatus {
            val apiKey = context.readArCoreApiKey()
            return if (apiKey.isNullOrBlank()) {
                CloudAnchorAuthStatus(
                    isConfigured = false,
                    message = "Cloud Anchor API key is not configured. Add ARCORE_API_KEY before hosting or resolving anchors."
                )
            } else {
                CloudAnchorAuthStatus(
                    isConfigured = true,
                    message = "Cloud Anchor API key is configured for same-day demo anchors."
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun Context.readArCoreApiKey(): String? {
    val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(
            packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        )
    } else {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }
    return appInfo.metaData?.getString("com.google.android.ar.API_KEY")
}
