# File Dossier: build.gradle.kts (App Module)

## Path
app/build.gradle.kts

## Type
build

## Role
Primary module build configuration, dependency management, and build logic.

## Imports / Includes
- `libs` (Version Catalog)

## Exports / Public Surface
N/A (Build configuration)

## Main Symbols
- `readDotEnvValue()`: Helper function to parse `.env` files for build-time constants.
- `android.defaultConfig.manifestPlaceholders`: Injects `ARCORE_API_KEY` into the manifest.

## Important Logic by Line Range
- L7-29: Implementation of `readDotEnvValue`. It sequentially checks `.env` for a specific key.
- L46-50: API Key resolution order: `gradleProperty` -> `environmentVariable` -> `.env`.
- L73-95: Dependency list, including `arcore`, `sceneview`, and `compose`.

## Uses
- `gradle/libs.versions.toml`
- `.env`
- `AndroidManifest.xml` (via placeholders)

## Used By
- Android Build System

## Config / Constants / Protocol Details
- `compileSdk`: 36 (Android 15+)
- `minSdk`: 24 (Android 7.0+)

## Related Tests
N/A

## Notes / Risks
- The `readDotEnvValue` function ensures that developers don't have to hardcode keys in the codebase.
