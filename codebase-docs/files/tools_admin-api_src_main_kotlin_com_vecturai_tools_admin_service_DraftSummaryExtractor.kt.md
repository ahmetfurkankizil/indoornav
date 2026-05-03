# File Dossier: DraftSummaryExtractor.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/Vectura AI/tools/admin/service/DraftSummaryExtractor.kt`
- **Type**: Kotlin Source (Service)
- **Feature**: `admin_orchestration`, `preprocessing`
- **Status**: Mapped

## Role
Responsible for parsing the various JSON and SVG artifacts produced by the `nav-preprocessor` and aggregating them into a unified `DraftSummary` object. It handles missing or malformed files gracefully by logging warnings instead of throwing exceptions.

## Main Symbols
- `DraftSummaryExtractor`: Core extractor class.
- `extract()`: The primary entry point that reads files from an output directory and merges them with user overrides.

## Important Logic
- **Artifact Availability** (L30-36): Checks for the physical existence of key files like occupancy SVGs and config JSONs.
- **Config Parsing** (L45-95):
    - Extracts building metadata (ID, Name, Floor).
    - Calculates graph statistics (Node/Edge/Marker counts).
    - Maps rooms to `DraftRoom` objects, automatically applying any user-provided `overrides` (L62-73).
- **Metadata & Stats Extraction** (L97-138): Parses `generation_metadata.json` (pipeline diagnostics) and `geometry_stats.json` (mesh and grid statistics).
- **Warning Generation** (L84-91, L141-146): Accumulates a list of human-readable warnings if the draft is incomplete or files are corrupted.

## Uses
- `DraftSummary`, `DraftRoom`, `ArtifactAvailability`: The target models.
- `kotlinx.serialization.json`: For manual JSON traversal without full class mapping.

## Used By
- `DraftJobService`: Uses this to generate the summary returned to the admin dashboard.

## Related Features
- `preprocessing`: Validates the output of the preprocessing pipeline.
- `admin_orchestration`: Prepares data for the "Review" phase of the admin workflow.

## Notes / Risks
- **Heuristic Sensitivity**: Relies on specific filenames (`authoring_config.generated.json`, etc.) produced by the `nav-preprocessor`.
- **Manual JSON Traversal**: Uses `jsonObject` access rather than `decodeFromString` for the internal draft files to remain resilient to schema changes in those files.
