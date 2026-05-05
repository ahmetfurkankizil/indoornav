# File Dossier: FloorPlaneEstimatorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/analysis/FloorPlaneEstimatorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the histogram-based floor detection algorithm. It ensures that the `FloorPlaneEstimator` can accurately identify the dominant horizontal surface in a 3D vertex cloud, even in the presence of noise or ceiling/wall geometry.

## Public Surface
- `FloorPlaneEstimatorTest`: Test class.

## Main Symbols
- `estimate()`: The primary method under test.
- `Vec3`: 3D vertex data structure.

## Important Logic
- **Dominant Floor Detection** (L15-25, L28-36): Validates that the estimator correctly identifies the Y-coordinate of the largest horizontal cluster (floor), whether at origin or shifted (e.g., -1.5m).
- **Confidence Scoring** (L46-51): Ensures that a perfectly flat vertex cloud results in a confidence score of 1.0.
- **Tolerance Band Validation** (L54-64): Verifies that only vertices within the estimator's allowed vertical tolerance (e.g., ±15cm) are classified as "floor vertices".
- **Edge Case Handling** (L39-43): Confirms that the estimator returns `null` if given an insufficient number of vertices to perform a meaningful analysis.

## Uses
- `FloorPlaneEstimator`: The component being tested.
- `Vec3`: Data structure.

## Related Features
- `preprocessing`: Fundamental step in identifying walkable space from 3D scans.

## Notes / Risks
- **Heuristics**: The estimator's accuracy depends on the "dominant" assumption. In multi-floor scans (e.g., stairs), the current logic might prioritize only one level.
