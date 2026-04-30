# File Dossier: gradle/libs.versions.toml

## Path
gradle/libs.versions.toml

## Type
build (Version Catalog)

## Role
Centralized dependency and plugin version management.

## Imports / Includes
N/A

## Exports / Public Surface
- Versions: `agp`, `kotlin`, `arcore`, `sceneview`, `composeBom`, etc.
- Libraries: `androidx-core-ktx`, `google-ar-core`, `sceneview-arsceneview`, etc.
- Plugins: `android-application`, `kotlin-compose`, `kotlin-serialization`.

## Main Symbols
- `[versions]`: Version definitions.
- `[libraries]`: Dependency definitions.
- `[plugins]`: Plugin definitions.

## Important Logic by Line Range
- L2-16: Core tech stack versions.
- L19-38: Library definitions mapped to versions.
- L41-43: Plugin definitions mapped to versions.

## Uses
N/A

## Used By
- `build.gradle.kts` (Root)
- `app/build.gradle.kts`

## Config / Constants / Protocol Details
- `arcore = "1.53.0"`
- `sceneview = "4.0.1"`
- `kotlin = "2.3.20"`

## Related Tests
N/A

## Notes / Risks
- Changes here affect the entire project's dependency resolution.
- Includes specific ARCore and SceneView versions critical for AR functionality.
