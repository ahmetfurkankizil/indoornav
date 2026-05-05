# File Dossier: UnifiedArSession.kt

## Path
`apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArSession.kt`

## Type
Authored Source (ARCore Session Lifecycle)

## Role
Owns the single ARCore `Session` lifecycle for `ArCameraActivity`: request/install readiness, create/configure, camera texture binding, resume, pause, and close.

## Imports / Includes
- Android Activity, camera permission, bitmap, canvas, and Toast APIs.
- ARCore `ArCoreApk`, `Session`, `Config`, and `AugmentedImageDatabase`.
- `AndroidReviewedPackageLoader.PackageMarker`.

## Exports / Public Surface
- `UnifiedArSession`
- `session`
- `isRunning`
- `referenceImageIndex`
- `onActivityResume(...)`
- `onActivityPause()`
- `onActivityDestroy()`

## Main Symbols
- `onActivityResume(activity, cameraTextureId, marker)`: Confirms permission, handles ARCore install flow, creates/configures the session if needed, binds the GL camera texture, and resumes.
- `createSession(activity, marker)`: Creates `Session(activity)` and preloads the reviewed package entrance poster into an augmented image database.
- `loadSanitizedBitmap(...)`: Decodes the marker bitmap, copies off hardware config, and composites alpha over opaque white.

## Important Logic
- There is no retry/backoff/rebuild path. Setup or resume exceptions show a one-shot toast and call `activity.finish()`.
- The session is paused on Activity pause but closed only on Activity destroy.
- The ARCore `Config` uses `LATEST_CAMERA_IMAGE`, `AUTO` focus, `HORIZONTAL` plane finding, and the preloaded augmented image database.

## Uses
- ARCore SDK
- Android assets: `assets/ar/<referenceImageName>.png`

## Used By
- `ArCameraActivity.kt`
- `UnifiedArRenderer.kt`

## Related Tests
- None.
