# Feature: Pathfinding

## Purpose
Calculates the shortest navigable route between two points in an indoor environment based on a mapping graph.

## Implemented In
- `app/src/main/java/com/example/vecturai/graph/Pathfinder.kt`
- `app/src/main/java/com/example/vecturai/graph/MapGraph.kt`

## Used By
- Navigation Subsystem

## Main Flow
1. Receives a `MapGraph` containing nodes and edges.
2. Constructs a bidirectional adjacency list.
3. Uses A* algorithm (weighted Dijkstra) to find the sequence of `MapNode` objects.
   - **Cost Factors:** Includes physical distance, turn penalties (`TURN_COST_METERS_PER_RADIAN`), and edge type multipliers (Stairs/Elevators).
4. **Path Smoothing:** Drops redundant intermediate nodes if they don't have labels and don't deviate significantly from a straight line.
5. Returns the optimized list for navigation guidance.

## Key Symbols
- `Pathfinder.shortestPath()`
- `Pathfinder.edgeCost()`
- `Pathfinder.smoothPath()`

## Config / Env / Flags
- `TURN_COST_METERS_PER_RADIAN`: Penalizes sharp turns to prefer straight corridors.
- `STAIRS_MULTIPLIER`: Increases cost of using stairs.
- `SMOOTHING_MAX_DEVIATION_M`: Threshold for dropping intermediate nodes.

## Data Structures / Protocols
- `MapGraph`: The underlying data model.
- `SearchState`: Tracks node and incoming direction for turn-cost calculation.

## Related Tests
- [PathfinderTest.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt)

## Related File Dossiers
- [MapGraph.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_graph_MapGraph.kt.md)
- [Pathfinder.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_graph_Pathfinder.kt.md)

## Risks / Notes
- Smoothing might over-simplify paths in very tight zigzag environments.
- Vertical costs (Stairs/Elevators) are static multipliers.
