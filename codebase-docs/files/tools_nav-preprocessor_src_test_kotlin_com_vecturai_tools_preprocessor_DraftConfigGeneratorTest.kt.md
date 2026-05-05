# File Dossier: DraftConfigGeneratorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/DraftConfigGeneratorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the `DraftConfigGenerator`, which is the final stage of the automated drafting pipeline. It ensures that analyzed spatial data (zones, graph, floor estimates) is correctly assembled into a valid `AuthoringConfig` JSON file that can be subsequently edited by an admin.

## Public Surface
- `DraftConfigGeneratorTest`: Test class.

## Important Logic
- **JSON Generation & Validity** (L23-75): Confirms that the generator produces a file that is not only syntactically correct JSON but also semantically valid according to the `AuthoringConfig` schema.
- **Heuristic labeling** (L65, L79-123): Validates the logic that decides which zones become "rooms" vs "corridors" (usually based on size/connectivity) and ensures they are assigned neutral, placeholder labels like "Zone A".
- **Metadata Persistence** (L71): Verifies that a `generation_metadata.json` file is created alongside the config, providing auditability for the automated run.
- **Resource Cleanup** (L73-75, L121-123): Uses `try-finally` blocks to ensure temporary test directories are deleted after each run.

## Uses
- `DraftConfigGenerator`: The component being tested.
- `NavigationGraphDrafter.DraftNavGraph`: Source data.
- `ZoneSuggester.Zone`: Source data.
- `FloorPlaneEstimator.FloorEstimate`: Source data.
- `AuthoringConfig`: Target data model.

## Related Features
- `preprocessing`: This test covers the "serialization of analyzed state" phase of the preprocessor.

## Notes / Risks
- **Heuristic Changes**: If the internal logic for identifying rooms vs corridors changes (e.g., area threshold), these tests will need to be updated to match the new expectations.
