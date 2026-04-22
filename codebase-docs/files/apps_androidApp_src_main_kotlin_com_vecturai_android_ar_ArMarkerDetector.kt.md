# File Dossier: ArMarkerDetector.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`

## Type
Authored Source (AR Engine Logic)

## Role
Handles the detection of ARCore `AugmentedImage` trackables. Distinguishes between ENTRANCE and CHECKPOINT markers.

## Imports / Includes
- `com.google.ar.core.AugmentedImage`
- `com.google.ar.core.Frame`

## Exports / Public Surface
- `ArMarkerDetector`: Main detection engine.
- `MarkerDetectionEvent`: Data class for detection results.

## Main Symbols
- `processFrame(frame)`: Iterates over updated trackables in every render frame.
- `registerMarker(index, marker)`: Maps ARCore image database indices to building metadata.
- `onMarkerDetected`: Callback for initial entrance alignment.
- `onCheckpointDetected`: Callback for mid-route corrections.

## Important Logic by Line Range
- **91-110**: Extracting pose (tx, ty, tz) and rotation (atan2 on quaternion) from ARCore image.
- **112-140**: Role-based dispatch (Entrance vs Checkpoint).

## Uses
- `ARCore SDK`: For trackable management.

## Used By
- `ArNavigationActivity.kt`: Primary consumer for world alignment.

## Config / Constants / Protocol Details
- Uses `Math.toDegrees` and `atan2` to convert ARCore quaternions to building-aligned Y-rotation.

## Related Tests
- None.

## Notes / Risks
- Relies on physical marker width (`markerWidthMeters`) for depth accuracy.
