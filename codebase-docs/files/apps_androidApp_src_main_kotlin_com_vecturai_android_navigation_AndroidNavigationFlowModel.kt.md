# File Dossier: AndroidNavigationFlowModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/navigation/AndroidNavigationFlowModel.kt`

## Type
Authored Source (Android Flow State)

## Role
Defines the Android visitor flow state split between MainActivity and the dedicated AR camera Activity.

## Imports / Includes
- `androidx.lifecycle.ViewModel`
- ARCore `Frame`
- `AndroidReviewedPackageLoader`
- `ArFrameQrScanner`
- `QRPayload`
- Kotlin `StateFlow` / `MutableStateFlow`

## Exports / Public Surface
- `AndroidNavigationFlowModel`
- `AndroidNavigationFlowModel.HomeState`
- `ArCameraFlowViewModel`
- `ArCameraFlowViewModel.Phase`
- `ArCameraFlowViewModel.SessionData`

## Main Symbols
- `AndroidNavigationFlowModel`: MainActivity-only package readiness model (`Home` or `PackageError`).
- `ArCameraFlowViewModel`: Activity-scoped QR-to-AR state machine.
- `RouteSummary`: Data class for walking time (at 1.2 m/s), distance, and floor info.
- `ArCameraFlowViewModel.onQrFrame(frame, rotationDegrees)`: Sends ARCore camera frames to ML Kit QR scanning.
- `ArCameraFlowViewModel.onQRScanned(rawValue)`: Parses and validates Vectura AI entrance QR payloads.
- `selectDestination(room)` / `startNavigation()`: Computes the route package and advances to AR navigation.
- `routeSummaryFor(room)`: Pre-computes route stats for destination selection.

## Important Logic
- Home no longer drives the post-home visitor flow. It only verifies that the bundled reviewed package can load.
- QR scan, entrance confirmation, destination selection, route preview, and AR navigation are scoped to `ArCameraActivity`.
- `SessionData` preserves `confirmedEntrance` and `validatedEntranceMarker` across phases.
- Route summary logic estimates time based on total distance and typical walking speed.
- QR scanning accepts only valid `Vectura AI-entrance` payloads matching the loaded manifest building id and entrance marker id.
- Route computation still happens immediately when a destination is selected so route preview and AR navigation share the same `LoadedPackage`.

## Uses
- `AndroidReviewedPackageLoader`
- `ArFrameQrScanner`
- `QRPayload`

## Used By
- `MainActivity.kt`: Uses `AndroidNavigationFlowModel`.
- `ArCameraActivity.kt`: Uses `ArCameraFlowViewModel`.
- `AndroidNavigationApp.kt`: Renders Home/PackageError from `AndroidNavigationFlowModel`.
- `QRScanScreen.kt`, `DestinationSelectScreen`, `RoutePreviewScreen`: Render Activity-scoped phases from `ArCameraFlowViewModel`.

## Related Tests
- None.
