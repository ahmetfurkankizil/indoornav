package com.vecturai.android.ar

import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlin.math.atan2

/**
 * Detects entrance and checkpoint marker augmented images and reports events.
 *
 * Entrance markers trigger session alignment (once).
 * Checkpoint markers emit correction events without restarting the session.
 */
class ArMarkerDetector {

    /**
     * Known marker with registration metadata and role.
     */
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

    /** Registered markers by augmented image index. */
    private val knownMarkersByIndex = mutableMapOf<Int, KnownMarker>()

    /** Fallback entrance marker metadata for single-marker backward compat. */
    var markerId: String = ""
    var markerNearestNodeId: String = ""
    var markerBuildingX: Double = 0.0
    var markerBuildingY: Double = 0.0
    var markerBuildingZ: Double = 0.0
    var markerBuildingRotationYDeg: Double = 0.0

    var hasDetectedMarker: Boolean = false
        private set

    var onMarkerDetected: ((MarkerDetectionEvent) -> Unit)? = null
    var onCheckpointDetected: ((MarkerDetectionEvent) -> Unit)? = null

    /**
     * Configure the detector with entrance marker metadata (backward-compatible).
     */
    fun configure(
        markerId: String,
        nearestNodeId: String,
        buildingX: Double,
        buildingY: Double,
        buildingZ: Double,
        buildingRotationYDeg: Double,
    ) {
        this.markerId = markerId
        this.markerNearestNodeId = nearestNodeId
        this.markerBuildingX = buildingX
        this.markerBuildingY = buildingY
        this.markerBuildingZ = buildingZ
        this.markerBuildingRotationYDeg = buildingRotationYDeg
        this.hasDetectedMarker = false
    }

    /**
     * Register a known marker by augmented image index.
     * Call after adding images to the augmented image database.
     */
    fun registerMarker(index: Int, marker: KnownMarker) {
        knownMarkersByIndex[index] = marker
    }

    /**
     * Process an ARCore frame, checking for augmented image detections.
     * Call this each frame from the render loop.
     *
     * Unlike v1.5, continues processing checkpoint markers after entrance detection.
     */
    fun processFrame(frame: Frame) {
        val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (image in augmentedImages) {
            if (image.trackingState != TrackingState.TRACKING) continue
            if (image.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING) continue

            // Extract pose
            val pose = image.centerPose
            val arX = pose.tx().toDouble()
            val arY = pose.ty().toDouble()
            val arZ = pose.tz().toDouble()

            // Extract Y rotation from pose quaternion
            val qx = pose.qx().toDouble()
            val qy = pose.qy().toDouble()
            val qz = pose.qz().toDouble()
            val qw = pose.qw().toDouble()
            val arRotationYDeg = Math.toDegrees(
                atan2(2.0 * (qw * qy + qx * qz), 1.0 - 2.0 * (qy * qy + qz * qz))
            )

            // Try to match by index
            val known = knownMarkersByIndex[image.index]
            if (known != null) {
                val event = MarkerDetectionEvent(
                    markerId = known.markerId,
                    entranceNodeId = known.nearestNodeId,
                    markerBuildingX = known.buildingX,
                    markerBuildingY = known.buildingY,
                    markerBuildingZ = known.buildingZ,
                    markerArX = arX,
                    markerArY = arY,
                    markerArZ = arZ,
                    markerArRotationYDeg = arRotationYDeg,
                    markerBuildingRotationYDeg = known.buildingRotationYDeg,
                    confidence = 1.0,
                    role = known.role,
                )

                when (known.role) {
                    MarkerDetectionRole.ENTRANCE -> {
                        if (!hasDetectedMarker) {
                            hasDetectedMarker = true
                            println("[MarkerDetector] Entrance marker detected at AR ($arX, $arY, $arZ)")
                            onMarkerDetected?.invoke(event)
                        }
                    }
                    MarkerDetectionRole.CHECKPOINT -> {
                        println("[MarkerDetector] Checkpoint marker '${known.markerId}' at AR ($arX, $arY, $arZ)")
                        onCheckpointDetected?.invoke(event)
                    }
                }
                continue
            }

            // Fallback: first unregistered image treated as entrance (backward compat)
            if (!hasDetectedMarker) {
                hasDetectedMarker = true

                val event = MarkerDetectionEvent(
                    markerId = markerId,
                    entranceNodeId = markerNearestNodeId,
                    markerBuildingX = markerBuildingX,
                    markerBuildingY = markerBuildingY,
                    markerBuildingZ = markerBuildingZ,
                    markerArX = arX,
                    markerArY = arY,
                    markerArZ = arZ,
                    markerArRotationYDeg = arRotationYDeg,
                    markerBuildingRotationYDeg = markerBuildingRotationYDeg,
                    confidence = 1.0,
                    role = MarkerDetectionRole.ENTRANCE,
                )

                println("[MarkerDetector] Marker detected at AR ($arX, $arY, $arZ)")
                onMarkerDetected?.invoke(event)
            }
        }
    }

    fun reset() {
        hasDetectedMarker = false
    }

    fun fullReset() {
        hasDetectedMarker = false
        knownMarkersByIndex.clear()
    }
}

/**
 * Marker detection event (mirrors MarkerAlignmentResult in shared code).
 */
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
