# File Dossier: RoomEditAndExportRoutesTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/vecturai/tools/admin/RoomEditAndExportRoutesTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Integration test suite for the room metadata correction and package export endpoints. It validates the full HTTP cycle from patching a room name to downloading the finalized JSON files, ensuring that overrides are correctly persisted and reflected in all related API outputs.

## Main Symbols
- `RoomEditAndExportRoutesTest`: Test suite class.
- `seedJob()`: Helper method to populate a mock succeeded job directory with required artifacts.
- `PATCH room updates displayName successfully`: Tests the core correction endpoint.
- `exported rooms json reflects PATCH edits`: Verifies the end-to-end integration between PATCH and final export.
- `GET reviewed-package content rejects path traversal`: Security test for the file download endpoint.

## Important Logic
- **Seeding Mechanism** (L30-58): Manually constructs the filesystem state (job.json, authoring_config, etc.) to test API behavior in a pre-populated environment.
- **Workflow Verification** (L147-162): Chained test that performs a PATCH, triggers an export, and then verifies the exported file content. This ensures no data is lost between service components.
- **Security Assertions** (L194-200): Enforces path traversal protection by attempting to download files outside the reviewed-package sandbox.

## Uses
- `DraftJobRoutes`: The routing layer under test.
- `configureApp`: To setup the test application context.
- `io.ktor.server.testing`: For end-to-end route testing.

## Used By
- CI/CD: Automated integration testing of the admin workflow.

## Related Features
- `admin_orchestration`: Validates the manual review and finalization steps.

## Notes / Risks
- **JSON String Coupling**: Tests rely on hardcoded JSON structures in `seedJob()`; if the internal draft schema changes, these tests will fail even if the API logic is correct.
- **Sandboxing**: Uses unique timestamped directories in `build/` to ensure test isolation.
