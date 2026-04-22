# Feature: Project Infrastructure & Build System

- **Feature Name**: Project Infrastructure & Build System
- **Purpose**: Provides the foundation for building, testing, and developing the Vecturai multiplatform project.
- **Implemented In**:
    - `build.gradle.kts`
    - `settings.gradle.kts`
    - `gradle/libs.versions.toml`
    - `Makefile`
    - `.github/workflows/ci.yml`
- **Used By**: Developers, CI/CD, and all functional modules of the project.
- **Main Flow**:
    1. Gradle initializes via `settings.gradle.kts` and `build.gradle.kts`.
    2. Subprojects are configured based on their local `build.gradle.kts` (to be mapped in later batches).
    3. `Makefile` provides shorthand for common developer tasks.
    4. GitHub Actions runs CI on push/PR to validate the build.
- **Key Symbols**: N/A (Infrastructure)
- **Config / Env / Flags**:
    - `JDK 21` requirement.
    - Gradle properties for memory management.
- **Data Structures / Protocols**:
    - Gradle Version Catalog (`libs.versions.toml`).
- **Related Tests**:
    - `test-all` (Makefile)
    - `test-preprocessor` (Makefile)
- **Related File Dossiers**:
    - [build.gradle.kts](../files/build.gradle.kts.md)
    - [settings.gradle.kts](../files/settings.gradle.kts.md)
    - [libs.versions.toml](../files/gradle_libs.versions.toml.md)
    - [Makefile](../files/Makefile.md)
    - [_github_workflows_ci.yml](../files/_github_workflows_ci.yml.md)
- **Risks / Notes**:
    - iOS builds are not yet integrated into the main Gradle build or GitHub Actions CI (ubuntu runner).
