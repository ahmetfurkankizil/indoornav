# File Dossier: build.gradle.kts (Root)

## Path
build.gradle.kts

## Type
build

## Role
Top-level Gradle build configuration.

## Imports / Includes
- `libs.plugins.android.application`
- `libs.plugins.kotlin.compose`
- `libs.plugins.kotlin.serialization`

## Exports / Public Surface
N/A (Build configuration)

## Main Symbols
- `plugins` block: Declares plugins used across modules without applying them to the root.

## Important Logic by Line Range
- L2-6: Plugin declaration using the version catalog (`libs`).

## Uses
- `gradle/libs.versions.toml`: For plugin aliases.

## Used By
- Root Project: Orchestrates sub-module build setup.

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Uses `apply false` for modules, meaning sub-modules must explicitly apply them.
