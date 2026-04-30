# File Dossier: Relocalizer.kt

## Path
app/src/main/java/com/example/vecturai/ar/Relocalizer.kt

## Type
source

## Role
Performs coordinate system alignment (Procrustes analysis) between the AR session and the building graph using multiple anchor correspondences.

## Imports / Includes
- `com.google.ar.core.Pose`

## Exports / Public Surface
- `Correspondence` (data class)
- `Relocalizer` (object)
- `fitGraphToSession(correspondences)`
- `rejectOutliers(correspondences, fit, maxResidualMeters)`

## Main Symbols
- `Correspondence`: Pairs a graph-coordinate pose with its resolved session-coordinate pose.
- `fitGraphToSession`: Computes a single `Pose` (2D Procrustes fit) representing the transform from graph space to session space.
- `rejectOutliers`: Filters out correspondences that deviate significantly from a proposed fit.

## Important Logic by Line Range
- L15-68: `fitGraphToSession` implementation, handling single-point and multi-point 2D fitting.
- L70-79: `rejectOutliers` logic using residual distance checks.

## Uses
- `PoseUtils.kt` (via `translationVec` and `distanceMeters`)

## Used By
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- `EPSILON`: Used for numerical stability in normalization.

## Related Tests
- `app/src/test/java/com/example/vecturai/ar/RelocalizerTest.kt`

## Notes / Risks
- Relies on the assumption that the graph and session use the same metric scale.
- Accuracy improves with more spatially distributed correspondences.
