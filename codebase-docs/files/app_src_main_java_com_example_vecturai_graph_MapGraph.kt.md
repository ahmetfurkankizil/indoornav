# File Dossier: MapGraph.kt

## Path
app/src/main/java/com/example/vecturai/graph/MapGraph.kt

## Type
source

## Role
Data models for the indoor navigation graph.

## Imports / Includes
- `com.google.ar.core.Pose`
- `kotlinx.serialization.Serializable`

## Exports / Public Surface
- `MapGraph`: Root object containing nodes and edges.
- `MapNode`: Individual points in space with Cloud Anchor IDs and labels.
- `MapEdge`: Connections between nodes with distances.

## Main Symbols
- `MapGraph` (data class)
- `MapNode` (data class)
- `MapEdge` (data class)
- `labeledNodes` (property): Filters nodes that have labels.
- `graphPose()` (function): Converts node coordinates/quaternion to an ARCore `Pose`.

## Important Logic by Line Range
- L7-12: `MapGraph` definition.
- L18-30: `MapNode` definition, including coordinate and orientation properties.
- L31-34: Pose conversion logic.
- L38-42: `MapEdge` definition.

## Uses
- ARCore: For `Pose` representation.

## Used By
- `Pathfinder.kt`
- `GraphRepository.kt`
- `MappingViewModel.kt`
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- Uses Kotlinx Serialization for JSON representation.

## Related Tests
- `app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt`

## Notes / Risks
- Coordinates are relative to a local origin established during mapping.
- Orientation (quaternion) is stored for each node.
