# File Dossier: ArContractSerializationTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/ArContractSerializationTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_alignment`, `navigation_data_format`
- **Status**: Mapped

## Role
Validates the JSON serialization contract for AR-related models. It ensures that data structures used for AR rendering (arrows, routes, transforms) can be reliably serialized and deserialized between the preprocessor, admin tools, and mobile applications.

## Public Surface
- `ArContractSerializationTest`: Test class.

## Main Symbols
- `TestArrowPlacement`: Mirror of the production AR arrow model.
- `TestArRenderableRoute`: Mirror of the AR route model.
- `TestAlignmentTransform`: Mirror of the coordinate transform model.
- `TestMarkerAlignmentResult`: Mirror of the marker detection result model.

## Important Logic
- **Roundtrip Serialization** (L65-130): Verifies that encoding a model to JSON and then decoding it back results in an identical object. This prevents data loss in the pipeline.
- **Default Value Verification** (L132-149): Ensures that the JSON parser correctly applies default values when optional fields (like `forwardDy` or `label`) are missing from the input JSON.

## Uses
- `kotlinx.serialization`: For JSON processing.

## Related Features
- `navigation_data_format`: Defines the shared contract.
- `ar_alignment`: Uses these models for spatial state.

## Notes / Risks
- **Mirrored Models**: Like other tests in this suite, it uses local `Test*` classes instead of production ones. This is a design pattern here to strictly validate the **schema** itself as a stable contract, regardless of implementation details in the main codebase.
