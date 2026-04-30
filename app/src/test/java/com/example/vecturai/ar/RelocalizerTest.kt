package com.example.vecturai.ar

import com.google.ar.core.Pose
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin
import kotlin.math.cos

class RelocalizerTest {
    @Test
    fun fitGraphToSessionMapsTwoAnchorIdentity() {
        val correspondences = listOf(
            Correspondence(
                graphPose = Pose.makeTranslation(0f, 0f, 0f),
                sessionPose = Pose.makeTranslation(1f, 0.2f, -1f)
            ),
            Correspondence(
                graphPose = Pose.makeTranslation(2f, 0f, 0f),
                sessionPose = Pose.makeTranslation(3f, 0.2f, -1f)
            )
        )

        val fit = requireNotNull(Relocalizer.fitGraphToSession(correspondences))

        correspondences.forEach { c ->
            val residual = distanceMeters(fit.compose(c.graphPose), c.sessionPose)
            assertTrue("residual=$residual", residual < 0.001f)
        }
    }

    @Test
    fun fitGraphToSessionMapsTwoAnchorNinetyDegreeYaw() {
        val transform = Pose(
            floatArrayOf(0.5f, 0.1f, 2f),
            floatArrayOf(0f, sin(Math.PI / 4.0).toFloat(), 0f, cos(Math.PI / 4.0).toFloat())
        )
        val graphPoses = listOf(
            Pose.makeTranslation(0f, 0f, 0f),
            Pose.makeTranslation(2f, 0f, 0f)
        )
        val correspondences = graphPoses.map {
            Correspondence(graphPose = it, sessionPose = transform.compose(it))
        }

        val fit = requireNotNull(Relocalizer.fitGraphToSession(correspondences))

        correspondences.forEach { c ->
            val residual = distanceMeters(fit.compose(c.graphPose), c.sessionPose)
            assertTrue("residual=$residual", residual < 0.001f)
        }
    }

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
    fun fitGraphToSessionStaysStableWithSmallNoisyResiduals() {
        val transform = Pose(
            floatArrayOf(-0.4f, 0.3f, 1.2f),
            floatArrayOf(0f, sin(Math.PI / 12.0).toFloat(), 0f, cos(Math.PI / 12.0).toFloat())
        )
        val graphPoses = listOf(
            Pose.makeTranslation(0f, 0f, 0f),
            Pose.makeTranslation(3f, 0f, 0f),
            Pose.makeTranslation(0f, 0f, 3f),
            Pose.makeTranslation(3f, 0f, 3f)
        )
        val noise = listOf(
            floatArrayOf(0.02f, 0f, -0.01f),
            floatArrayOf(-0.01f, 0.01f, 0.02f),
            floatArrayOf(0.01f, -0.01f, 0.01f),
            floatArrayOf(-0.02f, 0f, -0.02f)
        )
        val correspondences = graphPoses.mapIndexed { index, graphPose ->
            val sessionPose = transform.compose(graphPose)
            val n = noise[index]
            Correspondence(
                graphPose = graphPose,
                sessionPose = Pose.makeTranslation(
                    sessionPose.tx() + n[0],
                    sessionPose.ty() + n[1],
                    sessionPose.tz() + n[2]
                )
            )
        }

        val fit = requireNotNull(Relocalizer.fitGraphToSession(correspondences))
        val cleanCorrespondences = graphPoses.map {
            Correspondence(graphPose = it, sessionPose = transform.compose(it))
        }

        cleanCorrespondences.forEach { c ->
            val residual = distanceMeters(fit.compose(c.graphPose), c.sessionPose)
            assertTrue("residual=$residual", residual < 0.05f)
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
