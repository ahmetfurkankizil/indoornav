# File Dossier: RoomOverride.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/VecturAI/tools/admin/model/RoomOverride.kt`
- **Type**: Kotlin Source (Data Model)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Defines the models for manual metadata overrides. These allow admin users to correct or enhance the automatically generated room names, categories, and descriptions before final export.

## Main Symbols
- `RoomOverride`: A single correction for a room (L10-15).
- `RoomOverrides`: A map-based container for all overrides in a job (L21-23).
- `RoomPatchRequest`: Input model for updating a room's metadata via API (L29-33).
- `ExportResult`: Response model describing the outcome of a package export (L39-45).

## Important Logic
- **Partial Updates** (L11-13): Uses nullable fields to support merging. Only non-null fields replace values in the generated draft.
- **Export Context** (L42-43): Tracks which files were exported and where they were saved.

## Uses
- `kotlinx.serialization.Serializable`: For JSON persistence and API communication.

## Used By
- `RoomOverrideService`: Manages the lifecycle of overrides.
- `DraftJobRoutes`: Uses `RoomPatchRequest` and `ExportResult` for HTTP endpoints.
- `ReviewedPackageExporter`: Applies these overrides during the final transformation.

## Related Features
- `admin_orchestration`: Part of the "Review and Correct" workflow.

## Notes / Risks
- **Data Integrity**: Overrides are keyed by `roomId`. If the underlying draft is regenerated and IDs change, the overrides may become orphaned or incorrectly applied.
