# File Dossier: PoseUtils.kt

## Path
app/src/main/java/com/example/vecturai/ar/PoseUtils.kt

## Type
source

## Role
Low-level math and utility functions for `Pose` and `Vec3` operations.

## Imports / Includes
- `com.google.ar.core.Pose`

## Exports / Public Surface
- `Vec3` (data class with math operators)
- `Pose.translationVec()`
- `Pose.forwardVec()` (returns nullable Vec3)
- `distanceMeters(a, b)`
- `relativePose(origin, pose)`
- `estimateSessionPose(sessionFromGraph, node)`
- `positionInFrontOfCamera(cameraPose, meters)`

## Main Symbols
- `Vec3`: Custom lightweight 3D vector for coordinate math.
- `positionInFrontOfCamera`: Projects a point at camera height in the forward direction.
- `estimateSessionPose`: Computes where a graph node should be in session space given a global transform.
- `sessionFromGraphPose`: Computes the session-to-graph offset from a single resolved node.

## Important Logic by Line Range
- L10-27: `Vec3` definition with `horizontal()` and `normalized()` helpers.
- L91-112: `positionInFrontOfCamera` logic using flat-forward projection and vertical offset.

## Uses
- `MapGraph.kt` (via `MapNode`)

## Used By
- `ArrowRenderer.kt`
- `NavigationViewModel.kt`
- `Relocalizer.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- `forwardVec` uses ARCore's Z-axis inversion to get standard forward direction.
- `rotateY` handles horizontal rotation only.
