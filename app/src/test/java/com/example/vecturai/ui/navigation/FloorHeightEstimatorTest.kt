package com.example.vecturai.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorHeightEstimatorTest {
    @Test
    fun deterministicFallbackSubtractsDefaultCameraToFloorHeight() {
        val estimator = FloorHeightEstimator()

        val estimate = estimator.estimate(
            floor = 0,
            routeCameraHeightY = 1.5f,
            cameraY = 1.5f,
            detectedFloorY = null
        )

        assertEquals(1.5f - DEFAULT_CAMERA_TO_FLOOR_M, estimate.yMeters, 0.001f)
        assertTrue(estimate.confidence < 0.5f)
    }

    @Test
    fun smoothedEstimateRejectsSuddenImplausibleJump() {
        val estimator = FloorHeightEstimator()
        estimator.estimate(
            floor = 0,
            routeCameraHeightY = 1.5f,
            cameraY = 1.5f,
            detectedFloorY = null
        )

        val jumped = estimator.estimate(
            floor = 0,
            routeCameraHeightY = 1.5f,
            cameraY = 1.5f,
            detectedFloorY = 0.8f
        )

        assertTrue(jumped.yMeters < 0.4f)
        assertTrue(jumped.confidence > 0.8f)
    }

    @Test
    fun resetAllowsNewBuildingEstimateToStartFresh() {
        val estimator = FloorHeightEstimator()
        estimator.estimate(
            floor = 0,
            routeCameraHeightY = 1.5f,
            cameraY = 1.5f,
            detectedFloorY = null
        )

        estimator.reset()

        val estimate = estimator.estimate(
            floor = 0,
            routeCameraHeightY = 1.5f,
            cameraY = 1.5f,
            detectedFloorY = 0.8f
        )

        assertEquals(0.8f, estimate.yMeters, 0.001f)
    }
}
