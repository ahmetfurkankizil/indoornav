# File Dossier: Application.kt

## Metadata
- **Path**: `tools/admin-api/src/main/kotlin/com/VecturAI/tools/admin/Application.kt`
- **Type**: Kotlin Source (Entrypoint)
- **Feature**: `admin_orchestration`
- **Status**: Mapped

## Role
Main entrypoint for the Admin API. It initializes the Ktor server, configures global plugins (JSON, CORS), and wires together the `DraftJobService` with the HTTP routing layer.

## Public Surface
- `main()`: Entrypoint function for the JVM application.
- `configureApp()`: Extension function to setup the Ktor application module.

## Important Logic
- **Configuration** (L16-17): Reads `ADMIN_API_PORT` (default 8080) and `ADMIN_JOBS_DIR` (default `build/admin-draft-jobs`) from environment variables.
- **Plugins** (L32-46):
    - `ContentNegotiation`: Configures Kotlinx Serialization for JSON with lenient parsing and pretty printing.
    - `CORS`: Opens the API for browser-based admin tools (allows `anyHost`).
- **Orchestration Wiring** (L48-52): Instantiates `DraftJobService` and attaches it to the `draftJobRoutes`.

## Uses
- `DraftJobService`: Business logic for job management.
- `draftJobRoutes`: API endpoint definitions.
- `io.ktor.*`: Server engine and plugins.

## Used By
- `build.gradle.kts`: Configured as the `mainClass`.

## Config / Constants / Protocol Details
- **Default Port**: 8080.
- **Default Storage**: `build/admin-draft-jobs`.
- **Serialization**: JSON (kotlinx).

## Related Tests
- `RoutesTest.kt`: Validates the server startup and basic connectivity.

## Notes / Risks
- **Dev-Only Assumption** (L20): The server console output notes "dev-only", suggesting it lacks robust authentication/authorization for public deployment.
- **Local Storage**: Job state is persisted in a local directory, making the service stateful and not easily horizontally scalable without a shared volume.
