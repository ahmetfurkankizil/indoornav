# File Dossier: DemoFlowSmokeTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/DemoFlowSmokeTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`, `route_finding`
- **Status**: Mapped

## Role
Provides "smoke tests" for the repository's primary demo building data. It ensures that the hardcoded demo graph is internally consistent, fully reachable, and that basic navigation simulation on this data behaves as expected.

## Public Surface
- `DemoFlowSmokeTest`: Test class.

## Main Symbols
- `nodes`, `edges`, `rooms`, `markers`: Mirrors of the production demo building data.
- `TestNode`, `TestEdge`, `TestRoom`, `TestMarker`: Data structures for the test mirrors.

## Important Logic
- **Graph Consistency Checks** (L56-79): Validates that all edges are bidirectional and that all room/marker references point to valid node IDs.
- **Reachability Analysis** (L82-122): Uses Breadth-First Search (BFS) to confirm that key locations (Conference Room, Kitchen) are actually reachable from the entrance.
- **Path Cost Validation** (L138-148): Verifies that the expected shortest path to the Conference Room has a cumulative distance of exactly 13.0 meters.
- **Simulation Flow** (L151-165): Checks that a sequence of progress updates eventually triggers the "arrival" state within a reasonable number of steps.

## Related Features
- `preprocessing`: Validates the quality of the "gold" demo data.
- `route_finding`: Tested via reachability and cost checks.

## Notes / Risks
- **Data Mirroring**: This test effectively unit-tests the static data that ships with the app. If the demo building model is changed in production, these mirrors must be updated to match.
- **Redundant with Regression Test**: While `DemoCriticalRegressionTest` tests the *logic*, this test focuses on the *integrity of the specific demo data*.
