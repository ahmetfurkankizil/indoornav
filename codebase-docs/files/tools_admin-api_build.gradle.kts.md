# File Dossier: tools/admin-api/build.gradle.kts

## Metadata
- **Path**: `tools/admin-api/build.gradle.kts`
- **Type**: Gradle Build Script (Kotlin DSL)
- **Feature**: `project_infrastructure`
- **Status**: Mapped

## Role
Defines the build configuration, dependencies, and entrypoint for the Admin API tool. It configures a Ktor-based JVM application that orchestrates navigation graph preprocessing.

## Public Surface
- `mainClass`: `com.Vectura AI.tools.admin.ApplicationKt` (L8)

## Important Logic
- **Framework Stack** (L11-22): Uses Ktor Server (Netty) for the web layer, with JSON content negotiation and serialization.
- **Inter-Tool Dependency** (L24): Directly depends on `:tools:nav-preprocessor` project, allowing the API to trigger the raw processing logic mapped in previous batches.
- **Concurrency** (L21): Includes `kotlinx-coroutines-core` for managing asynchronous preprocessing jobs.

## Uses
- `libs.plugins.kotlin.jvm`: Kotlin JVM plugin.
- `libs.plugins.kotlin.serialization`: Kotlin Serialization plugin.
- `libs.ktor.*`: Ktor server components.
- `:tools:nav-preprocessor`: For graph processing and extraction logic.

## Used By
- Gradle: Used to build and run the admin-api subproject.

## Config / Constants / Protocol Details
- **Port/Host**: Not defined here (likely in `Application.kt`).
- **Engine**: Netty (L14).

## Related Tests
- None in this file; see `src/test/` for application tests.

## Notes / Risks
- **Direct Project Dependency**: Coupling to `nav-preprocessor` means any breaking changes in the preprocessor models or `Pipeline` class will immediately affect the API.
