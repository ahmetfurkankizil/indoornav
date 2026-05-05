# File Dossier: ReviewedPackageExporter.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/VecturAI/tools/admin/service/ReviewedPackageExporter.kt`
- **Type**: Kotlin Source (Service)
- **Feature**: `admin_orchestration`, `navigation_data_format`
- **Status**: Mapped

## Role
Generates the final, client-ready navigation package by merging automatically generated draft data with user-provided room metadata overrides. It produces a 5-file bundle (`manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, `route_rendering.json`) that mobile apps can consume.

## Main Symbols
- `ReviewedPackageExporter`: Core exporter class.
- `export()`: Processes a job's artifacts and overrides to create the reviewed package.

## Important Logic
- **Merge & Patch** (L72-108): Iterates through rooms from the generated authoring config and applies user overrides for names, categories, and descriptions.
- **Manifest Generation** (L48-70): Creates a package manifest tracking versioning, building metadata, and internal file references.
- **Data Transformation** (L110-148):
    - Splits the unified authoring config into specialized functional files.
    - Provides fallback defaults for `route_rendering.json` if missing from the draft (L138-146).
- **Safety Checks**: Returns a failed `ExportResult` if the required `authoring_config.generated.json` is missing (L24-30) or unparseable.

## Uses
- `ExportResult`, `RoomOverrides`: Internal API models.
- `kotlinx.serialization.json`: For manipulating JSON structures during export.

## Used By
- `DraftJobService`: Triggers the export process after user review.

## Related Features
- `navigation_data_format`: Defines the final schema for client-side consumption.
- `admin_orchestration`: The final step in the admin review workflow.

## Notes / Risks
- **Schema Lock-in**: This class defines the exact format expected by mobile clients. Any breaking changes here must be coordinated with app updates.
- **Lossy Transformation**: Only specific fields are currently merged. Complex geometry edits are not yet supported via this exporter.
