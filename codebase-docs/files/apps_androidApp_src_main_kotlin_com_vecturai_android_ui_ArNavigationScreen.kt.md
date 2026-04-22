# File Dossier: ArNavigationScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`

## Type
Authored Source (Android Compose AR Overlay)

## Role
Passive AR navigation overlay that draws projected route arrows, alignment guidance, next-action cards, tracking badge, ETA HUD, and arrival card over `ArCameraActivity`'s Activity-owned ARCore camera view.

## Imports / Includes
- Jetpack Compose animation/foundation/material/icons/runtime APIs.
- `AndroidArNavigationViewModel`
- `ArNavigationUiState`
- `NavigationActionIcon`
- `TrackingStatusIcon`
- `ArrowPlacementType`

## Exports / Public Surface
- `ArNavigationScreen(viewModel, onEnd, onRetryActivity)`

## Main Symbols
- `ArNavigationScreen`: Renders UI state only; it does not create or own a `GLSurfaceView`.
- `ProjectedArrowLayer`: Draws projected arrow icons on top of camera feed.
- `AlignmentOverlay`: Pre-alignment poster guidance and timeout actions.
- `ActiveNavigationOverlay`: Top guidance and bottom HUD during route following.
- `InstructionBanner`: Next-action card.
- `BottomHud`: Remaining distance, ETA, simulated advance, and end route action.
- `ArrivalOverlay`: Spring-animated arrival card.

## Important Logic
- Session lifecycle, camera permission, GL surface creation, and renderer wiring moved to `ArCameraActivity`.
- Error/timeout retry callbacks recreate the hosting Activity, yielding a fresh ARCore session instead of rebuilding in-place.
- Bottom HUD ETA uses remaining distance at 1.2 m/s.

## Uses
- `AndroidArNavigationViewModel`
- `ArNavigationUiState`

## Used By
- `ArCameraActivity.kt`: Rendered for `Phase.ArNavigation`.

## Related Tests
- None.
