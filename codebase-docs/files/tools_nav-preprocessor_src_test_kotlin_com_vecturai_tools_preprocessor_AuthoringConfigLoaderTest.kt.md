# File Dossier: AuthoringConfigLoaderTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/AuthoringConfigLoaderTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`, `navigation_data_format`
- **Status**: Mapped

## Role
Tests the validation logic within the `AuthoringConfigLoader`. It ensures that the preprocessor correctly identifies and rejects malformed or incomplete building configurations before they are used for navigation package export.

## Public Surface
- `AuthoringConfigLoaderTest`: Test class.

## Main Symbols
- `validConfig()`: Helper method that returns a minimally valid `AuthoringConfig` object.

## Important Logic
- **Structural Integrity Checks** (L28-70): Validates that mandatory top-level fields (ID, name, asset reference) and lists (nodes, edges, rooms, markers) are present.
- **Node/Edge Validation** (L73-81): Ensures that internal objects like nodes do not have blank IDs.
- **Marker Constraint Validation** (L84-101): Checks that physical dimensions (width/height) are positive and that the orientation basis is one of the four supported values.
- **Rendering Parameter Bounds** (L104-109): Verifies that visual configuration values (like arrow spacing) are within safe, usable ranges.
- **File System Handling** (L112-116): Ensures that the loader correctly throws a `ValidationException` when attempting to load a non-existent file.

## Uses
- `AuthoringConfigLoader`: The component being tested.
- `AuthoringConfig` model classes.

## Related Features
- `preprocessing`: The core workflow this loader supports.
- `navigation_data_format`: The schema being validated.

## Notes / Risks
- **Heuristic Thresholds**: Validates specific thresholds (e.g., `arrowSpacingMeters >= 0.3`) which are defined in the production code. If these thresholds change in the production `validateStructure` method, this test will correctly fail.
