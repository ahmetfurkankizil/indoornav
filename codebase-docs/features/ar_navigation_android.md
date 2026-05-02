# Feature: AR Navigation (Android)

- **Feature Name**: AR Navigation (Android)
- **Purpose**: Provides the Android visitor AR navigation experience with one Activity-owned ARCore camera session, Compose overlays, AR-frame QR scanning, entrance-poster alignment, rolling 3D route arrows, next-action guidance, ETA HUD, and arrival feedback.
- **Implemented In**:
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCameraActivity.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/UnifiedArRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/UnifiedArSession.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArArrow3DRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/Arrow3DGeometry.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/qr/ArFrameQrScanner.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/QRScanScreen.kt`
- **Used By**:
    - `AndroidNavigationApp.kt`
    - `MainActivity.kt`
- **Main Flow**:
    1. `MainActivity` renders the landing screen; `ArCameraActivity` is launched for the full navigation lifecycle.
    2. QR scanning and AR alignment share the same `UnifiedArSession` and `UnifiedArRenderer`.
    3. Navigation guidance is presented via glassmorphic overlays (`InstructionBanner`, `BottomHud`).
    4. `AndroidHapticManager` fires tactile events for key moments (Route start, near turn, re-centering, arrival).
    5. The UI automatically handles tracking degradation by showing a "Hold steady" or "Re-centering" badge.
    6. Arrival is celebrated with confetti and journey stats, providing a clear end-to-trip experience.
- **Key Symbols**:
    - `AndroidArNavigationViewModel`: Central logic for progress computation and action determination.
    - `AndroidHapticManager`: Singleton for dispatching platform-specific tactile feedback.
    - `ArArrow3DRenderer`: Renders high-quality 3D navigation arrows directly into the AR scene.
    - `InstructionBanner`: Top-level guidance showing the immediate next step and distance.
    - `BottomHud`: HUD containing the circular ETA progress and safe-stop action.
- **Config / Env / Flags**:
    - `HapticsEnabled`: Global flag to gate haptic feedback.
    - `RouteRenderingConfig`: Defines arrow density, height, and turn detection thresholds.
- **Related Tests**:
    - None.
- **Related File Dossiers**:
    - [AndroidArNavigationViewModel.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_AndroidArNavigationViewModel.kt.md)
    - [AndroidHapticManager.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_AndroidHapticManager.kt.md)
    - [ArNavigationScreen.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_ArNavigationScreen.kt.md)
    - [MotionUtils.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_MotionUtils.kt.md)
- **Risks / Notes**:
    - **Visual Parity**: Achieves 1:1 visual parity with the iOS client Phase 11 polish.
    - **Session Integrity**: ARCore remains resumed during overlay states (Destination/Route) to ensure instant relocalization after selection.
    - **Performance**: High-frequency haptic calls are gated and use the most efficient Android `Vibrator` APIs.
