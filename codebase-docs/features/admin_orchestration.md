# Feature: Admin Orchestration

## Purpose
The Admin Orchestration feature provides a management layer for the navigation data lifecycle. It allows administrators to upload 3D building scans (GLB), trigger automated navigation graph extraction, review the results via an interactive dashboard, and apply manual corrections (room names, categories) before exporting the final navigation package used by mobile apps.

## Implemented In
- `tools/admin-api/`: Ktor-based JVM backend.
- `shared/core/`: Provides the domain models used in the final export.

## Used By
- Admin Dashboard (Frontend - assumed, currently out of repository scope).
- Navigation Preprocessor (Backend tool).

## Main Flow
1. **Ingestion**: Admin uploads a GLB file via `POST /admin/draft-jobs`.
2. **Drafting**: The API triggers the `DraftPipeline` (from `:tools:nav-preprocessor`) asynchronously.
3. **Extraction**: `DraftSummaryExtractor` parses the resulting artifacts (SVGs, JSONs) into a unified summary.
4. **Review**: Admin corrections are submitted via `PATCH /admin/draft-jobs/{jobId}/rooms/{roomId}`.
5. **Correction**: `RoomOverrideService` persists corrections in a sidecar JSON file.
6. **Finalization**: `ReviewedPackageExporter` merges the draft and corrections into a client-ready bundle.

## Key Symbols
- `DraftJobService`: Primary coordinator for job state and operations.
- `DraftPipeline`: External engine used for graph generation.
- `DraftSummaryExtractor`: Data aggregator for the UI.
- `ReviewedPackageExporter`: Package bundler and correction merger.

## Config / Env / Flags
- `ADMIN_API_PORT`: Server port (default 8080).
- `ADMIN_JOBS_DIR`: Local storage for jobs and artifacts.

## Data Structures / Protocols
- **DraftJob**: Tracks processing state (`queued` -> `processing` -> `succeeded`/`failed`).
- **DraftSummary**: Aggregated view of building stats and navigable rooms.
- **Reviewed Package**: Standardized 5-file JSON bundle for client consumption.

## Related Tests
- `AsyncJobLifecycleTest.kt`: Verifies the end-to-end async flow.
- `DraftJobServiceTest.kt`: Logic tests for job management.

## Related File Dossiers
- [Application.kt](../files/tools_admin-api_src_main_kotlin_com_VecturAI_tools_admin_Application.kt.md)
- [DraftJobService.kt](../files/tools_admin-api_src_main_kotlin_com_VecturAI_tools_admin_service_DraftJobService.kt.md)
- [DraftJobRoutes.kt](../files/tools_admin-api_src_main_kotlin_com_VecturAI_tools_admin_routes_DraftJobRoutes.kt.md)

## Risks / Notes
- **Statefulness**: Job data is stored on the local file system; the service is not stateless.
- **Background Integrity**: No persistent task queue; jobs in progress are lost if the process crashes.
- **Data Drift**: If the GLB is re-uploaded and room IDs change, existing overrides may become invalid.
