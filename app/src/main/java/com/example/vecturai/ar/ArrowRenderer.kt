package com.example.vecturai.ar

import com.google.ar.core.Pose

data class ArrowPose(
    val position: Vec3,
    val yawDegrees: Float,
    val userDistanceToTargetMeters: Float
)

object ArrowRenderer {
    fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose? {
        val position = positionInFrontOfCamera(cameraPose, ARROW_FORWARD_OFFSET_M)
        val yawDegrees = yawDegreesToward(position, target) ?: return null
        return ArrowPose(
            position = position,
            yawDegrees = yawDegrees,
            userDistanceToTargetMeters = horizontalDistanceMeters(cameraPose.translationVec(), target)
        )
    }

    const val ARROW_FORWARD_OFFSET_M = 1.5f
    const val ARROW_MODEL_YAW_OFFSET_DEG = 0f
    const val WAYPOINT_ADVANCE_DISTANCE_M = 1.2f
}
