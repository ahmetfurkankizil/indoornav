# File Dossier: ArSessionManager.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArSessionManager.kt`

## Type
Authored Source (AR Infrastructure)

## Role
Wraps the ARCore `Session` lifecycle and builds an Augmented Image database from the reviewed package entrance marker asset.

## Imports / Includes
- `android.content.Context`
- `android.graphics.BitmapFactory`
- `com.google.ar.core.AugmentedImageDatabase`
- `com.google.ar.core.Config`
- `com.google.ar.core.Session`

## Exports / Public Surface
- `ArSessionManager`: Lifecycle manager.

## Main Symbols
- `createSession(context, markerImageAssetPath, markerImageName, markerWidthMeters)`: Loads marker bitmap from Android assets, adds it to an Augmented Image database, and configures ARCore.
- `setCameraTexture(textureId)`: Connects the GL camera background texture to ARCore.
- `resumeSession()` / `pauseSession()`: ARCore lifecycle hooks.
- `stopSession()`: Closes the session and clears loaded-image metadata.

## Important Logic
- Asset loading from `assets/ar/<referenceImageName>.png` is required before session creation succeeds.
- Marker physical width comes from `entrance_markers.json`.
- Session config uses latest camera image updates, horizontal plane finding, and auto focus.

## Uses
- ARCore SDK.
- Android assets: marker reference image.

## Used By
- `AndroidArNavigationViewModel.kt`: Creates/resumes/stops the session and sets the camera texture.
- `ArCoreCameraRenderer.kt`: Updates the session every GL frame.

## Config / Constants / Protocol Details
- Update Mode: `LATEST_CAMERA_IMAGE`.
- Plane Finding: `HORIZONTAL`.
- Reference image asset path: `ar/<referenceImageName>.png`.

## Related Tests
- None.

## Notes / Risks
- AR session creation can throw on unsupported devices.
- Missing marker assets now surface as configuration errors rather than silent alignment timeouts.
