# File Dossier: QRScanScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`

## Type
Authored Source (Android Compose QR Overlay)

## Role
Passive QR scan chrome rendered over the `ArCameraActivity` ARCore camera preview.

## Imports / Includes
- Jetpack Compose layout/material/icon APIs.
- `ArCameraFlowViewModel`.

## Exports / Public Surface
- `QRScanScreen(flowModel, onCancel)`: Composable QR overlay entrypoint.

## Main Symbols
- `QRScanScreen`: Applies a 40% black dim overlay and renders `ScanDotBackground` and `QRScanChrome`.
- `QRScanChrome`: Header, scan reticle, scanning status, validation error, and retry button.
- `ScanDotBackground`: Dotted grid Canvas drawing consistent with the app's dark theme.

## Important Logic
- This file no longer owns camera permission, CameraX, `PreviewView`, `ImageAnalysis`, `ProcessCameraProvider`, or any ARCore session.
- QR decoding happens in `ArFrameQrScanner` from frames supplied by `UnifiedArRenderer`.
- Simulation support: if `onSimulateScan` is provided (e.g., when running on an emulator), a "Simulate Entrance Scan" button is shown.
- Retry clears the ViewModel QR error and unlocks the scanner; it does not touch camera/session lifecycle.
- Background rendering: uses the same `DotGridBackground` pattern as the home screen for visual consistency.

## Uses
- `ArCameraFlowViewModel`

## Used By
- `ArCameraActivity.kt`: Renders during `Phase.QrScan`.

## Related Tests
- None.
