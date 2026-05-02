package com.vecturai.android.ar

import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlin.math.atan2

class ArMarkerDetector {
    data class KnownMarker(
        val markerId: String,
        val role: MarkerDetectionRole,
        val nearestNodeId: String,
        val buildingX: Double,
        val buildingY: Double,
        val buildingZ: Double,
        val buildingRotationYDeg: Double,
    )

    enum class MarkerDetectionRole {
        ENTRANCE,
        CHECKPOINT,
    }

    sealed interface DetectionFailureReason {
        data object NoCandidatesSeen : DetectionFailureReason
        data class CandidatesRejected(val seen: Int, val names: List<String>) : DetectionFailureReason
        data object AssetMissing : DetectionFailureReason
    }

    private val knownMarkersByIndex = mutableMapOf<Int, KnownMarker>()
    private val knownMarkersByName = mutableMapOf<String, KnownMarker>()

    var expectedMarkerName: String? = null
        private set

    var hasDetectedMarker: Boolean = false
        private set

    var totalCandidatesSeen: Int = 0
        private set

    var framesAnalyzed: Int = 0
        private set

    var rejectedCandidates: Int = 0
        private set

    var rejectedNames: List<String> = emptyList()
        private set

    val detectionFailureReason: DetectionFailureReason
        get() = if (totalCandidatesSeen == 0) {
            DetectionFailureReason.NoCandidatesSeen
        } else {
            DetectionFailureReason.CandidatesRejected(totalCandidatesSeen, rejectedNames)
        }

    var onMarkerDetected: ((MarkerDetectionEvent) -> Unit)? = null
    var onCheckpointDetected: ((MarkerDetectionEvent) -> Unit)? = null

    fun configure(
        markerId: String,
        markerName: String?,
        nearestNodeId: String,
        buildingX: Double,
        buildingY: Double,
        buildingZ: Double,
        buildingRotationYDeg: Double,
    ) {
        expectedMarkerName = markerName
        hasDetectedMarker = false
        framesAnalyzed = 0
        totalCandidatesSeen = 0
        rejectedCandidates = 0
        rejectedNames = emptyList()
        knownMarkersByIndex.clear()
        knownMarkersByName.clear()

        if (markerName != null) {
            knownMarkersByName[markerName] = KnownMarker(
                markerId = markerId,
                role = MarkerDetectionRole.ENTRANCE,
                nearestNodeId = nearestNodeId,
                buildingX = buildingX,
                buildingY = buildingY,
                buildingZ = buildingZ,
                buildingRotationYDeg = buildingRotationYDeg,
            )
        }
    }

    fun registerMarker(index: Int, name: String, marker: KnownMarker) {
        knownMarkersByIndex[index] = marker
        knownMarkersByName[name] = marker
    }

    fun processFrame(frame: Frame) {
        framesAnalyzed += 1
        val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (image in augmentedImages) {
            if (image.trackingState != TrackingState.TRACKING) continue
            if (image.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING) continue

            totalCandidatesSeen += 1
            val detectedName = image.name
            val known = knownMarkersByName[detectedName] ?: knownMarkersByIndex[image.index]
            if (known == null) {
                rejectedCandidates += 1
                if (detectedName !in rejectedNames) {
                    rejectedNames = rejectedNames + detectedName
                }
                println("[MarkerDetector] Rejected '$detectedName' (expected '${expectedMarkerName ?: "none"}')")
                continue
            }

            val pose = image.centerPose
            val qx = pose.qx().toDouble()
            val qy = pose.qy().toDouble()
            val qz = pose.qz().toDouble()
            val qw = pose.qw().toDouble()
            val arRotationYDeg = Math.toDegrees(
                atan2(2.0 * (qw * qy + qx * qz), 1.0 - 2.0 * (qy * qy + qz * qz))
            )

            val event = MarkerDetectionEvent(
                markerId = known.markerId,
                entranceNodeId = known.nearestNodeId,
                markerBuildingX = known.buildingX,
                markerBuildingY = known.buildingY,
                markerBuildingZ = known.buildingZ,
                markerArX = pose.tx().toDouble(),
                markerArY = pose.ty().toDouble(),
                markerArZ = pose.tz().toDouble(),
                markerArRotationYDeg = arRotationYDeg,
                markerBuildingRotationYDeg = known.buildingRotationYDeg,
                confidence = 1.0,
                role = known.role,
            )

            when (known.role) {
                MarkerDetectionRole.ENTRANCE -> {
                    if (!hasDetectedMarker) {
                        hasDetectedMarker = true
                        println("[MarkerDetector] Entrance marker aligned: $detectedName")
                        onMarkerDetected?.invoke(event)
                    }
                }

                MarkerDetectionRole.CHECKPOINT -> {
                    onCheckpointDetected?.invoke(event)
                }
            }
        }
    }

    fun reset() {
        hasDetectedMarker = false
        framesAnalyzed = 0
        totalCandidatesSeen = 0
        rejectedCandidates = 0
        rejectedNames = emptyList()
    }

    fun fullReset() {
        reset()
        knownMarkersByIndex.clear()
        knownMarkersByName.clear()
    }
}

data class MarkerDetectionEvent(
    val markerId: String,
    val entranceNodeId: String,
    val markerBuildingX: Double,
    val markerBuildingY: Double,
    val markerBuildingZ: Double,
    val markerArX: Double,
    val markerArY: Double,
    val markerArZ: Double,
    val markerArRotationYDeg: Double,
    val markerBuildingRotationYDeg: Double,
    val confidence: Double,
    val role: ArMarkerDetector.MarkerDetectionRole,
)
