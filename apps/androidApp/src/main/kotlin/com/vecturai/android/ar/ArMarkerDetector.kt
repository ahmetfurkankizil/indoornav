package com.vecturai.android.ar

import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import kotlin.math.atan2

/**
 * Detects entrance marker augmented images and reports alignment events.
 *
 * Watches ARCore frames for AugmentedImage tracking updates
 * and reports the first stable detection.
 */
class ArMarkerDetector {

    var markerId: String = ""
    var markerNearestNodeId: String = ""
    var markerBuildingX: Double = 0.0
    var markerBuildingY: Double = 0.0
    var markerBuildingZ: Double = 0.0
    var markerBuildingRotationYDeg: Double = 0.0

    var hasDetectedMarker: Boolean = false
        private set

    var onMarkerDetected: ((MarkerDetectionEvent) -> Unit)? = null

    /**
     * Configure the detector with entrance marker metadata.
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
     * Process an ARCore frame, checking for augmented image detections.
     * Call this each frame from the render loop.
     */
    fun processFrame(frame: Frame) {
        if (hasDetectedMarker) return

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
            )

            println("[MarkerDetector] Marker detected at AR ($arX, $arY, $arZ)")
            onMarkerDetected?.invoke(event)
            break
        }
    }

    fun reset() {
        hasDetectedMarker = false
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
)
