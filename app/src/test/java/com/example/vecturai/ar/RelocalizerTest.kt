package com.example.vecturai.ar

import com.google.ar.core.Pose
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin
import kotlin.math.cos

class RelocalizerTest {
    @Test
    fun fitGraphToSessionMapsMultipleCorrespondences() {
        val transform = Pose(
            floatArrayOf(1.5f, 0.2f, -2f),
            floatArrayOf(0f, sin(Math.PI / 8.0).toFloat(), 0f, cos(Math.PI / 8.0).toFloat())
        )
        val graphPoses = listOf(
            Pose.makeTranslation(0f, 0f, 0f),
            Pose.makeTranslation(2f, 0f, 0f),
            Pose.makeTranslation(0f, 0f, 2f),
            Pose.makeTranslation(2f, 0f, 2f)
        )
        val correspondences = graphPoses.map {
            Correspondence(graphPose = it, sessionPose = transform.compose(it))
        }

        val fit = requireNotNull(Relocalizer.fitGraphToSession(correspondences))

        correspondences.forEach { c ->
            val residual = distanceMeters(fit.compose(c.graphPose), c.sessionPose)
            assertTrue(
                "residual=$residual fit=(${fit.tx()}, ${fit.ty()}, ${fit.tz()}, ${fit.qx()}, ${fit.qy()}, ${fit.qz()}, ${fit.qw()})",
                residual < 0.05f
            )
        }
    }

    @Test
    fun rejectOutliersDropsLargeResiduals() {
        val transform = Pose.makeTranslation(1f, 0f, 0f)
        val good = Correspondence(
            graphPose = Pose.makeTranslation(0f, 0f, 0f),
            sessionPose = Pose.makeTranslation(1f, 0f, 0f)
        )
        val bad = Correspondence(
            graphPose = Pose.makeTranslation(0f, 0f, 0f),
            sessionPose = Pose.makeTranslation(5f, 0f, 0f)
        )

        assertTrue(Relocalizer.rejectOutliers(listOf(good, bad), transform, maxResidualMeters = 1f) == listOf(good))
    }
}
