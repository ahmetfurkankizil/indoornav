# Feature: AR Alignment & Spatial Transformation

- **Feature Name**: AR Alignment & Spatial Transformation
- **Purpose**: Bridges the gap between the AR-world coordinate system (established by ARKit/ARCore) and the building-local coordinate system (defined in the navigation graph).
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/ar/`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArMarkerDetector.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArSessionManager.kt`
- **Used By**:
    - `NavigationSessionCoordinator`
    - `ProgressEstimator`
    - `ArNavigationBridge`
    - Android Compose AR navigation flow
- **Main Flow**:
    1. A platform-specific AR session detects an `EntranceMarker` (image/object).
    2. The `AlignmentTransform` is calculated by comparing the detected pose with the known building-local coordinates of that marker.
    3. The transform is applied to all subsequent AR camera poses to project them into building-local space.
    4. Nav nodes (arrows, markers) are projected from building-local space into AR-world space for rendering.
    5. Android now reads the marker physical size/name/pose from `assets/reviewed-package/entrance_markers.json` and only accepts the matching ARCore Augmented Image.
- **Key Symbols**:
    - `AlignmentTransform`: Encapsulates rotation (around Y) and translation between coordinate systems.
    - `PoseModels`: Shared data types for position and orientation.
    - `RouteToArrowMapper`: Determines where to place visual cues along the route.
    - `AndroidArNavigationViewModel.handleMarkerDetected`: Android parity implementation of marker-based alignment lock.
- **Config / Env / Flags**:
    - Coordinate System: Building-local is +X (right), +Y (up), +Z (down/forward - depends on specific building map).
- **Data Structures / Protocols**:
    - `CameraPose`: X, Y, Z coordinates and rotation matrix from the AR session.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [AlignmentTransform.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_ar_AlignmentTransform.kt.md)
    - [ArNavigationBridge.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_ar_ArNavigationBridge.kt.md)
    - [AndroidArNavigationViewModel.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_AndroidArNavigationViewModel.kt.md)
    - [ArMarkerDetector.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArMarkerDetector.kt.md)
- **Risks / Notes**:
    - Only supports 1D rotation (yaw) for simplicity (ADR-007); assumes floors are flat and horizontal.
    - VIO drift over long distances is corrected via `CorrectionCoordinator` (if implemented).
