# File Dossier: NavigationScreen.kt

## Path
app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt

## Type
source

## Role
AR UI for real-time indoor navigation guidance.

## Imports / Includes
- `io.github.sceneview.ar.ARSceneView`
- `com.example.vecturai.ar.ArrowRenderer`
- `com.example.vecturai.ui.CameraPermissionGate`

## Exports / Public Surface
- `NavigationRoute` (Composable): Entry point with ViewModel injection.
- `NavigationScreen` (Composable): Main UI state machine.

## Main Symbols
- `NavigationArScene`: Renders the path markers and the floating navigation arrow.
- `NavigationDiagnostics`: Debug panel for tracking status and distances.
- `BuildingPicker`: UI for selecting which saved graph to load.
- `ArrivedPanel`: Final UI state upon reaching the destination.

## Important Logic by Line Range
- L81-107: `NavigationPhase` state machine driving the visibility of different UI panels.
- L141-195: `ARSceneView` integration.
- L171-172: `isSmoothTransformEnabled` configuration for the 3D arrow to reduce jitter.
- L182-192: Fallback rendering logic if `arrow.glb` asset is missing (renders a Cube + Sphere composition).

## Uses
- `NavigationViewModel.kt`
- `ArSessionConfig.kt`
- `SceneView` library
- `ArAssetUtils.kt` [Pending Mapping]

## Used By
- `MainActivity.kt` (Route mapping)

## Config / Constants / Protocol Details
- Arrow Model: `arrow.glb` (expected in assets).
- Color coding: Cyan (0xFF00BCD4) for resolved anchors, Amber (0xFFFFC107) for estimated positions.

## Related Tests
N/A

## Notes / Risks
- Asset validation via `hasValidGlbAsset` is crucial to prevent `SceneView` crashes.
- Relies on `PoseNode` for placing markers in 3D space.
