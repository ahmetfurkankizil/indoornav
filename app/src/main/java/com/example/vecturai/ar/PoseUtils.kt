package com.example.vecturai.ar

import com.example.vecturai.graph.MapNode
import com.google.ar.core.Pose
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin

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
        return if (len == 0f) Vec3(0f, 0f, -1f) else Vec3(x / len, y / len, z / len)
    }
}

fun Pose.translationVec(): Vec3 = Vec3(tx(), ty(), tz())

fun Pose.forwardVec(): Vec3 {
    val zAxis = getZAxis()
    return Vec3(-zAxis[0], -zAxis[1], -zAxis[2]).normalized()
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

fun yawDegreesToward(from: Vec3, to: Vec3): Float {
    val direction = (to - from).horizontal().normalized()
    return Math.toDegrees(atan2(direction.x.toDouble(), direction.z.toDouble())).toFloat()
}

fun poseAt(position: Vec3): Pose = Pose.makeTranslation(position.x, position.y, position.z)

fun positionInFrontOfCamera(cameraPose: Pose, meters: Float): Vec3 =
    cameraPose.translationVec() + cameraPose.forwardVec() * meters

fun Vec3.rotateY(radians: Float): Vec3 =
    Vec3(
        x = x * cos(radians) + z * sin(radians),
        y = y,
        z = -x * sin(radians) + z * cos(radians)
    )
