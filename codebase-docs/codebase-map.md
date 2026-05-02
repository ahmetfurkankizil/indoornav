# Codebase Map - Vecturai

## Repository Purpose
Vecturai is an end-to-end indoor navigation platform that enables users to navigate complex buildings using AR overlays. The system includes a Kotlin Multiplatform core for shared logic, native mobile applications for AR visualization, and a suite of backend tools for processing 3D building scans (GLB) into navigable navigation graphs.

## Tech Stack
- **Languages**: Kotlin (2.1.10), Swift (Native iOS)
- **Frameworks**: Kotlin Multiplatform (KMP), Jetpack Compose / Compose Multiplatform, ARKit (iOS), ARCore (Android), ML Kit Barcode Scanning
- **Build System**: Gradle (8.11.1) with KTS, Version Catalogs
- **Networking/Storage**: Ktor, SqlDelight
- **Dependency Injection**: Koin

## Entrypoints
- **Android**: `apps/androidApp/src/main/kotlin/com/vecturai/android/MainActivity.kt` (Home/PackageError), `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCameraActivity.kt` (QR-to-AR camera flow)
- **iOS**: `apps/iosApp/iosApp/iOSApp.swift`
- **Admin API**: `tools/admin-api/src/main/kotlin/com/vecturai/tools/admin/Application.kt`
- **CLI Tools**: `tools/nav-preprocessor/src/main/kotlin/com/vecturai/tools/preprocessor/Main.kt`

## Runtime Architecture
Vecturai follows a **Local-First AR architecture**. 
1. **Preprocessing**: Building geometry (GLB) is processed into a navigation graph (JSON) by the `nav-preprocessor`.
2. **Distribution**: Nav packages are served via `admin-api` or bundled into mobile apps.
3. **Localization**: Mobile apps use ARCore/ARKit to detect a physical marker (QR/Image) and establish a coordinate transform between AR space and building space.
4. **Navigation**: Shared logic (`shared/core`) calculates Dijkstra paths and projects the camera pose onto the graph to provide real-time guidance via 3D AR overlays.

## Subsystem Responsibilities
- `shared/core`: Graph domain models, pathfinding engine, and AR alignment math.
- `shared/designsystem`: Common Compose UI components and branding.
- `apps/androidApp`: Jetpack Compose visitor flow, reviewed-package asset loading, and Android AR camera/alignment overlays. Home runs in `MainActivity`; QR scan, entrance confirm, destination select, route preview, and AR navigation run inside `ArCameraActivity` over one long-lived ARCore session. QR decoding uses ML Kit on ARCore camera frames; CameraX is not part of the visitor flow.
- `apps/iosApp`: ARKit integration, Swift-native AR visualization.
- `tools/nav-preprocessor`: Geometry extraction, occupancy grid generation, and graph serialization.
- `tools/admin-api`: Ktor-based management of navigation drafts and reviewed packages.

## Build/Run/Test Flow
- **Build**: `./gradlew assembleDebug` (Android) or Xcode build (iOS).
- **Run Admin API**: `./gradlew :tools:admin-api:run`.
- **Test**: `./gradlew test` (Shared/Preprocessor) or `./scripts/verify-all.sh` for full repo validation.
- **External Integrations**: ARCore (Google), ARKit (Apple), Ktor (Networking), SqlDelight (Persistence).

## Major Dependency Directions
- `apps` → `shared/features` → `shared/core`
- `tools` → `shared/core` (for domain models)
- No circular dependencies allowed between feature modules.

## Feature Index
- [Feature: Project Infrastructure](features/project_infrastructure.md)
- [Feature: Navigation Data Format](features/navigation_data_format.md)
- [Feature: Navigation Routing Engine](features/routing_engine.md)
- [Feature: Navigation Session Management](features/navigation_session_management.md)
- [Feature: AR Alignment & Spatial Transformation](features/ar_alignment.md)
- [Feature: AR Navigation (Android)](features/ar_navigation_android.md)
- [Feature: AR Navigation (iOS)](features/ar_navigation_ios.md)
- [Feature: Building Data Repository](features/building_data_repository.md)
- [Feature: App State & Event Bus](features/app_state_management.md)
- [Feature: Route Finding & Navigation](features/route_finding.md)
- [Feature: Search & Discovery](features/search_discovery.md)
- [Feature: Visit History](features/visit_history.md)
- [Feature: Route Preview](features/route_preview.md)
- [Feature: Design System & UI Components](features/design_system.md)
- [Feature: Offline Data & Caching](features/offline_caching.md)
- [Feature: Data Preprocessing](features/preprocessing.md)
- [Feature: Admin Tools (Backend Orchestration)](features/admin_orchestration.md)
- [Feature: Admin Tools (iOS implementation)](features/admin_tools_ios.md)
- [Feature: Developer Experience & Samples](features/developer_experience.md)






## Notable Risk Areas
- **Coordinate Drift**: Errors in AR-to-building alignment can cause navigation markers to appear inside walls or in incorrect rooms.
- **Schema Evolution**: The contract between the preprocessor and clients must remain backward compatible to avoid breaking deployed apps when building data is re-exported.
- **Graph Scalability**: Pathfinding performance on mobile devices as building complexity and node count increase.
- **Asset Parsing**: Reliable extraction of floor planes and navigable geometry from heterogeneous GLB files.

## Coverage Summary
- **Folders**: 172 / 172 (100%)
- **Mapped Files**: 297 (Authored behavior-relevant source)
- **Total Files Indexed**: 637 (Including artifacts and minimal assets)
- **Status**: COMPLETE
