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
- `QRScanScreen`: Overlay entry point for the QR/Entrance poster scanning phase.

## Main Symbols
- `QRScanScreen`: Orchestrates the `ScanVignette` and `QRScanChrome`.
- `QRScanChrome`: Top-level container for the scan UI; handles the state transition between scanning, success, and error.
- `AnimatedScanReticle`: Custom `Canvas` drawing that renders "breathing" brackets and a horizontal laser-sweep line.
- `QRStatusPanel`: Glassmorphic status card at the bottom; shows loading, success, or error messages with actionable buttons.
- `ScanVignette`: A dark radial gradient overlay that centers the user's attention on the scan area.

## Important Logic
- **Visual Feedback**: The reticle changes color based on state: Cyan (searching), Green (detected), Amber (error).
- **Scanning Success**: Triggers a "success ripple" animation (`repeat(3)` with `drawCircle`) to provide positive reinforcement.
- **Vignette Strategy**: Uses a `Canvas` to draw a dark vertical gradient with a circular cutout (via `drawCircle` with a thick stroke) to frame the camera feed.
- **Glassmorphism**: The status panel uses `glass = true` to maintain a premium feel while ensuring legibility over the camera preview.
- **Simulation**: Provides a "Simulate Entrance Scan" button when `onSimulateScan` is non-null (typically on emulators).

## Uses
- `VecturaiColors`, `VecturaiShapes`, `Spacing`, `VecturaiBrush`
- `IconChip`, `VecturaiCard`, `VecturaiPrimaryButton`, `VecturaiSecondaryButton`
- `ArCameraFlowViewModel`

## Used By
- `ArCameraActivity.kt`: Renders during `Phase.QrScan`.

## Related Tests
- None.
