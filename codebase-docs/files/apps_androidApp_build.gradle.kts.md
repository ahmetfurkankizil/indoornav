# File Dossier: build.gradle.kts (Android App)

## Path
`apps/androidApp/build.gradle.kts`

## Type
Authored Config (Gradle)

## Role
Build configuration for the Android mobile application.

## Logic
- Configures `namespace`, `compileSdk`, `defaultConfig`, Java 21 compatibility, Compose build features, and resource packaging excludes.
- Applies Android/Kotlin/Compose plugins plus Kotlin Serialization.
- Lists all shared feature/data dependencies.
- Adds Android runtime libraries required by the modernized Compose flow: lifecycle runtime compose, coroutines Android, Kotlinx Serialization JSON, CameraX, ML Kit barcode scanning, and ARCore.

## Dependencies
- Project modules: `:shared:core`, `:shared:designsystem`, `:shared:feature-*`, `:shared:data-*`.
- Compose: runtime, foundation, Material3, UI, material icons extended.
- Android: Activity Compose, Lifecycle runtime/ViewModel/runtime Compose.
- Camera/QR: CameraX core/camera2/lifecycle/view and ML Kit barcode scanning.
- AR: `libs.arcore`.
- DI/serialization: Koin Android, kotlinx serialization JSON, coroutines Android.

## Used By
- Android Gradle Plugin.
- Android Studio sync/run configuration discovery.
