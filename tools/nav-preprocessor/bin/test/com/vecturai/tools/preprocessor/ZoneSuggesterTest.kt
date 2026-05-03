package com.Vectura AI.tools.preprocessor

import com.Vectura AI.tools.preprocessor.analysis.OccupancyGridGenerator
import com.Vectura AI.tools.preprocessor.analysis.ZoneSuggester
import kotlin.test.*

/**
 * Tests for [ZoneSuggester] — connected-component labeling of walkable zones.
 */
class ZoneSuggesterTest {

    private val suggester = ZoneSuggester(minCellCount = 2)

    private fun makeGrid(
        cells: Array<IntArray>,
        originX: Double = 0.0,
        originZ: Double = 0.0,
        cellSize: Double = 1.0,
    ): OccupancyGridGenerator.OccupancyGrid {
        val height = cells.size
        val width = if (height > 0) cells[0].size else 0
        var occupied = 0
        for (row in cells) for (c in row) if (c == 1) occupied++
        return OccupancyGridGenerator.OccupancyGrid(
            cells = cells, originX = originX, originZ = originZ,
            cellSize = cellSize, width = width, height = height,
            occupiedCount = occupied,
        )
    }

    @Test
    fun `single connected region produces one zone`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
        ))
        val zones = suggester.suggest(grid)
        assertEquals(1, zones.size)
        assertEquals("Zone A", zones[0].label)
        assertEquals(6, zones[0].cellCount)
    }

    @Test
    fun `two disconnected regions produce two zones`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 0, 1, 1),
            intArrayOf(1, 1, 0, 1, 1),
        ))
        val zones = suggester.suggest(grid)
        assertEquals(2, zones.size)
        assertEquals("Zone A", zones[0].label)
        assertEquals("Zone B", zones[1].label)
    }

    @Test
    fun `small clusters are filtered as noise`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 0, 0, 0, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 1, 1, 1),
        ))
        val zones = suggester.suggest(grid)
        // The single cell on left should be filtered (< minCellCount=2)
        assertEquals(1, zones.size)
        assertEquals(6, zones[0].cellCount)
    }

    @Test
    fun `empty grid produces no zones`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(0, 0, 0),
            intArrayOf(0, 0, 0),
        ))
        val zones = suggester.suggest(grid)
        assertTrue(zones.isEmpty())
    }

    @Test
    fun `zones sorted by size descending`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 0, 1, 1, 1),
            intArrayOf(0, 0, 0, 1, 1, 1),
        ))
        val zones = suggester.suggest(grid)
        assertEquals(2, zones.size)
        assertTrue(zones[0].cellCount >= zones[1].cellCount)
    }

    @Test
    fun `zone centroids are in world coordinates`() {
        val grid = makeGrid(
            cells = arrayOf(
                intArrayOf(1, 1),
                intArrayOf(1, 1),
            ),
            originX = 10.0,
            originZ = 20.0,
            cellSize = 2.0,
        )
        val zones = suggester.suggest(grid)
        assertEquals(1, zones.size)
        // Centroid should be near (12, 22) since origin is (10,20) and cells span 2x2 at 2m
        assertTrue(zones[0].centroidX > 10.0)
        assertTrue(zones[0].centroidZ > 20.0)
    }

    @Test
    fun `confidence is always low`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 1),
            intArrayOf(1, 1, 1),
        ))
        val zones = suggester.suggest(grid)
        assertTrue(zones.all { it.confidence == "low" })
    }
}
