# File Dossier: DestinationPicker.kt

## Path
app/src/main/java/com/example/vecturai/ui/navigation/DestinationPicker.kt

## Type
source

## Role
UI component for selecting navigation targets within a building graph.

## Imports / Includes
- `androidx.compose.foundation.lazy.LazyColumn`
- `com.example.vecturai.ui.navigation.NavigationUiState`

## Exports / Public Surface
- `DestinationPicker` (Composable)

## Main Symbols
- `DestinationPicker`: Renders a list of labeled nodes as selection buttons.

## Important Logic by Line Range
- L33-35: Empty state handling for graphs with no labeled rooms.
- L40-47: `LazyColumn` rendering of labeled `MapNode` objects.

## Uses
- `MapNode.kt`

## Used By
- `NavigationScreen.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Relies on `graph.labeledNodes` filtering in the ViewModel.
