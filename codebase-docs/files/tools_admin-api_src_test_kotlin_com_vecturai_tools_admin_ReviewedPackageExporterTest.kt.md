# File Dossier: ReviewedPackageExporterTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/VecturAI/tools/admin/ReviewedPackageExporterTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `admin_orchestration`, `navigation_data_format`
- **Status**: Mapped

## Role
Verifies the final export stage of the Admin tool. It ensures that the generated 5-file navigation bundle adheres to the required schema, correctly merges manual metadata corrections, and handles various edge cases like missing draft artifacts or re-running exports.

## Main Symbols
- `ReviewedPackageExporterTest`: Test suite class.
- `export produces all 5 required files`: Checks for existence of `manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, and `route_rendering.json`.
- `exported rooms use override...`: Validates the critical merge logic between draft data and user corrections.
- `exported manifest has correct buildingId...`: Ensures the package metadata is consistent with the source building.

## Important Logic
- **Synthetic Authoring Config** (L31-48): Generates a minimal valid navigation graph JSON on disk to serve as the input for export tests.
- **Merge Validation** (L79-92, L125-136): Rigorously checks that user-provided `RoomOverrides` replace draft values correctly, and that re-exporting the same job updates the files on disk.
- **Fallback Verification** (L116-122): Confirms that rendering parameters are preserved across the export process.

## Uses
- `ReviewedPackageExporter`: The service under test.
- `RoomOverrides`, `RoomOverride`: Input models for corrections.
- `kotlinx.serialization.json`: For inspecting exported file contents.

## Used By
- CI/CD: Automated verification of the final data bundle generation.

## Related Features
- `navigation_data_format`: Defines the schema that the exporter must produce.
- `admin_orchestration`: Marks the completion of the admin review workflow.

## Notes / Risks
- **Schema Lock-in**: Like the exporter itself, these tests enforce the exact JSON structure expected by client apps. Any change here likely implies a breaking change for the mobile clients.
