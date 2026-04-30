# Feature: Project Infrastructure

## Purpose
Provides the build system, dependency management, and environment configuration required to develop, build, and test the VecturAI application.

## Implemented In
- `build.gradle.kts` (Root)
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle.properties`
- `local.properties`
- `.env`

## Used By
- Entire Repository

## Main Flow
1. Gradle initializes via `settings.gradle.kts`.
2. Root `build.gradle.kts` declares common plugins.
3. App module `build.gradle.kts` resolves `ARCORE_API_KEY` from `.env` or system environment.
4. Dependencies are pulled from the `libs` version catalog.

## Key Symbols
- `libs`: Version catalog accessor.
- `ARCORE_API_KEY`: Configuration for AR services.

## Config / Env / Flags
- `ARCORE_API_KEY`: API key for Google Cloud Anchor services.
- `kotlin.code.style=official`: Formatting standard.

## Data Structures / Protocols
- Gradle Version Catalog (`libs.versions.toml`)

## Related Tests
- N/A

## Related File Dossiers
- [Root build.gradle.kts](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/build.gradle.kts.md)
- [App build.gradle.kts](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/app_build.gradle.kts.md)
- [settings.gradle.kts](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/settings.gradle.kts.md)
- [gradle_libs.versions.toml](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/gradle_libs.versions.toml.md)
- [gradle.properties](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/gradle.properties.md)
- [local.properties](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/local.properties.md)
- [.env / .env.example](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/root_dot_env.md)
- [gradle-wrapper.properties](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-document/files/gradle-wrapper.properties.md)

## Risks / Notes
- Misconfiguration of `ARCORE_API_KEY` prevents AR functionality.
- Gradle sync failure blocks development.
