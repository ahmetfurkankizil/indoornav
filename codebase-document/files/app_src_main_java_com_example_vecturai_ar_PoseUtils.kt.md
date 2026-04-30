# File Dossier: PoseUtils.kt

## Path
app/src/main/java/com/example/vecturai/ar/PoseUtils.kt

## Type
source

## Role
Math utility library for 3D vectors and ARCore Pose transformations.

## Imports / Includes
- `com.google.ar.core.Pose`
- `kotlin.math.*`

## Exports / Public Surface
- `Vec3` (data class): Custom 3D vector with arithmetic operators.
- `Pose.translationVec()`: Extension to get Vec3 from Pose.
- `Pose.forwardVec()`: Extension to get normalized forward direction.
- `distanceMeters(...)`: Overloaded distance functions for Poses and Vec3s.
- `sessionFromGraphPose(...)`: Computes the transformation from graph-space to session-space.
- `estimateSessionPose(...)`: Projects a graph node into session-space.
- `yawDegreesToward(...)`: Calculates horizontal rotation angle toward a target.

## Main Symbols
- `Vec3`: Core geometry primitive.
- `sessionFromGraphPose`: Critical for localizing the user within a loaded graph.
- `rotateY`: Rotates a vector around the vertical axis.

## Important Logic by Line Range
- L15-17: Operator overloading for `Vec3`.
- L31-34: Forward vector extraction from ARCore's Z-axis.
- L68-72: Coordinate system synchronization logic.
- L74-77: Atan2-based yaw calculation.

## Uses
- ARCore SDK

## Used By
- `ArrowRenderer.kt`
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Direction: Forward is `-Z` in ARCore camera space.

## Related Tests
N/A

## Notes / Risks
- `Vec3.normalized()` defaults to `-Z` if length is zero to avoid division by zero.
- `sessionFromGraphPose` assumes a single rigid transformation is sufficient for local drift correction.
