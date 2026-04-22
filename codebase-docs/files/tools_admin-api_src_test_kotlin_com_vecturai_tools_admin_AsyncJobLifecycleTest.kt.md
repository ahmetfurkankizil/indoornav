# File Dossier: AsyncJobLifecycleTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/vecturai/tools/admin/AsyncJobLifecycleTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Validates the asynchronous contract of the Admin API. It ensures that file uploads return immediately with a `queued` status, and that the long-running preprocessing pipeline executes in the background without blocking the HTTP response.

## Main Symbols
- `AsyncJobLifecycleTest`: Test suite class.
- `POST draft-jobs returns 201 with queued status...`: Verifies immediate non-blocking response.
- `runDraftGeneration transitions job to failed...`: Validates state transition and error handling for bad inputs.

## Important Logic
- **Non-blocking Verification** (L45-71): Measures response time and checks status codes to ensure the server doesn't wait for the pipeline to finish before replying to the client.
- **State Machine Testing** (L102-156): Enforces the `queued` -> `processing` -> `succeeded`/`failed` transition sequence and ensures terminal states are persisted to `job.json`.
- **Sandbox Management** (L25-38): Uses unique directories per test run in `build/` to prevent cross-test data pollution and ensures cleanup after each run.

## Uses
- `DraftJobService`: The service under test.
- `JobStatus`: For state assertions.
- `io.ktor.server.testing`: For full-stack integration testing of the routes.

## Used By
- CI/CD: Automated verification of the admin backend's reliability.

## Related Features
- `admin_orchestration`: This test suite is the primary validator for the orchestration lifecycle.

## Notes / Risks
- **Concurrency**: Relies on `runBlocking` for service-layer tests but tests route-level async behavior via `testApplication`.
- **Pipeline Coupling**: Uses the real `DraftPipeline`, so test failure could be caused by either the API orchestration or the underlying preprocessor engine.
