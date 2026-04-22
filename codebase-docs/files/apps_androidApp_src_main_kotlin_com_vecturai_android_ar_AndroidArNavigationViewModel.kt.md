# File Dossier: AndroidArNavigationViewModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`

## Type
Authored Source (Android AR Navigation State)

## Role
Android ViewModel for route configuration, entrance-poster alignment, progress projection, next-action guidance, tracking labels, haptic events, and arrival state. It no longer owns ARCore session lifecycle.

## Imports / Includes
- `androidx.lifecycle.ViewModel`, `viewModelScope`
- ARCore `Frame`, `TrackingState`, `TrackingFailureReason`
- `AndroidReviewedPackageLoader`
- `ArrowPlacementType`
- Kotlin coroutines `StateFlow`, `MutableStateFlow`, `delay`
- Kotlin math helpers

## Exports / Public Surface
- `ArNavigationUiState`
- `NavigationActionIcon`
- `TrackingStatusIcon`
- `AndroidArNavigationViewModel`

## Main Symbols
- `configure(routePackage, entranceMarker)`: Resets route/alignment state, configures marker detection, and starts the alignment timeout.
- `onFrame(frame, width, height)`: Per-frame marker detection, tracking status, camera pose sampling, arrow projection, and progress updates.
- `simulateAlignment()`: Development fallback for emulator/non-marker testing.
- `advanceProgress()`: Simulated progress advance for demo mode.
- `endNavigation()`: Clears route, marker, projection, timeout, and haptic-related state.
- `computeNextAction(distance)`: Derives user-friendly guidance ("Turn left ahead", etc.) from route arrows.

## Important Logic
- `UnifiedArSession` and `UnifiedArRenderer` now own all session lifecycle and frame delivery.
- There is no resume, pause, rebuild, backoff, or camera-texture logic in this ViewModel.
- Marker detection remains strict: only the reviewed package entrance marker image/name is accepted.
- Coordinate transformations assume the marker pose establishes the AR-world to building-local transform.
- Haptics fire for route start, imminent turn, re-centering, and arrival.

## Uses
- `ArMarkerDetector`
- `ArRouteRenderer`
- `AndroidHapticManager`

## Used By
- `ArCameraActivity.kt`: Configures the ViewModel and forwards navigation frames from `UnifiedArRenderer`.
- `ArNavigationScreen.kt`: Observes `uiState` for rendering.

## Notes / Risks
- Because the ARCore session runs before AR navigation, frame delivery may begin before this ViewModel is configured; only `Phase.ArNavigation` frames are forwarded here.
- Device validation remains important for ARCore marker detection and projection behavior.
