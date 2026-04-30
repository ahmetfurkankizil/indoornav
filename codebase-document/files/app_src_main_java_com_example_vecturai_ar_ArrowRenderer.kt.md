# File Dossier: ArrowRenderer.kt

## Path
app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt

## Type
source

## Role
Calculates the position and orientation of the AR navigation arrow.

## Imports / Includes
- `com.google.ar.core.Pose`

## Exports / Public Surface
- `ArrowPose` (data class)
- `ArrowRenderer` (object)
- `floatingArrowPose(cameraPose, target)` (function)

## Main Symbols
- `ARROW_FORWARD_OFFSET_M`: Constant (1.5m) for arrow placement in front of the camera.
- `WAYPOINT_ADVANCE_DISTANCE_M`: Threshold (1.2m) for arriving at a waypoint.
- `floatingArrowPose`: Combines translation and rotation to orient the arrow.

## Important Logic by Line Range
- L12-19: Core calculation combining placement logic (`positionInFrontOfCamera`) with orientation logic (`yawDegreesToward`).

## Uses
- `PoseUtils.kt` (Implicitly via `Vec3` and utility functions).

## Used By
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Uses horizontal-only rotation (yaw) for arrow direction.

## Related Tests
N/A

## Notes / Risks
- Arrow vertical position (Y) is locked relative to camera height unless modified by math in `PoseUtils`.
- Relies on `horizontalDistanceMeters` for arrival logic.
