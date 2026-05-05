# File Dossier: CheckpointMarkerValidationTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/CheckpointMarkerValidationTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`, `navigation_data_format`
- **Status**: Mapped

## Role
Tests the validation logic for checkpoint markers within the `GraphValidator`. Checkpoints are used for relocalization during navigation, and this test ensures they are structurally sound and correctly linked to the navigation graph.

## Public Surface
- `CheckpointMarkerValidationTest`: Test class.

## Main Symbols
- `baseConfig()`: Helper method to create a valid baseline configuration.

## Important Logic
- **Uniqueness Verification** (L55-78): Ensures that checkpoint IDs are unique and do not overlap with entrance marker IDs.
- **Referential Integrity** (L81-89): Validates that the `nearestNodeId` for a checkpoint refers to a valid node in the graph.
- **Valid Configuration Pass** (L39-52, L92-100): Confirms that correctly configured single or multiple checkpoints pass validation.

## Uses
- `GraphValidator`: The component being tested.
- `AuthoringConfig` and `AuthoringCheckpointMarker` models.

## Related Features
- `preprocessing`: Part of the validation stage.
- `navigation_data_format`: Defines the checkpoint schema.

## Notes / Risks
- **ID Overlap**: This test specifically targets the risk of ID collisions between different marker types, which could cause ambiguity in the mobile app's relocalization engine.
