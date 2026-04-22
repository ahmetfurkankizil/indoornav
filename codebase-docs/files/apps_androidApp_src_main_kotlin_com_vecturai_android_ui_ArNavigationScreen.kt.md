# File Dossier: ArNavigationScreen.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`

## Type
Authored Source (Android Compose AR UI)

## Role
Compose AR navigation screen that hosts the ARCore camera surface and overlays projected route arrows, alignment guidance, next-action cards, tracking badge, ETA HUD, and arrival card.

## Imports / Includes
- Android `GLSurfaceView`
- Jetpack Compose animation/foundation/material/icons/runtime APIs
- `androidx.compose.ui.viewinterop.AndroidView`
- `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- `com.vecturai.android.ar.AndroidArNavigationViewModel`
- `com.vecturai.android.ar.ArCoreCameraRenderer`
- `com.vecturai.android.navigation.AndroidNavigationFlowModel`
- `com.vecturai.android.data.ArrowPlacementType`

## Exports / Public Surface
- `ArNavigationScreen(...)`

## Main Symbols
- `ArNavigationScreen`: Configures AR navigation and embeds `GLSurfaceView`.
- `ProjectedArrowLayer`: Draws projected arrow icons on top of camera feed.
- `AlignmentOverlay`: Pre-alignment poster guidance and retry/simulate actions.
- `ActiveNavigationOverlay`: Top guidance and bottom HUD during route following.
- `InstructionBanner`: Next-action card.
- `BottomHud`: Remaining distance, ETA, and end route action.
- `ArrivalOverlay`: Spring-animated arrival card.

## Important Logic
- Calls `arViewModel.configure(...)` when route package changes.
- Uses `AndroidView` to bridge Compose and `GLSurfaceView` without returning to an Activity-based AR screen.
- Handles lifecycle by resuming/pausing AR session with the current lifecycle owner.
- Bottom HUD ETA uses remaining distance at 1.2 m/s.

## Uses
- `AndroidArNavigationViewModel`
- `AndroidNavigationFlowModel`
- `ArCoreCameraRenderer`

## Used By
- `AndroidNavigationApp.kt`: Rendered for `FlowState.ArNavigation`.

## Related Tests
- None.
