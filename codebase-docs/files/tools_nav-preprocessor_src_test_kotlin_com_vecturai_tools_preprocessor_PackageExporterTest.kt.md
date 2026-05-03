# File Dossier: PackageExporterTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/PackageExporterTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`, `navigation_data_format`
- **Status**: Mapped

## Role
Tests the `PackageExporter`, which is responsible for transforming the `AuthoringConfig` (internal admin format) into the optimized JSON "package" files consumed by the mobile clients. It ensures that data is correctly split across functional files and that specific client-side requirements (like QR payload generation) are met.

## Public Surface
- `PackageExporterTest`: Test class.

## Main Symbols
- `export()`: The primary method under test.
- `sampleConfig()`: Helper to create a realistic configuration for export testing.

## Important Logic
- **File Structure Validation** (L32-46): Verifies that a single export run produces the full suite of required client files: `nav_graph.json`, `rooms.json`, `entrance_markers.json`, `route_rendering.json`, and `manifest.json`.
- **Data Transformation** (L49-89):
    - Confirms `nav_graph.json` contains the correct node/edge counts and building ID.
    - Validates that `rooms.json` preserves search metadata (keywords).
    - Checks that `entrance_markers.json` includes the auto-generated `qrPayload` string, which encodes the building and marker IDs for client-side scanning.
- **Safety Checks** (L92-101): Ensures the exporter respects the `overwrite` flag, failing if the target directory already exists and overwrite is disabled.

## Uses
- `PackageExporter`: The component being tested.
- `PackageNavGraph`, `PackageRooms`, `PackageMarkers`: The target data models for mobile clients.

## Related Features
- `navigation_data_format`: This test defines the "source of truth" for the client-side data schema.

## Notes / Risks
- **Schema Drift**: Any changes to the JSON structure in the exported files will break mobile clients. These tests serve as a critical gatekeeper for the data contract between the preprocessor and the apps.
