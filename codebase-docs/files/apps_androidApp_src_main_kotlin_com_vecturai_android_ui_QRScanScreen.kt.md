# File Dossier: QRScanScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`

## Type
Authored Source (Android Compose QR Scanner)

## Role
Camera-backed QR scanning screen for the entrance poster flow.

## Imports / Includes
- Android camera permission APIs
- CameraX `Preview`, `ImageAnalysis`, `PreviewView`, `CameraSelector`
- ML Kit barcode scanning
- Jetpack Compose foundation/material/icons/runtime APIs
- `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- `com.vecturai.android.navigation.AndroidNavigationFlowModel`

## Exports / Public Surface
- `QRScanScreen(flowModel)`

## Main Symbols
- `QRScanScreen`: Permission-aware scanner host and retry coordination.
- `QRScanChrome`: Product copy, scanner frame, error/retry controls.
- `CameraPermissionCard`: Permission request UI.
- `CameraQrPreview`: CameraX preview and ML Kit QR analyzer.

## Important Logic
- Requests camera permission before binding CameraX.
- Binds CameraX preview and image analysis to the lifecycle owner.
- Uses ML Kit barcode scanner to read the first QR value, then closes the image proxy.
- A retry token unlocks scanning after invalid QR errors.

## Uses
- `AndroidNavigationFlowModel.onQRScanned`
- CameraX
- ML Kit Barcode Scanning

## Used By
- `AndroidNavigationApp.kt`: Rendered for `FlowState.QrScan`.

## Related Tests
- None.
