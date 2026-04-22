# File Dossier: build.gradle.kts (Design System)

## Path
`shared/designsystem/build.gradle.kts`

## Type
Authored Config (Gradle)

## Role
Build configuration for the shared design system component, providing common Compose UI elements for both Android and iOS.

## Logic
- Applies `kotlin-multiplatform`, `kotlin-compose`, `compose-multiplatform`, and `android-library` plugins.
- Configures `androidTarget()` and iOS targets.
- Defines dependencies for `commonMain` including `:shared:core`, Compose libraries, and Koin.
- Configures Android `namespace` and `compileSdk`.

## Dependencies
- `:shared:core`: Shared domain models and logic.
- `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`: Compose Multiplatform libraries.
- `libs.koin-core`, `libs.koin-compose`: Dependency injection.

## Used By
- `:apps:androidApp`
- `:shared:feature-search`
- `:shared:feature-routing`
- `:shared:feature-history`
- `:shared:feature-preview`

## Status
Mapped (Incremental Update)
