# File Dossier: AndroidModule.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/di/AndroidModule.kt`

## Type
Authored Source (DI Configuration)

## Role
Defines the Koin module for Android-specific dependencies, haptics, reviewed-package loading, AR navigation helpers, and Android ViewModels.

## Imports / Includes
- `AndroidArNavigationViewModel`
- `AndroidHapticManager`
- `ArBridge`
- `ArMarkerDetector`
- `ArRouteRenderer`
- `AndroidReviewedPackageLoader`
- `AndroidNavigationFlowModel`
- `ArCameraFlowViewModel`
- Koin module and ViewModel DSL APIs

## Exports / Public Surface
- `androidModule`

## Logic
- Registers `ArBridge`, `AndroidReviewedPackageLoader`, and `AndroidHapticManager` as singletons.
- Registers marker detector and route renderer as factories.
- Registers `AndroidNavigationFlowModel`, `ArCameraFlowViewModel`, and `AndroidArNavigationViewModel` as Koin ViewModels.
- `UnifiedArSession` is intentionally Activity-owned by `ArCameraActivity`, not a Koin singleton.

## Uses
- `ArBridge`
- `AndroidReviewedPackageLoader`
- `AndroidHapticManager`
- `ArMarkerDetector`
- `ArRouteRenderer`

## Used By
- `VecturaiApp.kt`: Loaded into the global Koin context.
- `MainActivity.kt`: Resolves the home ViewModel.
- `ArCameraActivity.kt`: Resolves the camera-flow and AR navigation ViewModels.

## Related Tests
- None.
