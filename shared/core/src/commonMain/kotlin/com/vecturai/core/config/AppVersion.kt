package com.vecturai.core.config

/**
 * App version and build metadata.
 */
object AppVersion {
    const val NAME = "VecturAI"
    const val VERSION = "1.2.0"
    const val BUILD_PHASE = "Phase 5 — Demo Ready"
    const val BUILD_DATE = "2026-03-08"

    fun displayString(): String = "$NAME v$VERSION"
    fun fullString(): String = "$NAME v$VERSION ($BUILD_PHASE, $BUILD_DATE)"
}
