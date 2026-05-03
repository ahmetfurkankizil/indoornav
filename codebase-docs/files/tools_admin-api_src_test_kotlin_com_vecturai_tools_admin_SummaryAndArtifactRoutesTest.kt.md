# File Dossier: SummaryAndArtifactRoutesTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/Vectura AI/tools/admin/SummaryAndArtifactRoutesTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Integration test suite for the data-intensive endpoints of the Admin API. It validates the aggregation and serving of job summaries and the delivery of raw binary/text artifacts (SVGs, JSONs) while enforcing strict security boundaries to prevent unauthorized file access.

## Main Symbols
- `SummaryAndArtifactRoutesTest`: Test suite class.
- `seedSuccessfulJob()`: Helper method to manually construct a valid job directory with specific artifact files.
- `GET summary returns parsed room list`: Verifies deep JSON extraction and room mapping in the summary endpoint.
- `GET artifact content returns SVG...`: Validates content-type handling and file reading for preview assets.
- `GET artifact content rejects path traversal`: Security verification for the file serving endpoint.

## Important Logic
- **Stubbed Success Simulation** (L32-83): Directly creates the filesystem state required for testing summary and artifact routes, allowing for targeted testing without the overhead of running the full preprocessor pipeline.
- **Security Assertions** (L192-200): Enforces that the API only serves files explicitly listed in the job's `artifacts` metadata and rejects any path traversal attempts (`../`).
- **Content Negotiation Verification** (L176-189): Ensures that SVGs and JSON files are served with content that matches their expected formats.

## Uses
- `DraftJobRoutes`: The routing layer under test.
- `configureApp`: To setup the test application context.
- `io.ktor.server.testing`: For end-to-end route testing.

## Used By
- CI/CD: Automated integration testing of the data delivery layer.

## Related Features
- `admin_orchestration`: Ensures the admin UI can reliably access and display the results of preprocessing.

## Notes / Risks
- **Data Dependency**: Tests are sensitive to the internal directory structure of the job store (`/output/`, `/job.json`).
- **Media Handling**: Serves files as byte arrays, which is efficient for small artifacts but could be a memory risk for extremely large GLB or point cloud files if served this way (though currently limited to SVGs and JSONs).
