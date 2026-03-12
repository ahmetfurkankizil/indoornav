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
        val grid = generator.generate(vertices)
        assertNotNull(grid)
        // Width should cover 0..5 with 1m cells + padding
        assertTrue(grid.width >= 5)
        assertTrue(grid.height >= 3)
    }

    @Test
    fun `dense grid has high occupied count`() {
        // Fill a 3x3 area densely
        val vertices = mutableListOf<Vec3>()
        for (x in 0..30) {
            for (z in 0..30) {
                vertices.add(Vec3(x * 0.1f, 0f, z * 0.1f))
            }
        }
        val grid = generator.generate(vertices)
        assertNotNull(grid)
        assertTrue(grid.occupiedCount >= 9, "Expected at least 9 occupied cells for a 3x3m area")
    }

    @Test
    fun `cellToWorld returns center of cell`() {
        val vertices = listOf(Vec3(0f, 0f, 0f), Vec3(10f, 0f, 10f))
        val grid = generator.generate(vertices)
        assertNotNull(grid)
        val (wx, wz) = grid.cellToWorld(0, 0)
        // Should be at origin + half cell size
        assertTrue(wx > grid.originX)
        assertTrue(wz > grid.originZ)
    }
}
