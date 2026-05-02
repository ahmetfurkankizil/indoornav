package com.vecturai.android.ar

import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState

class ArMarkerDetector {
    // This class is preserved to avoid massive refactoring of constructor dependencies,
    // but all active logic and data classes related to marker detection have been purged.

    enum class MarkerDetectionRole {
        ENTRANCE,
        CHECKPOINT,
    }

    sealed interface DetectionFailureReason {
        data object NoCandidatesSeen : DetectionFailureReason
        data class CandidatesRejected(val seen: Int, val names: List<String>) : DetectionFailureReason
        data object AssetMissing : DetectionFailureReason
    }

    val detectionFailureReason: DetectionFailureReason = DetectionFailureReason.NoCandidatesSeen

    fun configure(
        markerId: String,
        markerName: String?,
        nearestNodeId: String,
        buildingX: Double,
        buildingY: Double,
        buildingZ: Double,
        buildingRotationYDeg: Double,
    ) {
        // Purged
    }

    fun registerMarker(index: Int, name: String, marker: Any) {
        // Purged
    }

    fun processFrame(frame: Frame) {
        // Purged
    }

    fun reset() {
        // Purged
    }

    fun fullReset() {
        // Purged
    }
}
