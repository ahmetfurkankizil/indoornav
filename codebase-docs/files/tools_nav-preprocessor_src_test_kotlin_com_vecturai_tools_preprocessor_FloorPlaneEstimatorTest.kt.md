# File Dossier: FloorPlaneEstimatorTest.kt

## Path
`tools\nav-preprocessor\src\test\kotlin\com\Vectura AI\tools\preprocessor\FloorPlaneEstimatorTest.kt`

## Type
Unit/Integration Test

## Role
Unit/Integration Test for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.analysis.FloorPlaneEstimator
import com.Vectura AI.tools.preprocessor.glb.Vec3
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
        val vertices = (1..80).map { Vec3(it * 0.1f, -1.5f, it *
```

## Status
Mapped (Pass 3 Normalization)
