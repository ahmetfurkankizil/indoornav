package com.example.vecturai.ar

import com.google.ar.core.Pose

data class ArrowPose(
    val position: Vec3,
    val yawDegrees: Float,
    val distanceToTargetMeters: Float
)

object ArrowRenderer {
    fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose {
        val position = positionInFrontOfCamera(cameraPose, ARROW_FORWARD_OFFSET_M)
        return ArrowPose(
            position = position,
            yawDegrees = yawDegreesToward(cameraPose.translationVec(), target),
            distanceToTargetMeters = horizontalDistanceMeters(cameraPose.translationVec(), target)
        )
    }

    const val ARROW_FORWARD_OFFSET_M = 1.5f
    const val WAYPOINT_ADVANCE_DISTANCE_M = 1.2f
}
