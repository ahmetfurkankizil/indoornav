package com.example.vecturai.ar

import com.google.ar.core.Pose
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

        var sxx = 0f
        var sxz = 0f
        var szx = 0f
        var szz = 0f
        correspondences.forEach { c ->
            val w = c.weight.coerceAtLeast(0f) / totalWeight
            val px = c.graphPose.tx() - gx
            val pz = c.graphPose.tz() - gz
            val qx = c.sessionPose.tx() - sx
            val qz = c.sessionPose.tz() - sz
            sxx += px * qx * w
            sxz += px * qz * w
            szx += pz * qx * w
            szz += pz * qz * w
        }

        val theta = atan2((szx - sxz).toDouble(), (sxx + szz).toDouble()).toFloat()
        val c = cos(theta)
        val s = sin(theta)
        val translation = floatArrayOf(
            sx - (c * gx + s * gz),
            sy - gy,
            sz - (-s * gx + c * gz)
        )
        val rotation = floatArrayOf(0f, sin(theta * 0.5f), 0f, cos(theta * 0.5f))
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

    private const val EPSILON = 1e-6f
}
