package com.example.vecturai.ar

import com.google.ar.core.Pose
import kotlin.math.sqrt

data class Correspondence(
    val graphPose: Pose,
    val sessionPose: Pose,
    val weight: Float = 1f
)

object Relocalizer {
    fun fitGraphToSession(correspondences: List<Correspondence>): Pose? {
        if (correspondences.isEmpty()) return null
        if (correspondences.size == 1) {
            val c = correspondences.first()
            return c.sessionPose.compose(c.graphPose.inverse())
        }

        val totalWeight = correspondences.sumOf { it.weight.coerceAtLeast(0f).toDouble() }
            .toFloat()
            .coerceAtLeast(EPSILON)

        var gx = 0f
        var gy = 0f
        var gz = 0f
        var sx = 0f
        var sy = 0f
        var sz = 0f
        correspondences.forEach { c ->
            val w = c.weight.coerceAtLeast(0f) / totalWeight
            gx += c.graphPose.tx() * w
            gy += c.graphPose.ty() * w
            gz += c.graphPose.tz() * w
            sx += c.sessionPose.tx() * w
            sy += c.sessionPose.ty() * w
            sz += c.sessionPose.tz() * w
        }

        val rotation = averageDirectRotation(correspondences, totalWeight)
        val rotatedGraphCentroid = Pose(floatArrayOf(0f, 0f, 0f), rotation)
            .compose(Pose.makeTranslation(gx, gy, gz))
            .translationVec()
        val translation = floatArrayOf(
            sx - rotatedGraphCentroid.x,
            sy - rotatedGraphCentroid.y,
            sz - rotatedGraphCentroid.z
        )
        return Pose(translation, rotation)
    }

    fun rejectOutliers(
        correspondences: List<Correspondence>,
        fit: Pose,
        maxResidualMeters: Float = 1.0f
    ): List<Correspondence> {
        return correspondences.filter { c ->
            val predicted = fit.compose(c.graphPose)
            distanceMeters(predicted, c.sessionPose) <= maxResidualMeters
        }
    }

    private fun averageDirectRotation(
        correspondences: List<Correspondence>,
        totalWeight: Float
    ): FloatArray {
        var qw = 0f
        var qx = 0f
        var qy = 0f
        var qz = 0f
        val reference = correspondences.first()
            .let { it.sessionPose.compose(it.graphPose.inverse()) }

        correspondences.forEach { c ->
            val w = c.weight.coerceAtLeast(0f) / totalWeight
            val transform = c.sessionPose.compose(c.graphPose.inverse())
            val dot = transform.qx() * reference.qx() +
                transform.qy() * reference.qy() +
                transform.qz() * reference.qz() +
                transform.qw() * reference.qw()
            val sign = if (dot < 0f) -1f else 1f
            qx += transform.qx() * sign * w
            qy += transform.qy() * sign * w
            qz += transform.qz() * sign * w
            qw += transform.qw() * sign * w
        }

        val normalized = normalizeQuaternion(floatArrayOf(qw, qx, qy, qz))
        return floatArrayOf(normalized[1], normalized[2], normalized[3], normalized[0])
    }

    private fun normalizeQuaternion(q: FloatArray): FloatArray {
        val length = sqrt(q.sumOf { (it * it).toDouble() }).toFloat()
        if (length < EPSILON) return floatArrayOf(1f, 0f, 0f, 0f)
        return floatArrayOf(q[0] / length, q[1] / length, q[2] / length, q[3] / length)
    }

    private const val EPSILON = 1e-6f
}
