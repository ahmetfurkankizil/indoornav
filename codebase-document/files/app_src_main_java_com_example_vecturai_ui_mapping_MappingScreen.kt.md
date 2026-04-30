# File Dossier: MappingScreen.kt

## Path
app/src/main/java/com/example/vecturai/ui/mapping/MappingScreen.kt

## Type
source

## Role
AR UI for creating indoor navigation maps.

## Imports / Includes
- `io.github.sceneview.ar.ARSceneView`
- `com.example.vecturai.ar.ArSessionConfig`
- `com.example.vecturai.ui.CameraPermissionGate`

## Exports / Public Surface
- `MappingRoute` (Composable): Entry point with ViewModel injection.
- `MappingScreen` (Composable): Main UI layout.

## Main Symbols
- `MappingArScene`: Integrates `SceneView` with custom `PoseNode` and `SphereNode` markers.
- `MappingTopPanel`: Controls for building metadata and tracking status.
- `MappingBottomPanel`: Controls for anchor placement and saving.
- `TagRoomDialog`: Modal for labeling the most recent anchor.

## Important Logic by Line Range
- L66-105: Main layout using `Box` to overlay AR scene with panels and error messages.
- L135-157: `ARSceneView` configuration and marker rendering. Uses different colors/sizes for labeled vs. unlabeled nodes.
- L140: Configures the AR session using `ArSessionConfig::configureIndoorCloudSession`.

## Uses
- `MappingViewModel.kt`
- `ArSessionConfig.kt`
- `SceneView` library

## Used By
- `MainActivity.kt` (Route mapping)

## Config / Constants / Protocol Details
- Color coding: Green (0xFF1DB954) for waypoints, Red (0xFFE53935) for tagged rooms.

## Related Tests
N/A

## Notes / Risks
- Relies on `CameraPermissionGate` for AR session stability.
- Uses `rememberMaterialLoader` for efficient model/material management.
