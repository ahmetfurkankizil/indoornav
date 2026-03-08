package com.vecturai.core.config

import com.vecturai.core.navigation.NavigationSession
import kotlinx.serialization.Serializable

/**
 * Diagnostic snapshot for debug/QA.
 *
 * Captures current app state for troubleshooting and demo verification.
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
    val activeSessionId: String?,
    val activeDestination: String?,
    val sessionMode: String?,
    val historyCount: Int,
    val isDemoMode: Boolean,
    val simulateEnabled: Boolean,
) {
    fun toDisplayLines(): List<String> = listOf(
        "App: $appVersion",
        "Config: $configProfile",
        "Building: ${loadedBuildingName ?: "None"} (${loadedBuildingId ?: "-"})",
        "Nodes: $packageNodeCount | Rooms: $packageRoomCount | Markers: $packageMarkerCount",
        "Session: ${activeSessionId ?: "None"}",
        "Destination: ${activeDestination ?: "-"}",
        "Mode: ${sessionMode ?: "-"}",
        "History: $historyCount entries",
        "Demo: $isDemoMode | Simulate: $simulateEnabled",
    )
}
