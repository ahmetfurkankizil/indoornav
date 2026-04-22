# File Dossier: AndroidArNavigationViewModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`

## Type
Authored Source (Android AR Navigation State)

## Role
Android ViewModel that ports the iOS AR navigation state and guidance math into the Compose/ARCore implementation. It owns route configuration, entrance-poster alignment, progress projection, next-action guidance, tracking labels, haptic events, and arrival state.

## Imports / Includes
- `androidx.lifecycle.ViewModel`, `viewModelScope`
- `com.google.ar.core.Frame`, `Session`, `TrackingState`, `TrackingFailureReason`
- `com.vecturai.android.data.AndroidReviewedPackageLoader`
- `com.vecturai.android.data.ArrowPlacementType`
- Kotlin coroutines `StateFlow`, `MutableStateFlow`, `delay`
- `kotlin.math` helpers

## Exports / Public Surface
- `ArNavigationUiState`: Compose-facing AR screen state.
- `NavigationActionIcon`: semantic next-action icon enum.
- `TrackingStatusIcon`: user-facing tracking badge icon enum.
- `AndroidArNavigationViewModel`: AR navigation coordinator.

## Main Symbols
- `configure(...)`: Sets route package, marker, destination, thresholds, and renderer config.
- `startSession(context)`: Creates ARCore session, configures marker detection, registers the reference image, and starts alignment timeout.
- `retryAlignment(context)` / `simulateAlignment()`: Recovery/testing alignment entry points.
- `onFrame(frame, width, height)`: Processes marker detection, tracking status, camera pose progress, and projected arrows.
- `advanceProgress()`: Simulation helper that advances route progress.
- `endNavigation()`: Stops ARCore, cancels timeout, and clears renderer state.

## Important Logic
- Alignment lock uses marker AR yaw minus marker building yaw, then computes translation offsets from rotated building marker coordinates.
- Camera pose sampling inverts the alignment transform, projects the camera onto the route polyline, and keeps progress monotonic.
- Rolling lookahead and fade-behind are delegated to `ArRouteRenderer`.
- Arrival fires when destination distance is within reviewed-package threshold, defaulting to 1.5m.
- Tracking copy maps ARCore failure reasons to "Tracking", "Hold steady", and "Re-centering...".

## Uses
- `ArSessionManager`
- `ArMarkerDetector`
- `ArRouteRenderer`
- `AndroidHapticManager`
- `AndroidReviewedPackageLoader.LoadedPackage`

## Used By
- `MainActivity.kt`: Resolved through Koin.
- `AndroidNavigationApp.kt` / `ArNavigationScreen.kt`: Configured and observed during AR navigation.
- `ArCoreCameraRenderer.kt`: Calls `startSession`, `setCameraTexture`, `currentSession`, and `onFrame`.

## Config / Constants / Protocol Details
- Alignment timeout: 30 seconds.
- Pose sample cadence: 500ms.
- Projected-arrow update cadence: 100ms.
- Marker asset path: `assets/ar/<referenceImageName>.png`.

## Related Tests
- None.

## Notes / Risks
- Requires real ARCore device validation for marker matching, camera projection, and haptic timing.
