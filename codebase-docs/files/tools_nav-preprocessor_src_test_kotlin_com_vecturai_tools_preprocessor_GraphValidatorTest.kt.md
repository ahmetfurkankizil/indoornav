# File Dossier: GraphValidatorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/GraphValidatorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the `GraphValidator` component, which performs critical integrity and connectivity checks on the navigation graph. It ensures that the graph is free of structural errors (duplicates, dangling references) and that all destinations are reachable from building entry points.

## Public Surface
- `GraphValidatorTest`: Test class.

## Main Symbols
- `validate()`: The primary method under test.
- `validConfig()`: Helper to create a structurally sound baseline configuration.

## Important Logic
- **Referential Integrity** (L73-105): Checks that edges, rooms, and markers all point to existing node IDs.
- **Uniqueness Constraints** (L46-70): Ensures that both node and edge IDs are unique within the configuration.
- **Physical Bounds** (L108-131): Rejects edges with non-positive costs (distance), which would break Dijkstra pathfinding.
- **Topological Integrity** (L134-145): Rejects self-loops (edges from a node to itself), which are considered invalid in this navigation model.
- **Connectivity Analysis** (L148-177): Crucial test that uses BFS to verify that every room is reachable from at least one entrance marker. It also confirms that unidirectional edges are correctly handled during this reachability check.

## Uses
- `GraphValidator`: The component being tested.
- `AuthoringConfig` and related models.

## Related Features
- `preprocessing`: This is the final safety check before exporting a production package.

## Notes / Risks
- **Reachability**: This is the most complex validation. A failure here prevents the "Export" stage from proceeding, which is the desired behavior to prevent "broken" navigation packages from reaching mobile clients.
