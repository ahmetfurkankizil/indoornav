# File Dossier: Pathfinder.kt

## Path
app/src/main/java/com/example/vecturai/graph/Pathfinder.kt

## Type
source

## Role
Shortest path calculation engine with turn-cost awareness and path smoothing.

## Imports / Includes
- `java.util.PriorityQueue`
- `kotlin.math.acos`

## Exports / Public Surface
- `Pathfinder` (class)
- `shortestPath(startId, goalId)` (function)

## Main Symbols
- `shortestPath`: Implementation of A* algorithm using `SearchState` to track direction.
- `edgeCost`: Calculates traversal cost including turn penalties (`TURN_COST_METERS_PER_RADIAN`) and edge multipliers (Stairs/Elevator).
- `smoothPath`: Iteratively removes intermediate nodes that don't significantly change the path shape.
- `SearchState`: Identifies a node combined with the direction it was entered from.

## Important Logic by Line Range
- L26-82: A* core loop with turn-cost integration via `SearchState`.
- L84-101: `edgeCost` logic calculating turn angles.
- L103-137: `smoothPath` and `canDropIntermediate` logic.

## Uses
- `MapGraph.kt`

## Used By
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
- `TURN_COST_METERS_PER_RADIAN`: 0.6m penalty per radian of turning.
- `SMOOTHING_MAX_DEVIATION_M`: 0.75m threshold for smoothing.

## Related Tests
- `app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt`

## Notes / Risks
- Turn-cost calculation only considers the horizontal component of the movement.
- Intermediate labeled nodes are never dropped by the smoother.
