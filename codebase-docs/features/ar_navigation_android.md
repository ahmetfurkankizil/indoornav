# Feature: AR Navigation (Android)

- **Feature Name**: AR Navigation (Android)
- **Purpose**: Provides the Android visitor AR navigation experience with an ARCore camera feed, Compose overlays, entrance-poster alignment, rolling route arrows, next-action guidance, ETA HUD, and arrival feedback.
- **Implemented In**:
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCoreCameraRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArSessionManager.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArFeatureFlags.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`
- **Used By**:
    - `AndroidNavigationApp.kt`
    - `MainActivity.kt`
- **Main Flow**:
    1. `AndroidNavigationFlowModel` reaches `ArNavigation` with a selected `LoadedPackage` and validated entrance marker.
    2. `ArNavigationScreen` configures `AndroidArNavigationViewModel` and embeds a `GLSurfaceView`.
    3. `ArCoreCameraRenderer` starts the ARCore session, draws the camera feed, and forwards each frame to the ViewModel.
    4. If the AR session fails (e.g. camera busy), `AndroidArNavigationViewModel` performs an exponential backoff session rebuild.
    5. `ArMarkerDetector` accepts only the reviewed package entrance poster image/name and emits `MarkerDetectionEvent`.
    6. `AndroidArNavigationViewModel` locks alignment, computes progress from camera pose projection, updates visible arrows, next action, tracking label, ETA, and arrival state.
    7. Compose draws projected arrows and Phase 11-style overlays on top of the AR camera feed.
- **Key Symbols**:
    - `AndroidArNavigationViewModel`
    - `ArNavigationUiState`
    - `ArCoreCameraRenderer`
    - `ArFeatureFlags.ArUnifiedCameraPipeline`
    - `MarkerDetectionEvent`
- **Config / Env / Flags**:
    - ARCore is required by `AndroidManifest.xml`.
    - Marker image is loaded from `assets/ar/<referenceImageName>.png`.
    - `simulateAlignment()` exists as a testing/development fallback from the alignment overlay.
- **Data Structures / Protocols**:
    - `ArrowPlacementData`: Position, orientation, type, label, and cumulative distance for route cues.
    - `RouteRenderingConfig`: Arrow spacing, lookahead distance, destination threshold, turn threshold, and height offset.
    - `ArNavigationUiState`: Compose-facing state for alignment, tracking, progress, next action, projected arrows, and arrival.
- **Related Tests**:
    - None.
- **Related File Dossiers**:
    - [AndroidArNavigationViewModel.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_AndroidArNavigationViewModel.kt.md)
    - [ArCoreCameraRenderer.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArCoreCameraRenderer.kt.md)
    - [ArFeatureFlags.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArFeatureFlags.kt.md)
    - [ArMarkerDetector.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArMarkerDetector.kt.md)
    - [ArRouteRenderer.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArRouteRenderer.kt.md)
    - [ArSessionManager.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArSessionManager.kt.md)
    - [ArNavigationScreen.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_ArNavigationScreen.kt.md)
- **Risks / Notes**:
    - This commit removed the old `ArNavigationActivity`; Android AR is now single-activity Compose.
    - Device validation remains important for ARCore marker detection, camera projection, and lifecycle behavior.
