# File Dossier: ZoneSuggesterTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/analysis/ZoneSuggesterTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the connected-component labeling algorithm used to identify distinct walkable rooms/corridors (zones) from an occupancy grid. It ensures that spatially separated regions are correctly segmented, small noise clusters are filtered out, and zone metadata (labels, centroids, cell counts) is calculated accurately.

## Public Surface
- `ZoneSuggesterTest`: Test class.

## Main Symbols
- `suggest()`: The primary method under test.
- `makeGrid()`: Helper to create a test occupancy grid.

## Important Logic
- **Connected Component Analysis** (L32-53): Validates that contiguous blocks of occupied cells result in a single zone, while disjoint blocks result in multiple distinct zones.
- **Noise Filtering** (L56-65): Confirms that clusters with fewer than `minCellCount` (e.g., 2) are discarded as noise.
- **Centroid Calculation** (L89-104): Verifies that the geometric center of each zone is correctly transformed back into world coordinates, accounting for grid origin and cell size.
- **Result Ordering** (L78-86): Ensures that discovered zones are returned in descending order of size (cell count), which allows the generator to prioritize larger areas (likely corridors) for automated labeling.
- **Placeholders** (L107-114): Re-confirms that currently all automatically suggested zones are marked with "low" confidence, requiring human verification in the admin tool.

## Uses
- `ZoneSuggester`: The component being tested.
- `OccupancyGridGenerator.OccupancyGrid`: Input data structure.

## Related Features
- `preprocessing`: This is the core semantic analysis step of the preprocessor.

## Notes / Risks
- **Diagonal Connectivity**: The test confirms whether 4-way or 8-way connectivity is used (based on which cells are grouped).
- **Labeling Limitation**: The current system uses generic labels like "Zone A". More advanced semantic room discovery is a pending improvement.
