# File Dossier: settings.gradle.kts

## Path
settings.gradle.kts

## Type
build

## Role
Project settings and module inclusion.

## Imports / Includes
N/A

## Exports / Public Surface
- `rootProject.name`: "vecturai"
- Includes module `:app`

## Main Symbols
- `pluginManagement`: Configures plugin repositories.
- `dependencyResolutionManagement`: Configures global dependency repositories.
- `include(":app")`: Registers the application module.

## Important Logic by Line Range
- L1-13: Repository configuration for plugins (Google, MavenCentral, Portal).
- L17-23: Global dependency repository setup (Google, MavenCentral).
- L25-26: Project naming and module inclusion.

## Uses
N/A

## Used By
- Gradle: Initialization phase of the build.

## Config / Constants / Protocol Details
- `RepositoriesMode.FAIL_ON_PROJECT_REPOS`: Enforces central dependency configuration.

## Related Tests
N/A

## Notes / Risks
- Hardcodes the root project name as "vecturai" despite the folder being "vecturDENEME".
