# Feature: AR Alignment & Spatial Transformation

- **Feature Name**: AR Alignment & Spatial Transformation
- **Purpose**: Bridges the gap between the AR-world coordinate system (established by ARKit/ARCore) and the building-local coordinate system (defined in the navigation graph).
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/ar/`
- **Used By**:
    - `NavigationSessionCoordinator`
    - `ProgressEstimator`
    - `ArNavigationBridge`
- **Main Flow**:
    1. A platform-specific AR session detects an `EntranceMarker` (image/object).
    2. The `AlignmentTransform` is calculated by comparing the detected pose with the known building-local coordinates of that marker.
    3. The transform is applied to all subsequent AR camera poses to project them into building-local space.
    4. Nav nodes (arrows, markers) are projected from building-local space into AR-world space for rendering.
- **Key Symbols**:
    - `AlignmentTransform`: Encapsulates rotation (around Y) and translation between coordinate systems.
    - `PoseModels`: Shared data types for position and orientation.
    - `RouteToArrowMapper`: Determines where to place visual cues along the route.
- **Config / Env / Flags**:
    - Coordinate System: Building-local is +X (right), +Y (up), +Z (down/forward - depends on specific building map).
- **Data Structures / Protocols**:
    - `CameraPose`: X, Y, Z coordinates and rotation matrix from the AR session.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [AlignmentTransform.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_ar_AlignmentTransform.kt.md)
    - [ArNavigationBridge.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_ar_ArNavigationBridge.kt.md)
- **Risks / Notes**:
    - Only supports 1D rotation (yaw) for simplicity (ADR-007); assumes floors are flat and horizontal.
    - VIO drift over long distances is corrected via `CorrectionCoordinator` (if implemented).
