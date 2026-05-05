package com.VecturAI.core.config

/**
 * App version and build metadata.
 *
 * Single source of truth for version across all platforms.
 * Update this file when cutting a release or RC.
 */
object AppVersion {
    const val NAME = "VecturAI"
    const val VERSION = "1.7.0-rc1"
    const val BUILD_PHASE = "Phase 8 — RC"
    const val BUILD_DATE = "2026-03-10"

    fun displayString(): String = "$NAME v$VERSION"
    fun fullString(): String = "$NAME v$VERSION ($BUILD_PHASE, $BUILD_DATE)"
}
