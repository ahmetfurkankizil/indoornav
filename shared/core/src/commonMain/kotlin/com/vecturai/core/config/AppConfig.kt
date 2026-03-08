package com.vecturai.core.config

/**
 * App configuration profiles.
 *
 * Controls debug overlays, logging, demo mode defaults, and
 * simulate-scan availability per build variant.
 */
sealed class AppConfig {

    abstract val profileName: String
    abstract val logVerbose: Boolean
    abstract val showDebugOverlays: Boolean
    abstract val simulateAlignmentEnabled: Boolean
    abstract val preloadSamplePackage: Boolean
    abstract val showDemoModeLabel: Boolean
    abstract val defaultToDemoMode: Boolean

    /** Development profile — everything enabled. */
    data object Dev : AppConfig() {
        override val profileName = "dev"
        override val logVerbose = true
        override val showDebugOverlays = true
        override val simulateAlignmentEnabled = true
        override val preloadSamplePackage = true
        override val showDemoModeLabel = true
        override val defaultToDemoMode = false
    }

    /** Demo/investor profile — simulate enabled, debug hidden by default. */
    data object Demo : AppConfig() {
        override val profileName = "demo"
        override val logVerbose = false
        override val showDebugOverlays = false
        override val simulateAlignmentEnabled = true
        override val preloadSamplePackage = true
        override val showDemoModeLabel = true
        override val defaultToDemoMode = true
    }

    /** Release candidate — production-like, no simulate. */
    data object Release : AppConfig() {
        override val profileName = "release"
        override val logVerbose = false
        override val showDebugOverlays = false
        override val simulateAlignmentEnabled = false
        override val preloadSamplePackage = false
        override val showDemoModeLabel = false
        override val defaultToDemoMode = false
    }

    companion object {
        /** Current active config. Set at app startup. */
        var current: AppConfig = Dev

        fun fromName(name: String): AppConfig = when (name.lowercase()) {
            "demo" -> Demo
            "release" -> Release
            else -> Dev
        }
    }
}
