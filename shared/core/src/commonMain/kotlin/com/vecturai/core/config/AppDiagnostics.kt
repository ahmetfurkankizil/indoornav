package com.vecturai.core.config

import com.vecturai.core.navigation.NavigationSession
import kotlinx.serialization.Serializable

/**
 * Diagnostic snapshot for debug/QA.
 *
 * Captures current app state for troubleshooting and demo verification.
 * Extended in v1.6 with correction/confidence diagnostics.
 */
@Serializable
data class AppDiagnostics(
    val appVersion: String,
    val configProfile: String,
    val loadedBuildingId: String?,
    val loadedBuildingName: String?,
    val packageNodeCount: Int,
    val packageRoomCount: Int,
    val packageMarkerCount: Int,
    val packageCheckpointMarkerCount: Int = 0,
    val activeSessionId: String?,
    val activeDestination: String?,
    val sessionMode: String?,
    val historyCount: Int,
    val isDemoMode: Boolean,
    val simulateEnabled: Boolean,
    // v1.6 — correction & confidence
    val currentMarkerId: String? = null,
    val currentMarkerRole: String? = null,
    val lastCorrectionTimeMs: Long = 0L,
    val correctionCount: Int = 0,
    val correctionMagnitudeMeters: Double = 0.0,
    val alignmentConfidence: String = "NONE",
    val progressConfidence: String = "DEGRADED",
    val offRouteStatus: String = "ON_ROUTE",
    val isLiveMode: Boolean = false,
) {
    fun toDisplayLines(): List<String> = listOf(
        "App: $appVersion",
        "Config: $configProfile",
        "Building: ${loadedBuildingName ?: "None"} (${loadedBuildingId ?: "-"})",
        "Nodes: $packageNodeCount | Rooms: $packageRoomCount | Markers: $packageMarkerCount | Checkpoints: $packageCheckpointMarkerCount",
        "Session: ${activeSessionId ?: "None"}",
        "Destination: ${activeDestination ?: "-"}",
        "Mode: ${sessionMode ?: "-"} | Live: $isLiveMode",
        "Marker: ${currentMarkerId ?: "-"} (${currentMarkerRole ?: "-"})",
        "Corrections: $correctionCount (${((correctionMagnitudeMeters * 100).toInt() / 100.0)}m total)",
        "Alignment: $alignmentConfidence | Progress: $progressConfidence | Route: $offRouteStatus",
        "History: $historyCount entries",
        "Demo: $isDemoMode | Simulate: $simulateEnabled",
    )
}
