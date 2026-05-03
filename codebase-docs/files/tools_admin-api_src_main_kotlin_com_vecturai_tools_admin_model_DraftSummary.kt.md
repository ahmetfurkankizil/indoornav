# File Dossier: DraftSummary.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/Vectura AI/tools/admin/model/DraftSummary.kt`
- **Type**: Kotlin Source (Data Model)
- **Feature**: `admin_orchestration`, `preprocessing`
- **Status**: Mapped

## Role
Provides a high-level summary of a navigation draft, aggregating metadata from various pipeline outputs (authoring config, occupancy grid, geometry stats). It is designed to be consumed by the admin UI to display job results and allow for manual refinement.

## Main Symbols
- `DraftSummary`: Root model for the job summary.
- `ArtifactAvailability`: Flags indicating which output files were successfully generated.
- `GraphCounts`: Statistics on the generated navigation graph (nodes, edges, etc.).
- `DraftRoom`: Simplified view of a room in the draft.
- `GenerationMetadataSummary`: Heuristics and confidence scores from the extraction process.
- `GeometryStatsSummary`: Spatial and mesh statistics for the building asset.

## Important Logic
- **Data Aggregation** (L6-18): Combines identity information (`buildingId`), counts, room lists, and warnings into a single object.
- **Preprocessing Metrics** (L48-68): Exposes internal parameters like `floorY`, `occupancyGridWidth`, and `zoneCount` to help admin users judge the quality of the automated extraction.

## Uses
- `kotlinx.serialization.Serializable`: For JSON serialization to the frontend.

## Used By
- `DraftSummaryExtractor`: Populates this model from raw files.
- `DraftJobRoutes`: Returns the summary for a specific job.

## Related Features
- `preprocessing`: Exposes the results of the preprocessing pipeline.
- `admin_orchestration`: Used to present job outcomes to users.

## Notes / Risks
- **UI Dependency**: This model is the primary data contract for the Admin Dashboard. Any changes here directly affect the UI rendering of job results.
- **Optional Fields**: Many fields are nullable or have defaults, reflecting the fact that different jobs may produce different subsets of data based on their inputs.
