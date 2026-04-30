# File Dossier: ArCameraActivity.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCameraActivity.kt`

## Type
Authored Source (Android Activity / AR Camera Host)

## Role
Dedicated Activity for the full post-home visitor flow. It owns one long-lived ARCore session, one `GLSurfaceView`, one camera texture, and all Compose overlays from QR scan through AR navigation.

## Imports / Includes
- Android Activity, camera permission, `GLSurfaceView`, and Toast APIs.
- Jetpack Compose and Activity Result APIs.
- ARCore `Frame`.
- `ArCameraFlowViewModel`, `AndroidArNavigationViewModel`.
- `UnifiedArSession`, `UnifiedArRenderer`.
- QR, entrance-confirm, destination, route-preview, and AR overlay composables.

## Exports / Public Surface
- `ArCameraActivity`

## Main Symbols
- `unifiedSession`: Activity-owned `UnifiedArSession`.
- `renderer`: Single `UnifiedArRenderer` instance for camera background and frame dispatch. It also passes `ArRouteRenderer` for 3D arrow rendering.
- `ArCameraContent`: Hosts the GL view and phase-specific Compose overlays.
- `resumeCameraWhenReady()`: Waits for camera permission, reviewed package load, and GL texture creation before resuming ARCore.
- `onArFrame(frame, width, height, rotationDegrees)`: Dispatches frames to QR scanning or AR navigation based on `ArCameraFlowViewModel.Phase`.
- `isLikelyEmulator()`: Detects if the app is running on an Android emulator (moved inline from DeviceEnvironment).

## Important Logic
- Emulator detection: provides a "Simulate Entrance Scan" button in QR scan and a manual alignment option in AR navigation for development.
- Camera permission denial is handled inside this Activity; canceling finishes the Activity.
- ARCore setup/resume fatal failures end the Activity with a toast instead of retrying in-place.
- Destination select and route preview are opaque overlays above a still-resumed ARCore session.
- Finishing the flow calls `finish()`, and `onDestroy` closes the ARCore session deterministically.

## Uses
- `UnifiedArSession`
- `UnifiedArRenderer`
- `ArRouteRenderer`
- `ArCameraFlowViewModel`
- `AndroidArNavigationViewModel`
- `QRScanScreen`
- `DestinationSelectScreen`
- `RoutePreviewScreen`
- `ArNavigationScreen`

## Used By
- `MainActivity.kt`: Launched from Home via `Intent`.
- `AndroidManifest.xml`: Registered as non-exported Activity.

## Related Tests
- None.
