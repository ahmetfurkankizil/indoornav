# File Dossier: ArNavigationScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`

## Type
Authored Source (Android Compose AR Overlay)

## Role
Passive AR navigation overlay that draws alignment guidance, next-action cards, tracking badge, ETA HUD, and arrival card over `ArCameraActivity`'s Activity-owned ARCore camera view.

## Imports / Includes
- Jetpack Compose animation/foundation/material/icons/runtime APIs.
- `AndroidArNavigationViewModel`
- `ArNavigationUiState`
- `NavigationActionIcon`
- `TrackingStatusIcon`
- `ArrowPlacementType`

## Exports / Public Surface
- `ArNavigationScreen`: Root overlay orchestrator for the AR session lifecycle.

## Main Symbols
- `ArNavigationScreen`: Switches between alignment, active navigation, and arrival states using `AnimatedContent`.
- `AlignmentOverlay`: Provides scan guidance with `RadarSweep` animation and diagnostic stats (frames, candidates).
- `InstructionBanner`: Glassmorphic banner at the top showing the next turn action (`TurnGlyph`), distance, and `TrackingBadge`.
- `BottomHud`: Contains a circular `ProgressEtaCluster` and the `SwipeToEndRoute` safety action.
- `ArrivalOverlay`: Celebratory full-screen state with `ConfettiBurst`, `AnimatedCheckMark`, and journey statistics.
- `TrackingBadge`: Contextual `StatPill` (Tracking, Hold steady, Re-centering) with color-coded confidence levels.

## Important Logic
- **State Transitioning**: Uses `AnimatedContent` for smooth cross-fades between session states (aligning → active → arrival).
- **Circular Progress**: `ProgressEtaCluster` draws a custom `Canvas` arc based on `remainingDistance / totalDistance`.
- **Swipe Action**: `SwipeToEndRoute` uses `Modifier.draggable` to prevent accidental navigation cancellation.
- **Glassmorphism**: Banners and HUDs use `glass = true` cards to maintain visibility of the AR feed while providing high text contrast.
- **Micro-animations**: Includes `RadarSweep` for scanning and `ConfettiBurst` for arrival to enhance user delight.

## Uses
- `VecturaiColors`, `VecturaiShapes`, `Spacing`, `VecturaiTypography`, `VecturaiBrush`
- `StatPill`, `IconChip`, `VecturaiCard`, `VecturaiPrimaryButton`, `VecturaiSecondaryButton`, `AnimatedNumber`, `GradientText`, `vecturaiTap`
- `AndroidArNavigationViewModel`, `ArNavigationUiState`

## Used By
- `ArCameraActivity.kt`: Rendered for `Phase.ArNavigation`.

## Related Tests
- None.
