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
- `NavigationOverlay`: Displays the distance to destination and current waypoint name.
- `RelocalizeButton`: Triggers manual re-computation of the graph fit.

## Important Logic by Line Range
- L45-120: Screen layout and phase-based navigation (Selecting building -> Localizing -> Navigating).
- L140-160: Integration with `NavigationViewModel` to drive AR model positions.

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
