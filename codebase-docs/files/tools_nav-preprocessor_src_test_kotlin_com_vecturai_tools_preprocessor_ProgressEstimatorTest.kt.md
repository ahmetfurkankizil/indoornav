# File Dossier: ProgressEstimatorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/ProgressEstimatorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_session_management`
- **Status**: Mapped

## Role
Tests the core progress estimation algorithm, which projects the user's AR position onto a multi-segment 3D route polyline. It ensures that the percentage of completion and remaining distance are calculated accurately, while also validating guards against sensor noise and backward movement.

## Public Surface
- `ProgressEstimatorTest`: Test class.

## Main Symbols
- `TestEstimator`: Mock implementation of the polyline projection engine.
- `update()`: Projects a coordinate and returns an `EstimatorResult`.
- `EstimatorResult`: Data structure containing progress (0..1), remaining distance (meters), current segment index, and confidence status.

## Important Logic
- **Polyline Projection** (L37-55): Validates the math for projecting a 3D point onto the closest point on any segment of the route polyline.
- **Progress Calculation** (L63-64): Tests the translation of cumulative path distance into a 0.0 to 1.0 fraction.
- **Monotonic Guard** (L58-61, L133-140): Ensures that reported progress never decreases, even if the user physically walks backward or the sensor drifts.
- **Off-Route Detection** (L126-130): Verifies that lateral distance from the route is calculated correctly and triggers `lowConfidence` if it exceeds a threshold (e.g., 3m).
- **Segment Tracking** (L162-168): Confirms that the engine correctly identifies which segment of the route the user is currently on (crucial for accurate turn instructions).

## Related Features
- `navigation_session_management`: This is the engine that drives the progress bar and arrival detection.

## Notes / Risks
- **Redundant Implementation**: Like other tests in this suite, it replicates the core algorithm in a mock class to validate the logical contract independently.
- **Euclidean Math**: Assumes flat-world (XZ) projection for progress, which matches the building floor-level navigation model.
