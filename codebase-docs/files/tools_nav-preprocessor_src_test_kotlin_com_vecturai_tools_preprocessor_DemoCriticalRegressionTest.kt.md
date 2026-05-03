# File Dossier: DemoCriticalRegressionTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/DemoCriticalRegressionTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `preprocessing`, `route_finding`, `navigation_session_management`
- **Status**: Mapped

## Role
A high-level integration-style test suite that validates the entire end-to-end "Demo" scenario. It ensures that the core features required for a successful live demonstration—from building configuration to pathfinding and arrival detection—function correctly in unison.

## Public Surface
- `DemoCriticalRegressionTest`: Test class.

## Main Symbols
- `demoConfig()`: Generates a realistic 5-node building configuration matching the repository's sample building.
- `Pt`: Local coordinate data class for progress calculations.

## Important Logic
- **Full Pipeline Validation** (L56-87): Confirms that the `demoConfig` is structurally valid, graph-consistent (connectivity), and serializes without data loss.
- **Semantic Discovery** (L91-106): Tests room search capability by display name and multilingual aliases (e.g., "Kitchen" vs "Mutfak").
- **Dijkstra Pathfinding** (L112-154): Implements a local Dijkstra solver to verify that the shortest path through the demo building correctly visits all waypoints in sequence.
- **Progress Monotonicity** (L158-213): Validates the progress estimation algorithm, ensuring that progress increases as the user moves along the route and—crucially—never regresses even if the user moves backward or sensors drift.
- **Arrival Logic** (L217-233): Re-validates the dual-trigger arrival detection (progress > 95% OR distance < 1.5m).

## Related Features
- `route_finding`: Tested via the Dijkstra logic.
- `navigation_session_management`: Tested via progress and arrival logic.
- `search_discovery`: Tested via room alias matching.

## Notes / Risks
- **Integration Coverage**: This is the single most important test for preventing breaking changes to the core navigation experience. It effectively acts as a "Smoke Test" for the entire project.
- **Simplified Algorithms**: Like other preprocessor tests, it uses local implementations of key algorithms (Dijkstra, Progress projection) to validate the expected behavior against a fixed building model.
