# File Dossier: Pathfinder.kt

## Path
app/src/main/java/com/example/vecturai/graph/Pathfinder.kt

## Type
source

## Role
Shortest path calculation engine.

## Imports / Includes
- `java.util.PriorityQueue`

## Exports / Public Surface
- `Pathfinder` (class)
- `shortestPath(startId, goalId)` (function)

## Main Symbols
- `adjacentEdges` (property): Bidirectional adjacency list built from the graph's edges.
- `shortestPath` (function): Implementation of Dijkstra's algorithm.

## Important Logic by Line Range
- L7-15: Adjacency list construction, ensuring all edges are treated as bidirectional.
- L23-39: Dijkstra core loop using a `PriorityQueue` with `compareBy`.
- L42-43: Path reconstruction from the `previous` map.

## Uses
- `MapGraph.kt`

## Used By
- `NavigationViewModel.kt`

## Config / Constants / Protocol Details
N/A

## Related Tests
- `app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt`

## Notes / Risks
- Returns `null` if no path is found or if start/goal IDs are invalid.
- Efficiency is $O(E \log V)$ which is suitable for indoor mapping graphs.
