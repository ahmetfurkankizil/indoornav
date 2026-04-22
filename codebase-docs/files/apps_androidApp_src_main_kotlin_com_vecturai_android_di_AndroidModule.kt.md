# File Dossier: AndroidModule.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/di/AndroidModule.kt`

## Type
Authored Source (DI Configuration)

## Role
Defines the Koin module for Android-specific dependencies, including ARCore helpers, haptics, reviewed-package loading, and Android ViewModels.

## Imports / Includes
- `com.vecturai.android.ar.ArBridge`
- `com.vecturai.android.ar.AndroidArNavigationViewModel`
- `com.vecturai.android.ar.AndroidHapticManager`
- `com.vecturai.android.ar.ArMarkerDetector`
- `com.vecturai.android.ar.ArRouteRenderer`
- `com.vecturai.android.ar.ArSessionManager`
- `com.vecturai.android.data.AndroidReviewedPackageLoader`
- `com.vecturai.android.navigation.AndroidNavigationFlowModel`
- `org.koin.android.ext.koin.androidContext`
- `org.koin.core.module.dsl.viewModel`
- `org.koin.dsl.module`

## Exports / Public Surface
- `androidModule`: Android Koin module with singleton, factory, and ViewModel providers.

## Logic
- Registers `ArBridge`, `AndroidReviewedPackageLoader`, and `AndroidHapticManager` as singletons.
- Registers AR session, marker detector, and route renderer as factories.
- Registers `AndroidNavigationFlowModel` and `AndroidArNavigationViewModel` as Koin ViewModels.

## Uses
- `ArBridge`
- `AndroidReviewedPackageLoader`
- `AndroidHapticManager`
- `ArSessionManager`
- `ArMarkerDetector`
- `ArRouteRenderer`

## Used By
- `VecturaiApp.kt`: Loaded into the global Koin context.
- `MainActivity.kt`: Resolves the registered ViewModels.

## Related Tests
- None.
