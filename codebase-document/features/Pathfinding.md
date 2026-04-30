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
3. Uses Dijkstra's algorithm to find the sequence of `MapNode` objects from the start to the destination.
4. Returns the ordered list for navigation guidance.

## Key Symbols
- `Pathfinder.shortestPath()`
- `MapEdge.distanceMeters`

## Config / Env / Flags
N/A

## Data Structures / Protocols
- `MapGraph`: The underlying data model.
- `PriorityQueue`: Used for efficient node selection in Dijkstra.

## Related Tests
- [PathfinderTest.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt)

## Related File Dossiers
- [MapGraph.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_graph_MapGraph.kt.md)
- [Pathfinder.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_src_main_java_com_example_vecturai_graph_Pathfinder.kt.md)

## Risks / Notes
- The pathfinder assumes a static graph; if the environment changes, a new map must be created.
- Unreachable goals return a null path.
