# Feature: Design System & UI Components

- **Feature Name**: Design System & UI Components
- **Purpose**: Provides a unified, premium visual identity and reusable UI components across Android and iOS using Compose Multiplatform.
- **Implemented In**:
    - `shared/designsystem/`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt` (Android app-local Phase 11 parity screens)
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/ArNavigationScreen.kt` (Android AR overlay polish)
- **Used By**:
    - Android app UI
    - iOS app UI (via Compose integration)
- **Main Flow**:
    1. UI code uses `VecturaiTheme` as the root wrapper, defaulting to a dark visitor flow.
    2. Shared tokens (`VecturaiColors.SurfaceCanvas`, `Spacing.md`, `VecturaiShapes.Medium`) provide the visual foundation.
    3. Polished components (`VecturaiPrimaryButton`, `VecturaiCard`, `AuroraBackground`) are used to build immersive screens.
    4. Android-specific `MotionUtils` and `AndroidHapticManager` provide platform-appropriate animations and tactile feedback.
- **Key Symbols**:
    - `VecturaiTheme` / `VecturaiColors` / `VecturaiTypography` / `VecturaiShapes` / `Spacing`
    - `AuroraBackground`: Animated backdrop with radial gradients and dot grid.
    - `VecturaiPrimaryButton`: CTA button with brand gradient and haptic integration.
    - `GradientText` / `AnimatedNumber`: Typographic enhancements for premium feel.
    - `Modifier.vecturaiTap()`: Centralized tap behavior (scale + haptics).
- **Config / Env / Flags**:
    - `darkTheme`: Boolean flag for theme switching.
    - `reduceMotion`: Detected via `Settings.Global.TRANSITION_ANIMATION_SCALE` to gate heavy animations.
- **Data Structures / Protocols**:
    - Material 3 dark/light color schemes.
    - `VecturaiHapticsGate`: Context-based toggle for haptic feedback.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [Color.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Color.kt.md)
    - [Shapes.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Shapes.kt.md)
    - [Spacing.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Spacing.kt.md)
    - [Components.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Components.kt.md)
    - [MotionUtils.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_MotionUtils.kt.md)
- **Risks / Notes**:
    - **Platform Parity**: The Android UI is now visually synchronized with the iOS Phase 11 polish, using Inter font and glassmorphic overlays.
    - **Accessibility**: All components target a minimum 48dp touch area and support system reduce-motion settings.
    - **Performance**: `AuroraBackground` intensity is automatically throttled in battery saver mode to conserve power.
