package com.example.vecturai.ar

import com.google.ar.core.Pose
import kotlin.math.atan2

data class ArrowPose(
    val position: Vec3,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val userDistanceToTargetMeters: Float
)

object ArrowRenderer {
    fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose? {
        val cameraVec = cameraPose.translationVec()
        val toTarget = horizontalDistanceMeters(cameraVec, target)
        val offset = minOf(ARROW_FORWARD_OFFSET_M, toTarget * 0.9f)
        val position = positionInFrontOfCamera(cameraPose, offset) ?: return null
        val yawDegrees = yawDegreesToward(position, target) ?: return null
        val verticalDelta = target.y - position.y
        val horizontalDelta = horizontalDistanceMeters(position, target).coerceAtLeast(1e-3f)
        val pitchDegrees = Math.toDegrees(
            atan2(verticalDelta.toDouble(), horizontalDelta.toDouble())
        ).toFloat().coerceIn(-MAX_ARROW_PITCH_DEG, MAX_ARROW_PITCH_DEG)
        return ArrowPose(
            position = position,
            yawDegrees = yawDegrees,
            pitchDegrees = pitchDegrees,
            userDistanceToTargetMeters = toTarget
        )
    }

    const val ARROW_FORWARD_OFFSET_M = 1.5f
    const val ARROW_MODEL_YAW_OFFSET_DEG = 0f
    const val WAYPOINT_ADVANCE_DISTANCE_M = 1.2f
    const val MAX_ARROW_PITCH_DEG = 60f
}
