package com.example.vecturai.ui.navigation

import kotlin.math.abs
import kotlin.math.sign

class FloorHeightEstimator(
    private val defaultCameraToFloorMeters: Float = DEFAULT_CAMERA_TO_FLOOR_M
) {
    private val estimatesByFloor = mutableMapOf<Int, FloorHeightEstimate>()

    fun reset() {
        estimatesByFloor.clear()
    }

    fun estimate(
        floor: Int,
        routeCameraHeightY: Float,
        cameraY: Float,
        detectedFloorY: Float?
    ): FloorHeightEstimate {
        val detectedIsPlausible = detectedFloorY?.let { isPlausibleFloorY(it, cameraY) } == true
        val rawFloorY = if (detectedIsPlausible) {
            detectedFloorY
        } else {
            routeCameraHeightY - defaultCameraToFloorMeters
        }
        val clampedFloorY = clampBelowCamera(rawFloorY, cameraY)
        val previous = estimatesByFloor[floor]
        val confidence = if (detectedIsPlausible) DETECTED_FLOOR_CONFIDENCE else FALLBACK_FLOOR_CONFIDENCE

        val nextY = if (previous == null) {
            clampedFloorY
        } else {
            val delta = clampedFloorY - previous.yMeters
            when {
                abs(delta) <= FLOOR_Y_DEADBAND_M -> previous.yMeters
                abs(delta) > MAX_FLOOR_Y_STEP_M -> previous.yMeters + sign(delta) * MAX_FLOOR_Y_STEP_M
                else -> previous.yMeters + delta * FLOOR_Y_SMOOTHING_ALPHA
            }
        }

        return FloorHeightEstimate(
            floor = floor,
            yMeters = nextY,
            confidence = confidence
        ).also { estimatesByFloor[floor] = it }
    }

    private fun isPlausibleFloorY(floorY: Float, cameraY: Float): Boolean {
        val cameraToFloor = cameraY - floorY
        return cameraToFloor in MIN_CAMERA_TO_FLOOR_M..MAX_CAMERA_TO_FLOOR_M
    }

    private fun clampBelowCamera(floorY: Float, cameraY: Float): Float =
        floorY.coerceIn(
            minimumValue = cameraY - MAX_CAMERA_TO_FLOOR_M,
            maximumValue = cameraY - MIN_CAMERA_TO_FLOOR_M
        )

    private companion object {
        const val MIN_CAMERA_TO_FLOOR_M = 0.6f
        const val MAX_CAMERA_TO_FLOOR_M = 2.2f
        const val FLOOR_Y_DEADBAND_M = 0.02f
        const val FLOOR_Y_SMOOTHING_ALPHA = 0.35f
        const val MAX_FLOOR_Y_STEP_M = 0.18f
        const val DETECTED_FLOOR_CONFIDENCE = 0.9f
        const val FALLBACK_FLOOR_CONFIDENCE = 0.35f
    }
}
