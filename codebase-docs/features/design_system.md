# Feature: Design System & UI Components

- **Feature Name**: Design System & UI Components
- **Purpose**: Provides a unified, premium visual identity and reusable UI components across Android and iOS using Compose Multiplatform.
- **Implemented In**:
    - `shared/designsystem/`
- **Used By**:
    - Android app UI
    - iOS app UI (via Compose integration)
- **Main Flow**:
    1. UI code uses `VecturaiTheme` as the root wrapper.
    2. Components like `VecturaiButton`, `VecturaiCard`, and `VecturaiSectionHeader` are used to build screens.
    3. Navigation is orchestrated by `AppNavigation` within the `designsystem` module.
- **Key Symbols**:
    - `VecturaiTheme`
    - `VecturaiColors`
    - `VecturaiTypography`
    - `VecturaiAppContent`
- **Config / Env / Flags**:
    - `darkTheme`: Boolean flag for theme switching.
- **Data Structures / Protocols**:
    - Material 3 Color Schemes.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [Theme.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Theme.kt.md)
    - [Color.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Color.kt.md)
    - [Components.kt](../files/shared_designsystem_src_commonMain_kotlin_com_vecturai_designsystem_Components.kt.md)
- **Risks / Notes**:
    - The design system module also hosts the common screen implementations, making it a "Feature UI" module as well as a "Design System" module.
    - AR views are NOT part of this Compose-based system; they are native overlays.
