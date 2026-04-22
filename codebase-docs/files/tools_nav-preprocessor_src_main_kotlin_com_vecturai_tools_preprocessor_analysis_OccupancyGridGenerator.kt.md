# File Dossier: OccupancyGridGenerator.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/vecturai/tools/preprocessor/analysis/OccupancyGridGenerator.kt`
- **Type**: Kotlin Source (Analysis Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Generates a 2D representation of the walkable area by projecting floor-level vertices onto a 2D grid. It cleans up sensor noise to create a solid "floor plan" for navigation graph generation.

## Public Surface
- `generate(floorVertices: List<Vec3>): OccupancyGrid?`: Creates a grid with 25cm resolution (`cellSize`).

## Main Symbols
- `OccupancyGrid`: A 2D array of cells (OCCUPIED vs EMPTY) with world-coordinate mapping.

## Important Logic
- **XZ Projection** (L81-88): Maps 3D vertices to 2D grid coordinates based on the calculated floor bounds.
- **Morphological Gap Filling** (L117-133): A `fillGaps` pass that marks empty cells as occupied if they have 2 or more occupied neighbors. This bridges small holes caused by scan noise or missing floor data.
- **Coordinate Mapping** (L39-43): Provides `cellToWorld` utility to convert grid indices back to global 3D space.

## Used By
- `DraftPipeline.kt`: Step 4 of the extraction flow.
- `ZoneSuggester.kt`: Consumes the grid to find components.
- `NavigationGraphDrafter.kt`: Uses the grid for spatial reference.

## Notes / Risks
- **Resolution**: The 25cm resolution is optimized for indoor navigation. Finer resolution increases memory usage, while coarser resolution loses architectural detail.
- **Projection**: Only projects `floorVertices`; objects above the floor tolerance (like furniture or ceilings) are ignored in this stage.
