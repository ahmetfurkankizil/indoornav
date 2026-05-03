# File Dossier: ZoneSuggester.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/analysis/ZoneSuggester.kt`
- **Type**: Kotlin Source (Analysis Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Identifies distinct spatial zones (rooms, hallways) within the occupancy grid using connected-component labeling.

## Public Surface
- `suggest(grid: OccupancyGrid): List<Zone>`: Discovers all connected components and filters out small noise clusters.

## Main Symbols
- `Zone`: A cluster of cells with a unique ID, neutral label (e.g., "Zone A"), and a calculated spatial centroid.

## Important Logic
- **BFS Flood-Fill** (L79-110): A standard breadth-first search implementation to find all connected occupied cells starting from an unvisited seed cell.
- **Noise Filtering** (L43): Discards components smaller than 4 cells (`minCellCount`) to ignore outliers.
- **Centroid Calculation** (L58-66): Computes the geometric average of all cell centers in the zone to find its focal point for node placement.

## Used By
- `DraftPipeline.kt`: Step 5 of the extraction flow.
- `NavigationGraphDrafter.kt`: Uses zones as the basis for the navigation graph.

## Notes / Risks
- **Neutral Labels**: This stage has no semantic understanding; labels are purely alphabetical (A, B, C) and require human renaming in the authoring tool.
- **Connectivity**: If a doorway is blocked by a 1-cell gap in the occupancy grid, it will result in two separate disconnected zones.
