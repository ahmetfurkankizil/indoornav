# File Dossier: OccupancyGridGeneratorTest.kt

## Path
`tools\nav-preprocessor\src\test\kotlin\com\vecturai\tools\preprocessor\OccupancyGridGeneratorTest.kt`

## Type
Unit/Integration Test

## Role
Unit/Integration Test for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator
import com.vecturai.tools.preprocessor.glb.Vec3
import kotlin.test.*

/**
 * Tests for [OccupancyGridGenerator] — 2D occupancy grid from floor vertices.
 */
class OccupancyGridGeneratorTest {

    private val generator = OccupancyGridGenerator(cellSize = 1.0)

    @Test
    fun `single vertex creates occupied cell`() {
        val vertices = listOf(Vec3(0.5f, 0f, 0.5f))
        val grid = generator.generate(vertices)
        assertNotNull(grid)
        assertTrue(grid.occupiedCount >= 1)
    }

    @Test
    fun `empty vertices returns null`() {
        val grid = generator.generate(emptyList())
        assertNull(grid)
    }

    @Test
    fun `grid dimensions match vertex extent`() {
        // Vertices spanning 0..5 in X and 0..3 in Z
        val vertices = listOf(
            Vec3(0f, 0f, 0f),
            Vec3(5f, 0f, 3f),
        )
        val grid = generator.generate(v
```

## Status
Mapped (Pass 3 Normalization)
