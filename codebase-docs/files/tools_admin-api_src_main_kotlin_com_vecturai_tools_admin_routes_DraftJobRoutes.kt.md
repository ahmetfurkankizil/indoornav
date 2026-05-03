# File Dossier: DraftJobRoutes.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/Vectura AI/tools/admin/routes/DraftJobRoutes.kt`
- **Type**: Kotlin Source (Routes)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Defines the REST API surface for the Admin tool. It handles multipart file uploads, job status queries, artifact retrieval, and room metadata correction endpoints.

## Public Surface
- `draftJobRoutes()`: Extension function to attach admin routes to a Ktor `Route` builder.

## Important Logic
- **Multipart Upload** (L24-66):
    - Receives `.glb` files via `multipart/form-data`.
    - Validates file extension and size (rejects non-GLB or empty files).
    - Triggers the async `runDraftGeneration` coroutine in a separate scope (L61).
- **Summary & Artifacts** (L96-127):
    - Provides high-level aggregated summaries.
    - Serves raw artifact content (SVG/JSON) with appropriate content types.
- **Correction Workflow** (L129-161):
    - `PATCH` endpoint for updating room metadata.
    - `POST` endpoint to finalize and export the reviewed package.
- **Export Retrieval** (L163-193): Allows listing and downloading files from the final reviewed bundle.

## Uses
- `DraftJobService`: Business logic provider.
- `RoomPatchRequest`, `ErrorResponse`: API models.
- `io.ktor.*`: Routing and multipart handling.

## Used By
- `Application.kt`: Registers these routes under the `/admin` prefix.

## Config / Constants / Protocol Details
- **Prefix**: `/admin/draft-jobs`.
- **Media Types**: Handles `multipart/form-data` (upload) and `application/json` (responses).

## Related Tests
- `RoutesTest.kt`: Basic route connectivity tests.
- `SummaryAndArtifactRoutesTest.kt`: Tests for data-intensive retrieval endpoints.
- `RoomEditAndExportRoutesTest.kt`: Tests for correction and export flow.

## Notes / Risks
- **In-Process Backgrounding**: Using `CoroutineScope(Dispatchers.IO).launch` (L61) is suitable for a single-tenant admin tool but lacks the reliability of a proper task queue (jobs would be lost if the server restarts during processing).
- **Security**: Relies on `anyHost()` CORS (defined in `Application.kt`), which is permissive. No authentication logic is present in these routes.
