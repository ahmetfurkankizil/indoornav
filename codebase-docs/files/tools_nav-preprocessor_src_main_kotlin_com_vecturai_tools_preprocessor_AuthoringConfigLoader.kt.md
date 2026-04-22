# File Dossier: AuthoringConfigLoader.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/vecturai/tools/preprocessor/AuthoringConfigLoader.kt`
- **Type**: Kotlin Source (Serialization Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Handles the loading, deserialization, and structural validation of the `AuthoringConfig` JSON files. It ensures that the human-refined configuration meets the minimum data requirements before further processing.

## Public Surface
- `load(configPath: String): AuthoringConfig`: Deserializes JSON from disk.
- `validateStructure(config: AuthoringConfig): List<String>`: Performs a comprehensive field-by-field check for mandatory values and valid ranges.

## Important Logic
- **Deserialization** (L37-41): Uses `kotlinx.serialization` with `ignoreUnknownKeys = true` to allow for forward-compatibility in the JSON schema.
- **Structural Constraints** (L52-59): Enforces the presence of a Building ID, Asset reference, and at least one room, node, edge, and entrance marker.
- **Value Range Checks** (L88-92): Validates that rendering parameters (arrow spacing, lookahead distance) are within safe, usable bounds.

## Used By
- `Pipeline.kt`: Step 2 of the production flow.

## Notes / Risks
- **ID Validation**: Checks that IDs are not blank, but does not check for cross-references; that is handled by the `GraphValidator`.
- **Forward Basis**: Strictly validates that marker orientation is one of the four supported bases (`+x`, `-x`, `+z`, `-z`).
