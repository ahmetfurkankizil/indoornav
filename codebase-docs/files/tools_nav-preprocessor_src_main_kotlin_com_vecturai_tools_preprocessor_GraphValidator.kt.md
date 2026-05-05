# File Dossier: GraphValidator.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/VecturAI/tools/preprocessor/GraphValidator.kt`
- **Type**: Kotlin Source (Validation Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Performs deep structural and logical validation of the navigation graph. It ensures that the building data is safe to be consumed by the mobile apps' pathfinding and AR rendering engines.

## Public Surface
- `validate(config: AuthoringConfig): ValidationResult`: Returns a list of fatal errors and non-fatal warnings.

## Important Logic
- **Referential Integrity** (L48-75): Ensures every edge, room, and marker references a node ID that actually exists in the `nodes` list.
- **Connectivity Analysis** (L108-125): Uses BFS (Breadth-First Search) to verify that every room in the building is reachable from at least one entrance marker. This prevents "island" nodes that would cause pathfinding failures.
- **Duplicate Detection** (L32-45): Checks for ID collisions across nodes, edges, and rooms.
- **Orphan Detection** (L128-136): Warns about nodes that are not connected to any edges, which might indicate accidental deletions or incomplete drafting.

## Used By
- `Pipeline.kt`: Step 3 of the production flow.

## Notes / Risks
- **BFS Starting Point**: Connectivity is checked per entrance; a building with multiple disconnected floor sections will fail validation unless each section has its own entrance marker.
- **Positive Costs**: Strictly enforces positive edge costs to prevent infinite loops or crashes in Dijkstra's algorithm.
