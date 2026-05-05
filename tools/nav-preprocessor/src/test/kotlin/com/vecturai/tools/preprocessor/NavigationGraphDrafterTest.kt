package com.vecturai.tools.preprocessor

import com.vecturai.tools.preprocessor.analysis.OccupancyGridGenerator
import com.vecturai.tools.preprocessor.analysis.ZoneSuggester
import com.vecturai.tools.preprocessor.draft.NavigationGraphDrafter
import kotlin.test.*

/**
 * Tests for [NavigationGraphDrafter] — draft nav graph from zones.
 */
class NavigationGraphDrafterTest {

    private val drafter = NavigationGraphDrafter(adjacencyThreshold = 10.0, waypointSpacing = 3.0)

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
    fun `single zone produces at least one node`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1),
        ))
        val zones = listOf(
            ZoneSuggester.Zone("zone-a", "Zone A", 8, 2.0, 1.0, listOf())
        )
        val result = drafter.draft(zones, grid, 0.0)
        assertTrue(result.nodes.isNotEmpty())
    }

    @Test
    fun `two zones produce connecting edge`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 0, 1, 1),
            intArrayOf(1, 1, 0, 1, 1),
        ))
        val zones = listOf(
            ZoneSuggester.Zone("zone-a", "Zone A", 4, 1.0, 0.5, listOf()),
            ZoneSuggester.Zone("zone-b", "Zone B", 4, 4.0, 0.5, listOf()),
        )
        val result = drafter.draft(zones, grid, 0.0)
        assertTrue(result.nodes.size >= 2)
        assertTrue(result.edges.isNotEmpty(), "Should have edge connecting the two zones")
    }

    @Test
    fun `empty zones produce empty graph`() {
        val grid = makeGrid(arrayOf(intArrayOf(0)))
        val result = drafter.draft(emptyList(), grid, 0.0)
        assertTrue(result.nodes.isEmpty())
        assertTrue(result.edges.isEmpty())
    }

    @Test
    fun `edge costs are positive Euclidean distances`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 0, 1, 1),
            intArrayOf(1, 1, 0, 1, 1),
        ))
        val zones = listOf(
            ZoneSuggester.Zone("zone-a", "Zone A", 4, 0.5, 0.5, listOf()),
            ZoneSuggester.Zone("zone-b", "Zone B", 4, 3.5, 0.5, listOf()),
        )
        val result = drafter.draft(zones, grid, 0.0)
        for (edge in result.edges) {
            assertTrue(edge.cost > 0, "Edge cost should be positive")
            assertTrue(edge.bidirectional, "Edges should be bidirectional")
        }
    }

    @Test
    fun `node IDs are unique`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
        ))
        val zones = listOf(
            ZoneSuggester.Zone("zone-a", "Zone A", 8, 4.0, 1.0, listOf()),
            ZoneSuggester.Zone("zone-b", "Zone B", 4, 0.5, 0.5, listOf()),
            ZoneSuggester.Zone("zone-c", "Zone C", 4, 7.5, 0.5, listOf()),
        )
        val result = drafter.draft(zones, grid, 0.0)
        val ids = result.nodes.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Node IDs must be unique")
    }

    @Test
    fun `edge IDs are unique`() {
        val grid = makeGrid(arrayOf(
            intArrayOf(1, 1, 0, 1, 1),
            intArrayOf(1, 1, 0, 1, 1),
        ))
        val zones = listOf(
            ZoneSuggester.Zone("zone-a", "Zone A", 4, 1.0, 0.5, listOf()),
            ZoneSuggester.Zone("zone-b", "Zone B", 4, 4.0, 0.5, listOf()),
        )
        val result = drafter.draft(zones, grid, 0.0)
        val ids = result.edges.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Edge IDs must be unique")
    }
}
