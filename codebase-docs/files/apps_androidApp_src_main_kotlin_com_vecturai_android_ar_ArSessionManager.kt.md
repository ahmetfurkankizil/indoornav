# File Dossier: ArSessionManager.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArSessionManager.kt`

## Type
Authored Source (AR Infrastructure)

## Role
Wraps the ARCore `Session` lifecycle and builds an Augmented Image database from the reviewed package entrance marker asset.

## Imports / Includes
- `android.app.Activity`
- `android.graphics.BitmapFactory`
- `com.google.ar.core.AugmentedImageDatabase`
- `com.google.ar.core.Config`
- `com.google.ar.core.Session`
- `com.vecturai.android.ar.ArFeatureFlags`

## Exports / Public Surface
- `ArSessionManager`: Lifecycle manager.
- `SessionFailure`: Sealed class for categorized failures.

## Main Symbols
- `initializeEmptySession(activity)`: Creates an AR session without any reference images.
- `createSession(activity, markerImageAssetPath, markerImageName, markerWidthMeters)`: Loads marker bitmap, sanitizes it (copying hardware bitmaps, ensuring opacity), and configures ARCore with an image database.
- `setCameraTexture(textureId)`: Connects the GL camera background texture to ARCore.
- `resumeSession()` / `pauseSession()`: ARCore lifecycle hooks.
- `stopSession()`: Pauses and closes the session.
- `awaitClosed()`: Suspends to allow session cleanup.

## Important Logic
- **Activity Dependency**: Now requires `Activity` instead of `Context` for session creation to avoid ARCore initialization deadlocks.
- **Bitmap Sanitization**: pro-actively copies `HARDWARE` bitmaps to `ARGB_8888` and ensures they are opaque before passing to ARCore to prevent native crashes.
- **Unified Pipeline**: Respects `ArFeatureFlags.ArUnifiedCameraPipeline` to potentially preserve sessions between screens.

## Uses
- ARCore SDK.
- Android assets: marker reference image.

## Used By
- `AndroidArNavigationViewModel.kt`: Creates/resumes/stops the session.
- `ArCoreCameraRenderer.kt`: Updates the session every GL frame.
- `QRScanScreen.kt`: Uses `ArCoreQrPreview` which interacts with the session manager.

## Config / Constants / Protocol Details
- Update Mode: `LATEST_CAMERA_IMAGE`.
- Plane Finding: `HORIZONTAL`.

## Related Tests
- None.

## Notes / Risks
- Extensive debug logging (`[ARDiag]`) added for troubleshooting real-device camera issues.
- `SessionFailure` provides user-friendly error messages for ARCore installation or device support issues.

