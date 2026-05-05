# File Dossier: NavigationGraphDrafterTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/NavigationGraphDrafterTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the heuristic-based navigation graph drafting logic. It ensures that the `NavigationGraphDrafter` correctly places nodes at zone centroids and establishes bidirectional edges between spatially adjacent zones based on a distance threshold.

## Public Surface
- `NavigationGraphDrafterTest`: Test class.

## Main Symbols
- `draft()`: The primary method under test.
- `makeGrid()`: Helper to create a test occupancy grid.

## Important Logic
- **Zone-to-Node Mapping** (L33-43): Verifies that a discovered zone results in at least one navigation node.
- **Adjacency & Connectivity** (L46-58): Tests the core heuristic that connects two distinct zones if they are within the `adjacencyThreshold`.
- **Edge Cost Calculation** (L69-83): Confirms that edges are assigned positive costs based on Euclidean distance and are correctly marked as bidirectional.
- **Uniqueness Constraints** (L86-114): Ensures that the auto-generated node and edge IDs are unique across the entire draft graph.
- **Empty State** (L61-66): Validates that an empty set of zones results in an empty graph without crashing.

## Uses
- `NavigationGraphDrafter`: The component being tested.
- `OccupancyGridGenerator.OccupancyGrid`: Input data structure.
- `ZoneSuggester.Zone`: Input data structure.

## Related Features
- `preprocessing`: This is the bridge between spatial analysis (grids/zones) and navigation data (graphs).

## Notes / Risks
- **Heuristic Sensitivity**: The drafting logic relies heavily on the `adjacencyThreshold` (default 10m). If zones are separated by more than this distance in the test grid, edges will not be created.
