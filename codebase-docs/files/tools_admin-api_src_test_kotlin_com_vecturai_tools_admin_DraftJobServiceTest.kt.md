# File Dossier: DraftJobServiceTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/vecturai/tools/admin/DraftJobServiceTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Tests the core job management logic within `DraftJobService`. It validates job creation, disk persistence (GLB and metadata), job retrieval, and listing functionality.

## Main Symbols
- `DraftJobServiceTest`: Test suite class.
- `createJob stores GLB and creates job json`: Validates the filesystem layout and initial state.
- `listJobs returns all jobs sorted...`: Ensures correct ordering of jobs for the dashboard.

## Important Logic
- **Filesystem Verification** (L35-44): Directly checks that `input.glb`, `job.json`, and an `output` directory are created correctly on disk.
- **Sorting Logic** (L63-72): Verifies that jobs are returned in descending chronological order, which is critical for user experience in the admin UI.
- **Persistence Roundtrip** (L51-60): Ensures that data serialized to JSON can be accurately reconstructed.

## Uses
- `DraftJobService`: The service under test.
- `JobStatus`: For state assertions.

## Used By
- CI/CD: Automated unit testing.

## Related Features
- `admin_orchestration`: Validates the base job handling logic.

## Notes / Risks
- **IO Performance**: Since tests perform real disk IO, they may be slower than pure logic tests.
- **Thread.sleep** (L65): Uses a small sleep to ensure distinct timestamps for sorting tests; this is a potential source of flakiness if the file system doesn't update timestamps with enough precision.
