package com.example.vecturai.ar

import com.example.vecturai.graph.MapNode
import com.google.ar.core.Pose
import kotlin.math.atan2
import kotlin.math.sqrt

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)

    fun horizontal() = Vec3(x, 0f, z)

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length()
        return if (len < 1e-6f) Vec3(0f, 0f, 0f) else Vec3(x / len, y / len, z / len)
    }
}

fun Pose.translationVec(): Vec3 = Vec3(tx(), ty(), tz())

fun Pose.forwardVec(): Vec3? {
    val zAxis = getZAxis()
    val raw = Vec3(-zAxis[0], -zAxis[1], -zAxis[2])
    val len = raw.length()
    return if (len < 1e-4f) null else raw * (1f / len)
}

private fun Pose.topVec(): Vec3 {
    val yAxis = getYAxis()
    return Vec3(yAxis[0], yAxis[1], yAxis[2])
}

fun distanceMeters(a: Pose, b: Pose): Float = distanceMeters(a.translationVec(), b.translationVec())

fun distanceMeters(a: Vec3, b: Vec3): Float = (a - b).length()

fun horizontalDistanceMeters(a: Vec3, b: Vec3): Float = (a - b).horizontal().length()

fun relativePose(origin: Pose, pose: Pose): Pose = origin.inverse().compose(pose)

fun MapNode.withLabel(label: String?): MapNode = copy(label = label?.trim()?.ifBlank { null })

fun Pose.toMapNodePoseValues(): PoseValues {
    return PoseValues(
        x = tx(),
        y = ty(),
        z = tz(),
        qx = qx(),
        qy = qy(),
        qz = qz(),
        qw = qw()
    )
}

data class PoseValues(
    val x: Float,
    val y: Float,
    val z: Float,
    val qx: Float,
    val qy: Float,
    val qz: Float,
    val qw: Float
)

fun sessionFromGraphPose(resolvedNode: MapNode, resolvedSessionPose: Pose): Pose =
    resolvedSessionPose.compose(resolvedNode.graphPose().inverse())

fun estimateSessionPose(sessionFromGraph: Pose, node: MapNode): Pose =
    sessionFromGraph.compose(node.graphPose())

fun yawDegreesToward(from: Vec3, to: Vec3): Float? {
    val direction = (to - from).horizontal()
    if (direction.length() < 1e-3f) return null
    return Math.toDegrees(atan2(direction.x.toDouble(), direction.z.toDouble())).toFloat()
}

fun poseAt(position: Vec3): Pose = Pose.makeTranslation(position.x, position.y, position.z)

fun positionInFrontOfCamera(cameraPose: Pose, meters: Float): Vec3? {
    val camera = cameraPose.translationVec()
    val forward = cameraPose.forwardVec() ?: return null
    var flatX = forward.x
    var flatZ = forward.z
    var flatLen = sqrt(flatX * flatX + flatZ * flatZ)
    if (flatLen < 0.1f) {
        val top = cameraPose.topVec()
        flatX = top.x
        flatZ = top.z
        flatLen = sqrt(flatX * flatX + flatZ * flatZ)
        if (flatLen < 1e-4f) return null
    }
    val scale = meters / flatLen
    return Vec3(
        x = camera.x + flatX * scale,
        y = camera.y + ARROW_VERTICAL_OFFSET_M,
        z = camera.z + flatZ * scale
    )
}

const val ARROW_VERTICAL_OFFSET_M = -0.4f
