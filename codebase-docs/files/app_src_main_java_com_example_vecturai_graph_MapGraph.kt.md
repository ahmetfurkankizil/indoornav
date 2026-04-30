# File Dossier: MapGraph.kt

## Path
app/src/main/java/com/example/vecturai/graph/MapGraph.kt

## Type
source

## Role
Core data model for the indoor navigation graph.

## Imports / Includes
- `kotlinx.serialization.Serializable`

## Exports / Public Surface
- `MapNode` (data class)
- `MapEdge` (data class)
- `EdgeType` (enum)
- `MapGraph` (data class)

## Main Symbols
- `MapNode`: Represents a physical point with `cloudAnchorId`, `label`, and coordinates.
- `MapEdge`: Connects two nodes with a `distanceMeters` and `type`.
- `EdgeType`: `Normal`, `Stairs`, `Elevator`.

## Important Logic by Line Range
- L10-45: Data class definitions with `@Serializable` annotations.

## Uses
N/A

## Used By
- `GraphRepository.kt`
- `Pathfinder.kt`
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Uses `cloudAnchorId` as the primary key for AR localization.

## Related Tests
N/A

## Notes / Risks
- Coordinates are relative to the graph's origin, not global GPS.
- Edge direction is logically bidirectional but represented by individual instances in many loops.
