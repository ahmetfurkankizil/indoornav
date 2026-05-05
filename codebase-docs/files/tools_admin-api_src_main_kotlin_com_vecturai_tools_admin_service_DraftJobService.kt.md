# File Dossier: DraftJobService.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/VecturAI/tools/admin/service/DraftJobService.kt`
- **Type**: Kotlin Source (Service)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Orchestrates the lifecycle of navigation draft jobs. It manages job creation, triggers the `nav-preprocessor` pipeline asynchronously, handles artifact retrieval, manages room metadata overrides, and facilitates final reviewed package exports.

## Main Symbols
- `DraftJobService`: Core service class.
- `createJob()`: Initializes a new job by storing the uploaded GLB.
- `runDraftGeneration()`: Asynchronously executes the `DraftPipeline`.
- `getSummary()`: Aggregates job results and overrides into a `DraftSummary`.
- `patchRoom()`: Updates metadata overrides for a specific room.
- `exportReviewedPackage()`: Triggers the final export of correction-applied navigation data.

## Important Logic
- **Job Lifecycle** (L43-94): Transitions job status from `queued` to `processing` and then `succeeded` or `failed`. It uses `withContext(Dispatchers.IO)` to run the heavy-duty preprocessor without blocking the Ktor event loop.
- **Path Traversal Protection** (L131-142, L190-196): Validates artifact names against known lists and rejects path separators (`/`, `\`, `..`) to ensure client requests stay within the job's sandbox.
- **Override Persistence** (L146-164): Delegates override management to `RoomOverrideService` but validates `roomId` exists in the generated authoring config to prevent phantom overrides.
- **Artifact Discovery** (L62-66): Automatically indexes all files produced by the pipeline in the job's `output` directory.

## Uses
- `DraftPipeline` (from `:tools:nav-preprocessor`): The engine that generates the draft.
- `RoomOverrideService`: For managing metadata corrections.
- `DraftSummaryExtractor`: For building the UI-facing summary.
- `ReviewedPackageExporter`: For the final export step.

## Used By
- `Application.kt`: Wires this service into the application module.
- `DraftJobRoutes.kt`: Calls service methods for all API endpoints.

## Config / Constants / Protocol Details
- **Base Directory**: `build/admin-draft-jobs` (configurable via constructor).
- **Job ID**: Random 8-character UUID string.

## Related Tests
- `DraftJobServiceTest.kt`: Unit tests for job management logic.
- `AsyncJobLifecycleTest.kt`: Validates the async pipeline execution.

## Notes / Risks
- **IO Errors**: Frequent file operations (read/write JSON) are susceptible to file system permission issues or disk space limits.
- **Blocking Tasks**: While `DraftPipeline` is run in `Dispatchers.IO`, multiple concurrent jobs will compete for CPU/memory on the server.
