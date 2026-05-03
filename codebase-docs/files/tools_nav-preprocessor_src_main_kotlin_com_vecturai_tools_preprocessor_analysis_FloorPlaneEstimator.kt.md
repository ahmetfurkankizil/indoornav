# File Dossier: FloorPlaneEstimator.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/analysis/FloorPlaneEstimator.kt`
- **Type**: Kotlin Source (Analysis Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Implements a heuristic for detecting the floor level in a point cloud of mesh vertices. This is crucial for isolating the walkable surface from walls and ceilings.

## Public Surface
- `estimate(vertices: List<Vec3>): FloorEstimate?`: Identifies the dominant Y-plane and returns the vertices residing on it.

## Main Symbols
- `FloorEstimate`: Contains the `floorY` value, a `confidence` metric, and the filtered `floorVertices`.

## Important Logic
- **Histogram Analysis** (L59-69): Discretizes the Y-axis into 5cm bins (`binWidth`) and counts vertices in each bin. The peak bin is identified as the floor.
- **Vertex Filtering** (L71-75): Collects all vertices within ±15cm (`floorTolerance`) of the identified peak Y.
- **Confidence Calculation** (L77): Measures the ratio of floor vertices to total vertices; higher ratios indicate cleaner scans.

## Used By
- `Main.kt`: For the `inspect` command.
- `DraftPipeline.kt`: Step 3 of the extraction flow.

## Notes / Risks
- **Horizontal Bias**: Assumes the floor is a horizontal plane (Y-up). This is standard for LiDAR scans but might fail for tilted or rotated models.
- **Tolerance**: The `floorTolerance` allows for slight floor unevenness or sensor noise typical in mobile LiDAR/Polycam scans.
