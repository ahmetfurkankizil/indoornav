# File Dossier: ContractSerializationTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/ContractSerializationTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_data_format`
- **Status**: Mapped

## Role
Comprehensive validation of the serialization logic for both the intermediate `AuthoringConfig` and the final `Package*` production formats. It ensures that the preprocessor can correctly generate and consume all files in the navigation data contract.

## Public Surface
- `ContractSerializationTest`: Test class.

## Important Logic
- **AuthoringConfig Deep Roundtrip** (L16-46): Tests a complex configuration with multiple nodes, edges, rooms, rendering parameters, and metadata, ensuring every field is preserved.
- **Production Package Verification** (L49-109): Individual roundtrip tests for the four core production artifacts:
    - `PackageManifest`: Metadata and file registry.
    - `PackageNavGraph`: Nodes and edges for mobile client pathfinding.
    - `PackageRooms`: Room definitions and metadata for UI discovery.
    - `PackageMarkers`: Entrance/Checkpoint marker definitions for AR alignment.
- **Default Value Enforcement** (L112-139): Critical check that missing optional fields (like `floorId`, `forwardBasis`, or `bidirectional`) are correctly filled with their specified schema defaults during deserialization.

## Uses
- `AuthoringConfig` models.
- `PackageManifest`, `PackageNavGraph`, `PackageRooms`, `PackageMarkers` models.
- `kotlinx.serialization`: For JSON processing.

## Related Features
- `navigation_data_format`: The schema contract being tested.

## Notes / Risks
- **Completeness**: This is the primary guard against schema drift. Any change to the `model/*.kt` classes that breaks serialization will be caught here immediately.
