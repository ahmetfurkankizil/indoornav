# File Dossier: NavigationScreen.kt

## Path
app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt

## Type
source

## Role
Main UI component for the navigation experience, displaying the AR view and guidance overlays.

## Imports / Includes
- `com.example.vecturai.ui.navigation.NavigationViewModel`
- `androidx.compose.runtime.collectAsState`

## Exports / Public Surface
- `NavigationScreen` (Composable)

## Main Symbols
- `NavigationScreen`: Host composable that switches content based on `uiState.phase`.
- `NavigationArScene`: Renders node spheres and the 3D arrow using SceneView.
- `NavigationDiagnostics`: Top-center overlay with tracking info and relocalize controls.

## Important Logic by Line Range
- L74-126: Screen layout and phase-based navigation host.
- L180-261: `ARSceneView` integration and rendering of node spheres and the arrow.

## Uses
- `NavigationViewModel.kt`
- `ArAssetUtils.kt`

## Used By
- `MainActivity.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- UI is highly dynamic; many components are hidden/shown based on tracking state.
- Guidance text reflects the `activePath` current node label.
