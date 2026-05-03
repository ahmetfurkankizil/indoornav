# File Dossier: DijkstraRouteEngineTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/DijkstraRouteEngineTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `route_finding`
- **Status**: Mapped

## Role
Validates the shortest-path routing logic using Dijkstra's algorithm. It ensures that the pathfinding engine correctly navigates different graph topologies, respects edge costs, and handles directional constraints.

## Public Surface
- `DijkstraRouteEngineTest`: Test class.

## Main Symbols
- `dijkstra()`: A standalone implementation of Dijkstra's algorithm for testing purposes, mirroring the production logic.
- `TestNode`, `TestEdge`, `TestPath`: Local data structures for building test graphs.

## Important Logic
- **Diamond Graph Shortest Path** (L65-71): Verifies that the engine chooses the path with the lowest cumulative cost (5.0 vs 6.0) in a diamond-shaped network.
- **Reachability & Failure Handling** (L82-103): Ensures the engine returns `null` for unreachable nodes or non-existent start/end points.
- **Directionality Verification** (L106-126): Confirms that bidirectional edges allow two-way traversal while unidirectional edges strictly enforce one-way movement.
- **Identity Case** (L74-79): Validates that a path from a node to itself has zero cost and contains only that node.

## Related Features
- `route_finding`: This is the core logic engine for finding navigation paths between rooms.

## Notes / Risks
- **Algorithm Mirroring**: By re-implementing Dijkstra in the test file, the suite provides a "clean room" verification of the algorithm. If the production implementation in the KMP shared module deviates from this tested logic, integration tests will detect the discrepancy.
- **Performance**: While testing small graphs, this confirms the logical correctness of the greedy search strategy.
