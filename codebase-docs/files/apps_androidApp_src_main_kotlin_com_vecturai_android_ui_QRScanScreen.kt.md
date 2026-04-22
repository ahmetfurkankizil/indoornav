# File Dossier: QRScanScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`

## Type
Authored Source (Android Compose QR Scanner)

## Role
Camera-backed QR scanning screen for the entrance poster flow.

## Imports / Includes
- Android camera permission APIs.
- CameraX `Preview`, `ImageAnalysis`, `ProcessCameraProvider` (standard mode).
- ARCore `Session` and `Frame` (unified mode).
- ML Kit barcode scanning.
- Jetpack Compose components.
- `com.vecturai.android.ar.ArFeatureFlags`.
- `com.vecturai.android.ar.ArSessionManager`.

## Exports / Public Surface
- `QRScanScreen(flowModel)`: Composable entrypoint.

## Main Symbols
- `QRScanScreen`: Host component that chooses the preview mode based on feature flags.
- `CameraQrPreview`: Legacy mode using CameraX and ML Kit.
- `ArCoreQrPreview`: Unified mode using ARCore to acquire camera frames for ML Kit scanning.
- `QRScanChrome`: UI overlay for scanning status and errors.

## Important Logic
- **Preview Selection**: Uses `ArFeatureFlags.ArUnifiedCameraPipeline` to decide whether to use standard CameraX or an ARCore-backed preview.
- **Unified Scanning**: In `ArCoreQrPreview`, it uses `sessionManager.createSessionWithoutMarker` to start a background AR session, then acquires camera images from AR frames for scanning.
- **Camera Release Coordination**: Explicitly waits for the camera to close (using `CameraState` observer) before transitioning to the next screen in standard mode to prevent contention.

## Uses
- `AndroidNavigationFlowModel`: State management.
- `ArSessionManager`: Lifecycle management (unified mode).
- ML Kit Barcode Scanning.

## Used By
- `AndroidNavigationApp.kt`: Main navigation host.

## Related Tests
- None.

## Notes / Risks
- Unified mode minimizes camera "flicker" when transitioning from QR scan to AR navigation but relies on ARCore stability.

