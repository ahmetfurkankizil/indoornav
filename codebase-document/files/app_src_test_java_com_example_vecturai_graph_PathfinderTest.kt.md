# File Dossier: PathfinderTest.kt

## Path
app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt

## Type
test

## Role
Unit tests for the shortest path calculation logic.

## Imports / Includes
- `org.junit.Test`
- `com.example.vecturai.graph.Pathfinder`

## Exports / Public Surface
N/A

## Main Symbols
- `shortestPathPrefersLowestDistanceRoute`: Validates Dijkstra's weighted path selection.
- `shortestPathReturnsNullWhenDisconnected`: Validates unreachable node handling.

## Important Logic by Line Range
- L9-30: Setup of a mock graph with multiple paths to verify that the pathfinder chooses the one with the lowest total distance meters.

## Uses
- `Pathfinder.kt`
- `MapGraph.kt`

## Used By
- Gradle Test Runner

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Uses manual node/edge creation rather than loading from JSON.
