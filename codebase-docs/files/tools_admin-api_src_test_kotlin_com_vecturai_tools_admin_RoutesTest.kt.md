# File Dossier: RoutesTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/VecturAI/tools/admin/RoutesTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Provides base integration testing for the Admin API routes. It focuses on the HTTP interface, validating correct status codes and basic error handling for job listing, file uploads, and resource lookups.

## Main Symbols
- `RoutesTest`: Test suite class.
- `GET draft-jobs returns empty list initially`: Verifies fresh state handling.
- `POST draft-jobs rejects non-GLB file`: Validates input filtering.
- `POST draft-jobs accepts GLB and creates job`: Checks successful creation via HTTP multipart.

## Important Logic
- **HTTP Multipart Handling** (L44-87): Rigorously tests the upload endpoint with different file types and contents to ensure the server correctly parses and validates incoming binary data.
- **Error Response Validation** (L90-99): Confirms that missing resources return the appropriate 404 Not Found status, as expected by REST conventions.
- **Application Context Setup** (L30-34): Uses `testApplication` to launch an in-memory Ktor server configured with a unique temporary storage directory for each test.

## Uses
- `DraftJobRoutes`: The routing layer under test.
- `configureApp`: To setup the test application context.
- `io.ktor.server.testing`: For end-to-end route testing.

## Used By
- CI/CD: Automated verification of the API surface.

## Related Features
- `admin_orchestration`: Validates the primary API entry points for the orchestration layer.

## Notes / Risks
- **Surface Level**: These tests primarily check "happy paths" and basic errors for the API surface; they do not deep-test the underlying business logic or async processing (see `AsyncJobLifecycleTest.kt` for that).
