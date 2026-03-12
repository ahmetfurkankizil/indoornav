package com.vecturai.tools.preprocessor.analysis

import com.vecturai.tools.preprocessor.glb.Vec3
import kotlin.math.floor

/**
 * Generates a 2D occupancy grid by projecting floor-level vertices
 * onto the XZ plane.
 *
 * Each cell in the grid is either OCCUPIED (at least one vertex projects
 * into it) or EMPTY. A morphological closing pass (dilate then erode)
 * fills small gaps caused by scan noise.
 */
class OccupancyGridGenerator(
    val cellSize: Double = 0.25,
) {

    companion object {
        const val EMPTY = 0
        const val OCCUPIED = 1
    }

    data class OccupancyGrid(
        /** 2D grid: cells[row][col], row = Z-axis, col = X-axis. */
        val cells: Array<IntArray>,
        /** World X coordinate of column 0. */
        val originX: Double,
        /** World Z coordinate of row 0. */
        val originZ: Double,
        val cellSize: Double,
        /** Number of columns (X). */
        val width: Int,
        /** Number of rows (Z). */
        val height: Int,
        /** Total number of occupied cells. */
        val occupiedCount: Int,
    ) {
        /** Convert grid coordinates to world coordinates (cell center). */
        fun cellToWorld(col: Int, row: Int): Pair<Double, Double> {
            val worldX = originX + (col + 0.5) * cellSize
            val worldZ = originZ + (row + 0.5) * cellSize
            return Pair(worldX, worldZ)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is OccupancyGrid) return false
            return originX == other.originX && originZ == other.originZ &&
                cellSize == other.cellSize && width == other.width && height == other.height &&
                cells.contentDeepEquals(other.cells)
        }

        override fun hashCode(): Int = cells.contentDeepHashCode()
    }

    /**
     * Generate an occupancy grid from floor-level vertices.
     *
     * @param floorVertices Vertices classified as floor-level (Y values ignored for projection)
     * @return OccupancyGrid or null if insufficient data
     */
    fun generate(floorVertices: List<Vec3>): OccupancyGrid? {
        if (floorVertices.isEmpty()) return null

        // Compute XZ bounds
        val xs = floorVertices.map { it.x.toDouble() }
        val zs = floorVertices.map { it.z.toDouble() }
        val minX = xs.min()
        val maxX = xs.max()
        val minZ = zs.min()
        val maxZ = zs.max()

        // Add 1-cell padding around the edges
        val originX = minX - cellSize
        val originZ = minZ - cellSize
        val width = (floor((maxX - originX) / cellSize).toInt() + 2).coerceAtLeast(1)
        val height = (floor((maxZ - originZ) / cellSize).toInt() + 2).coerceAtLeast(1)

        val grid = Array(height) { IntArray(width) }

        // Mark occupied cells
        for (v in floorVertices) {
            val col = floor((v.x.toDouble() - originX) / cellSize).toInt()
            val row = floor((v.z.toDouble() - originZ) / cellSize).toInt()
            if (col in 0 until width && row in 0 until height) {
                grid[row][col] = OCCUPIED
            }
        }

        // Gap-filling: keep all original cells, then add cells that have
        // at least 2 occupied neighbors (bridges small gaps from scan noise)
        val filled = fillGaps(grid, width, height)

        var occupiedCount = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (filled[row][col] == OCCUPIED) occupiedCount++
            }
        }

        return OccupancyGrid(
            cells = filled,
            originX = originX,
            originZ = originZ,
            cellSize = cellSize,
            width = width,
            height = height,
            occupiedCount = occupiedCount,
        )
    }

    /**
     * Fill small gaps: keep all original occupied cells, then mark empty cells
     * that have at least 2 occupied 4-connected neighbors as occupied.
     * This bridges 1-cell holes without expanding the boundary.
     */
    private fun fillGaps(grid: Array<IntArray>, w: Int, h: Int): Array<IntArray> {
        val result = Array(h) { row -> grid[row].copyOf() }
        for (row in 0 until h) {
            for (col in 0 until w) {
                if (grid[row][col] == EMPTY) {
                    var neighborCount = 0
                    if (row > 0 && grid[row - 1][col] == OCCUPIED) neighborCount++
                    if (row < h - 1 && grid[row + 1][col] == OCCUPIED) neighborCount++
                    if (col > 0 && grid[row][col - 1] == OCCUPIED) neighborCount++
                    if (col < w - 1 && grid[row][col + 1] == OCCUPIED) neighborCount++
                    if (neighborCount >= 2) {
                        result[row][col] = OCCUPIED
                    }
                }
            }
        }
        return result
    }
}
