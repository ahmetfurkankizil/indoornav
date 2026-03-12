package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.analysis.FloorPlaneEstimator
import com.vecturai.tools.preprocessor.glb.Vec3
import kotlin.test.*

/**
 * Tests for [FloorPlaneEstimator] — floor Y detection from vertex cloud.
 */
class FloorPlaneEstimatorTest {

    private val estimator = FloorPlaneEstimator()

    @Test
    fun `flat floor at Y=0`() {
        // 100 vertices at Y=0 (floor) + 20 at Y=2.5 (ceiling)
        val vertices = (1..100).map { Vec3(it * 0.1f, 0f, it * 0.05f) } +
            (1..20).map { Vec3(it * 0.1f, 2.5f, it * 0.05f) }

        val result = estimator.estimate(vertices)
        assertNotNull(result)
        assertEquals(0.0, result.floorY, 0.1)
        assertTrue(result.confidence > 0.5, "Confidence should be high for dominant floor")
        assertTrue(result.floorVertexCount >= 100)
    }

    @Test
    fun `floor at non-zero Y`() {
        // Floor at Y = -1.5
        val vertices = (1..80).map { Vec3(it * 0.1f, -1.5f, it * 0.05f) } +
            (1..30).map { Vec3(it * 0.1f, 1.0f, it * 0.05f) }

        val result = estimator.estimate(vertices)
        assertNotNull(result)
        assertEquals(-1.5, result.floorY, 0.15)
    }

    @Test
    fun `returns null for too few vertices`() {
        val vertices = listOf(Vec3(0f, 0f, 0f), Vec3(1f, 0f, 1f))
        val result = estimator.estimate(vertices)
        assertNull(result)
    }

    @Test
    fun `all same height gets confidence 1`() {
        val vertices = (1..50).map { Vec3(it * 0.1f, 0f, it * 0.1f) }
        val result = estimator.estimate(vertices)
        assertNotNull(result)
        assertEquals(1.0, result.confidence, 0.01)
    }

    @Test
    fun `floor vertices are within tolerance band`() {
        val vertices = (1..100).map { Vec3(it * 0.1f, 0f, 0f) } +
            (1..50).map { Vec3(it * 0.1f, 3f, 0f) }

        val result = estimator.estimate(vertices)
        assertNotNull(result)
        // All floor vertices should have Y near 0
        for (v in result.floorVertices) {
            assertTrue(kotlin.math.abs(v.y.toDouble() - result.floorY) <= 0.15)
        }
    }
}
