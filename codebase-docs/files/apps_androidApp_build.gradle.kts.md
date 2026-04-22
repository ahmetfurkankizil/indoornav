# File Dossier: build.gradle.kts (Android App)

## Path
`apps/androidApp/build.gradle.kts`

## Type
Authored Config (Gradle)

## Role
Build configuration for the Android mobile application.

## Logic
- Configures `namespace`, `compileSdk`, and `defaultConfig`.
- Enables `compose`.
- Lists all shared feature and data dependencies.

## Dependencies
- `:shared:core`, `:shared:designsystem`, `:shared:feature-*`, `:shared:data-*`.
- `libs.arcore`: ARCore SDK.

## Used By
- Android Build System.
