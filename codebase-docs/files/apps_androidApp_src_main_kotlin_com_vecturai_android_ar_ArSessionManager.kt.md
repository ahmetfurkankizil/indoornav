# File Dossier: ArSessionManager.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArSessionManager.kt`

## Type
Authored Source (AR Infrastructure)

## Role
Wraps the ARCore `Session` lifecycle. Configures the camera update mode, plane finding, and the Augmented Image database.

## Imports / Includes
- `com.google.ar.core.AugmentedImageDatabase`
- `com.google.ar.core.Session`
- `com.google.ar.core.Config`

## Exports / Public Surface
- `ArSessionManager`: Lifecycle manager.

## Main Symbols
- `createSession(...)`: Initializes ARCore with specified configuration.
- `resumeSession()` / `pauseSession()`: Lifecycle hooks for Activity.
- `getTrackingStateLabel()`: Diagnostic tracking status.

## Important Logic by Line Range
- **40-55**: `AugmentedImageDatabase` configuration with `entrance_marker_main`.
- **45**: `markerWidthMeters` (default 0.21m) used for physical scale anchoring.

## Uses
- `ARCore SDK`: Core session management.

## Used By
- `ArNavigationActivity.kt`: Primary consumer.

## Config / Constants / Protocol Details
- Update Mode: `LATEST_CAMERA_IMAGE`.
- Plane Finding: `HORIZONTAL`.

## Related Tests
- None.

## Notes / Risks
- AR session creation can be expensive and may throw on unsupported devices.
