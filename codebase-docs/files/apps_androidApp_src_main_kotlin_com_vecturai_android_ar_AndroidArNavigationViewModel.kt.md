# File Dossier: AndroidArNavigationViewModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`

## Type
Authored Source (Android AR Navigation State)

## Role
Android ViewModel for route configuration, entrance-poster alignment, next-action guidance, tracking labels, haptic events, and arrival state. It no longer owns ARCore session lifecycle.

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
- `ArNavigationUiState`: State object containing route info, next-action guidance, tracking labels, and arrival status.
- `configure(routePackage, entranceMarker)`: Resets route/alignment state, configures marker detection, and starts the alignment timeout.
- `onFrame(frame, width, height)`: Per-frame marker detection, tracking status, camera pose sampling, and progress updates.
- `simulateAlignment()`: Development fallback for emulator/non-marker testing.
- `advanceProgress()`: Simulated progress advance for demo mode.
- `endNavigation()`: Clears route, marker, timeout, and haptic-related state.
- `computeNextAction(distance)`: Derives user-friendly guidance ("Turn left ahead", etc.) from route arrows.

## Important Logic
- `UnifiedArSession` and `UnifiedArRenderer` now own all session lifecycle and frame delivery.
- Refined tracking labels: "Tracking", "Hold steady", "Re-centering..." based on ARCore `TrackingState` and failure reasons.
- Route progress: tracks total distance, current node, and ETA (at 1.2 m/s).
- Next-action card: derives guidance text and icons (straight, left, right, arrival) from the lookahead arrow. Uses `VecturaiColors` and `VecturaiTypography` for high-fidelity rendering.
- Haptics: `AndroidHapticManager` fires events for alignment lock (medium), turn warning (notification), re-centering (light), and arrival (success).

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
