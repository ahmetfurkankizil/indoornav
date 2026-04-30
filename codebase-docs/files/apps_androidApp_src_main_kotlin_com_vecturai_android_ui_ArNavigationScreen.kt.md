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
- `ProjectedArrowLayer`: Draws projected 3D arrow icons pinned to building-local coordinates.
- `AlignmentOverlay`: Premium pre-alignment guidance with actionable hints (No poster detected, Hold steady).
- `ActiveNavigationOverlay`: Hosts the `NextActionCard`, `TrackingBadge`, and `ProgressStrip`.
- `NextActionCard`: Prominent guidance card ("Turn left ahead", etc.) with dynamic icons and distance lookahead.
- `TrackingBadge`: Context-aware status label ("Tracking", "Hold steady", "Re-centering...").
- `ProgressStrip`: Compact bottom HUD with remaining distance, ETA, and end-route action.
- `ArrivalOverlay`: Spring-animated success card with "You've reached [Destination]".

## Important Logic
- Navigation guidance: derived from `AndroidArNavigationViewModel.ArNavigationUiState`, ensuring zero-latency UI updates.
- Visual consistency: uses the same glassmorphic design tokens (blur, borders, vibrant accents) as the home screen.
- ETA calculation: distance-based estimate matching the iOS client logic for platform parity.
- Emulator detection: `isLikelyEmulator()` is now implemented inline to conditionally show simulation controls during development.

## Uses
- `AndroidArNavigationViewModel`
- `ArNavigationUiState`

## Used By
- `ArCameraActivity.kt`: Rendered for `Phase.ArNavigation`.

## Related Tests
- None.
