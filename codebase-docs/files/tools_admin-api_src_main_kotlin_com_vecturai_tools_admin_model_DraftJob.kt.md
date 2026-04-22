# File Dossier: DraftJob.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/vecturai/tools/admin/model/DraftJob.kt`
- **Type**: Kotlin Source (Data Model)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Defines the state and metadata of a preprocessing job. This model tracks the lifecycle of transforming a raw GLB upload into a navigation draft.

## Main Symbols
- `DraftJob`: Data class representing a processing task.
- `JobStatus`: Enum for the job state machine (`queued`, `processing`, `succeeded`, `failed`).

## Important Logic
- **Job Metadata** (L6-14):
    - `id`: Unique identifier for the job.
    - `originalFilename`: Name of the uploaded GLB asset.
    - `artifacts`: List of generated files (JSON, SVG, etc.) available after success.
    - `status`: Current execution state.

## Uses
- `kotlinx.serialization.Serializable`: For JSON persistence.

## Used By
- `DraftJobService`: Manages collections of jobs.
- `DraftJobRoutes`: Returns job status to the client.

## Related Features
- `admin_orchestration`: This is the primary unit of work in the orchestration layer.

## Notes / Risks
- **Persistence**: Typically serialized to `job.json` within the job directory.
- **Timestamps**: Uses strings for `createdAt` and `updatedAt`, which requires consistent ISO-8601 formatting across the service.
