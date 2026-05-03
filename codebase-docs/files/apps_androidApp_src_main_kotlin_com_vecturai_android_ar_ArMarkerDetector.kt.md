# File Dossier: ArMarkerDetector.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`

## Type
Authored Source (AR Engine Logic)

## Role
Detects ARCore Augmented Images and converts strictly accepted entrance/checkpoint images into internal marker detection events. The detector mirrors the single-poster contract: only configured marker names or registered database indices are accepted.

## Imports / Includes
- `com.google.ar.core.AugmentedImage`
- `com.google.ar.core.Frame`
- `com.google.ar.core.TrackingState`
- `kotlin.math.atan2`

## Exports / Public Surface
- `ArMarkerDetector`: Main detection engine.
- `KnownMarker`: Registered marker metadata.
- `MarkerDetectionRole`: `ENTRANCE` or `CHECKPOINT`.
- `DetectionFailureReason`: Timeout diagnostics for no candidates, rejected candidates, or missing asset.
- `MarkerDetectionEvent`: Data class for detection results.

## Main Symbols
- `configure(...)`: Resets detector state and registers the expected entrance marker name/metadata.
- `registerMarker(index, name, marker)`: Binds an ARCore image database index and image name to marker metadata.
- `processFrame(frame)`: Scans fully tracking augmented images, rejects unknown names/indices, computes marker yaw, and emits callbacks. Increments `framesAnalyzed` counter.
- `reset()` / `fullReset()`: Clear detection counters (including `framesAnalyzed`) and optionally known marker registrations.
- `onMarkerDetected`, `onCheckpointDetected`: Alignment/correction callbacks.

## Important Logic
- Candidate filtering requires `TrackingState.TRACKING` and `FULL_TRACKING`.
- Rejected marker names are retained for user-facing timeout hints.
- Marker Y rotation is extracted from ARCore quaternion data and carried in `MarkerDetectionEvent`.

## Uses
- ARCore `Frame` and `AugmentedImage`.
- ARCore augmented image database names/indices configured by `UnifiedArSession`.

## Used By
- `AndroidArNavigationViewModel.kt`: Configures the detector and consumes marker events to lock alignment.

## Config / Constants / Protocol Details
- Marker name must match the reviewed package `referenceImageName` or its registered ARCore image index.

## Related Tests
- None.

## Notes / Risks
- No fallback to "any image" is allowed; this preserves strict marker validation.
