# File Dossier: ZoneSuggesterTest.kt

## Path
`tools\nav-preprocessor\src\test\kotlin\com\VecturAI\tools\preprocessor\ZoneSuggesterTest.kt`

## Type
Unit/Integration Test

## Role
Unit/Integration Test for the tools component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
package com.VecturAI.tools.preprocessor

import com.VecturAI.tools.preprocessor.analysis.OccupancyGridGenerator
import com.VecturAI.tools.preprocessor.analysis.ZoneSuggester
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
    
```

## Status
Mapped (Pass 3 Normalization)
