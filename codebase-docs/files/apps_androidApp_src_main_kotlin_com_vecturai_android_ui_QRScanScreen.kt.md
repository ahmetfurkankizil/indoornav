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
- `QRScanScreen`: Applies a 40% black dim overlay and renders QR scan chrome.
- `QRScanChrome`: Header, scan reticle, scanning status, validation error, and retry button.

## Important Logic
- This file no longer owns camera permission, CameraX, `PreviewView`, `ImageAnalysis`, `ProcessCameraProvider`, or any ARCore session.
- QR decoding happens in `ArFrameQrScanner` from frames supplied by `UnifiedArRenderer`.
- Retry clears the ViewModel QR error and unlocks the scanner; it does not touch camera/session lifecycle.

## Uses
- `ArCameraFlowViewModel`

## Used By
- `ArCameraActivity.kt`: Renders during `Phase.QrScan`.

## Related Tests
- None.
