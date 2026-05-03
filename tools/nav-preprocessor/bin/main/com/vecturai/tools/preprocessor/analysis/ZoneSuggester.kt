package com.Vectura AI.tools.preprocessor.analysis

/**
 * Discovers walkable zones by connected-component labeling of the occupancy grid.
 *
 * Uses flood-fill (BFS) to find connected clusters of OCCUPIED cells.
 * Small clusters below a threshold are filtered as noise.
 * Zones receive neutral labels: "Zone A", "Zone B", etc.
 */
class ZoneSuggester(
    /** Minimum number of cells for a zone to be kept (rejects noise). */
    private val minCellCount: Int = 4,
) {

    data class Zone(
        val id: String,
        val label: String,
        /** Number of occupied cells in this zone. */
        val cellCount: Int,
        /** Centroid X in world coordinates. */
        val centroidX: Double,
        /** Centroid Z in world coordinates. */
        val centroidZ: Double,
        /** All cells belonging to this zone as (col, row) pairs. */
        val cells: List<Pair<Int, Int>>,
        /** Confidence is always "low" — no semantic understanding. */
        val confidence: String = "low",
    )

    /**
     * Identify zones from the occupancy grid.
     *
     * @return List of zones sorted by cell count (largest first), or empty list
     */
    fun suggest(grid: OccupancyGridGenerator.OccupancyGrid): List<Zone> {
        val visited = Array(grid.height) { BooleanArray(grid.width) }
        val components = mutableListOf<List<Pair<Int, Int>>>()

        for (row in 0 until grid.height) {
            for (col in 0 until grid.width) {
                if (grid.cells[row][col] == OccupancyGridGenerator.OCCUPIED && !visited[row][col]) {
                    val component = bfs(grid, visited, col, row)
                    if (component.size >= minCellCount) {
                        components.add(component)
                    }
                }
            }
        }

        // Sort by size descending (largest zone first)
        components.sortByDescending { it.size }

        return components.mapIndexed { index, cells ->
            val label = "Zone ${('A' + index).toChar()}"
            val id = "zone-${('a' + index).toChar()}"

            // Compute centroid in world coordinates
            var sumX = 0.0
            var sumZ = 0.0
            for ((col, row) in cells) {
                val (wx, wz) = grid.cellToWorld(col, row)
                sumX += wx
                sumZ += wz
            }
            val centroidX = sumX / cells.size
            val centroidZ = sumZ / cells.size

            Zone(
                id = id,
                label = label,
                cellCount = cells.size,
                centroidX = centroidX,
                centroidZ = centroidZ,
                cells = cells,
            )
        }
    }

    private fun bfs(
        grid: OccupancyGridGenerator.OccupancyGrid,
        visited: Array<BooleanArray>,
        startCol: Int,
        startRow: Int,
    ): List<Pair<Int, Int>> {
        val component = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(startCol, startRow))
        visited[startRow][startCol] = true

        val dx = intArrayOf(0, 0, 1, -1)
        val dz = intArrayOf(1, -1, 0, 0)

        while (queue.isNotEmpty()) {
            val (col, row) = queue.removeFirst()
            component.add(Pair(col, row))

            for (d in 0..3) {
                val nc = col + dx[d]
                val nr = row + dz[d]
                if (nc in 0 until grid.width && nr in 0 until grid.height &&
                    !visited[nr][nc] && grid.cells[nr][nc] == OccupancyGridGenerator.OCCUPIED
                ) {
                    visited[nr][nc] = true
                    queue.add(Pair(nc, nr))
                }
            }
        }

        return component
    }
}
