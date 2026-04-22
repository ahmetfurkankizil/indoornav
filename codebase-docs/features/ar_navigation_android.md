# Feature: AR Navigation (Android)

- **Feature Name**: AR Navigation (Android)
- **Purpose**: Provides the Android visitor AR navigation experience with one Activity-owned ARCore camera session, Compose overlays, AR-frame QR scanning, entrance-poster alignment, rolling route arrows, next-action guidance, ETA HUD, and arrival feedback.
- **Implemented In**:
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCameraActivity.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/UnifiedArRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/UnifiedArSession.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/qr/ArFrameQrScanner.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`
- **Used By**:
    - `AndroidNavigationApp.kt`
    - `MainActivity.kt`
- **Main Flow**:
    1. `MainActivity` renders Home/PackageError only; tapping Start launches `ArCameraActivity`.
    2. `ArCameraActivity` owns one `UnifiedArSession`, one `GLSurfaceView`, one GL camera texture, and one `UnifiedArRenderer` for the whole QR-to-navigation flow.
    3. `UnifiedArSession` creates/configures/resumes a single ARCore `Session` after camera permission, ARCore install readiness, and GL texture creation are satisfied. It pauses on Activity pause and closes only on Activity destroy.
    4. `UnifiedArRenderer` draws the ARCore camera background every frame and dispatches frames by phase: QR frames to `ArCameraFlowViewModel`/`ArFrameQrScanner`, navigation frames to `AndroidArNavigationViewModel`.
    5. QR scanning uses `Frame.acquireCameraImage()` and ML Kit barcode scanning; no CameraX preview, `ImageAnalysis`, or camera provider is used.
    6. Destination selection and route preview are opaque Compose overlays above the still-running AR session.
    7. In AR navigation, `ArMarkerDetector` accepts only the reviewed package entrance poster image/name and emits `MarkerDetectionEvent`.
    8. `AndroidArNavigationViewModel` locks alignment, computes progress from camera pose projection, updates visible arrows, next action, tracking label, ETA, and arrival state.
- **Key Symbols**:
    - `AndroidArNavigationViewModel`
    - `ArCameraActivity`
    - `ArNavigationUiState`
    - `UnifiedArSession`
    - `UnifiedArRenderer`
    - `ArFrameQrScanner`
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
    - [ArCameraActivity.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArCameraActivity.kt.md)
    - [UnifiedArRenderer.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_UnifiedArRenderer.kt.md)
    - [UnifiedArSession.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_UnifiedArSession.kt.md)
    - [ArMarkerDetector.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArMarkerDetector.kt.md)
    - [ArRouteRenderer.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArRouteRenderer.kt.md)
    - [ArFrameQrScanner.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_qr_ArFrameQrScanner.kt.md)
    - [ArNavigationScreen.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_ArNavigationScreen.kt.md)
    - [QRScanScreen.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_QRScanScreen.kt.md)
- **Risks / Notes**:
    - The old CameraX QR scanner and the old AR session rebuild/backoff path have been removed.
    - Camera ownership is intentionally wasteful during destination selection and route preview: ARCore remains resumed under opaque Compose overlays.
    - Device validation remains important for ARCore marker detection, camera projection, and lifecycle behavior.
