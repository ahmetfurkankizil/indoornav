# File Dossier: ArFrameQrScanner.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/qr/ArFrameQrScanner.kt`

## Type
Authored Source (ARCore Frame QR Scanner)

## Role
Converts ARCore camera frames into ML Kit QR scan attempts without CameraX.

## Imports / Includes
- Android `media.Image`
- ARCore `Frame`, `NotYetAvailableException`
- ML Kit `BarcodeScannerOptions`, `BarcodeScanning`, `Barcode`, `InputImage`
- `AtomicBoolean`

## Exports / Public Surface
- `ArFrameQrScanner`
- `scan(frame, rotationDegrees)`
- `reset()`
- `close()`

## Main Symbols
- `scan(frame, rotationDegrees)`: Throttles to roughly 6 Hz, acquires the ARCore camera image, builds an `InputImage`, and submits it to ML Kit.
- `processImage(image, rotationDegrees)`: Handles ML Kit callbacks, closes the acquired image on completion, and locks scanning after the first accepted code.

## Important Logic
- Uses `Frame.acquireCameraImage()` from the existing ARCore session.
- No `PreviewView`, `ImageAnalysis`, `ProcessCameraProvider`, or CameraX APIs are used.
- The callback returns whether the payload was accepted; invalid QR payloads do not permanently lock scanning.

## Uses
- ML Kit barcode scanning
- ARCore camera image frames

## Used By
- `ArCameraFlowViewModel` in `AndroidNavigationFlowModel.kt`

## Related Tests
- None.
