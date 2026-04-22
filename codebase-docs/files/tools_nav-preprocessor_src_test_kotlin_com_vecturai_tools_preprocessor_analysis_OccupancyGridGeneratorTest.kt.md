# File Dossier: OccupancyGridGeneratorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/vecturai/tools/preprocessor/analysis/OccupancyGridGeneratorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the generation of a 2D occupancy grid from a 3D vertex cloud. It ensures that spatial data is correctly projected onto a discrete XZ grid, that grid dimensions accurately reflect the input extent, and that coordinate transformations between grid-space and world-space are numerically sound.

## Public Surface
- `OccupancyGridGeneratorTest`: Test class.

## Main Symbols
- `generate()`: The primary method under test.
- `cellToWorld()`: Coordinate transformation method.

## Important Logic
- **Spatial Projection** (L15-20): Verifies that a single 3D vertex correctly triggers the occupancy of its corresponding 2D grid cell.
- **Bounding Box Calculation** (L29-40): Confirms that the grid size (width/height) is dynamically calculated to encompass the entire extent of the input vertex set with appropriate padding.
- **Density Accumulation** (L43-54): Validates that a dense cluster of vertices results in a logically consistent `occupiedCount` in the resulting grid.
- **Coordinate Mapping** (L57-65): Ensures that `cellToWorld` correctly identifies the world-space center of a given grid cell, accounting for the grid's origin and cell size.
- **Empty Handling** (L23-26): Gracefully handles empty input by returning `null`.

## Uses
- `OccupancyGridGenerator`: The component being tested.
- `Vec3`: Input data structure.

## Related Features
- `preprocessing`: Fundamental step in converting unstructured scans into structured navigation maps.

## Notes / Risks
- **Resolution Sensitivity**: The test uses a `cellSize` of 1.0m. In production, this is typically much smaller (e.g., 0.1m) for finer detail. The test confirms the *logic* of the tiling, not the specific resolution.
