# File Dossier: ContractBackwardCompatTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/ContractBackwardCompatTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_data_format`
- **Status**: Mapped

## Role
Ensures that the navigation data JSON contract remains backward compatible. Specifically, it verifies that adding new features (like `checkpointMarkers`) does not break the ability to parse older configuration files that lack these fields.

## Public Surface
- `ContractBackwardCompatTest`: Test class.

## Important Logic
- **Default Field Injection** (L25-56): Validates that when a JSON file without the `checkpointMarkers` field is loaded, the resulting `AuthoringConfig` object correctly initializes it with an empty list instead of failing or using null.
- **Full Field Roundtrip** (L118-140): Confirms that the `AuthoringCheckpointMarker` correctly serializes and deserializes all its optional and mandatory fields (rotation, reference images, notes).
- **Validation Stability** (L92-115): Ensures that the `GraphValidator` still accepts configurations that only use the original `entranceMarkers` and lack checkpoints.

## Uses
- `AuthoringConfig` and related model classes.
- `kotlinx.serialization`: For JSON testing.
- `GraphValidator`: To verify behavioral compatibility.

## Related Features
- `navigation_data_format`: The core focus of this test.

## Notes / Risks
- **Schema Evolution**: This test is critical during the "Authoring v2" transition to ensure that buildings mapped with older versions of the tool can still be processed without manual JSON editing.
