# Feature: Route Finding & Navigation

- **Feature Name**: Route Finding & Navigation
- **Purpose**: High-level orchestration of the navigation experience, from destination selection to arrival.
- **Implemented In**:
    - `shared/feature-routing/src/commonMain/kotlin/com/vecturai/feature/routing/RouteNavigationUseCase.kt`
- **Used By**:
    - Navigation Screen UI
    - AR View Controller
- **Main Flow**:
    1. **Selection**: User selects a destination (`startNavigation`). State -> `Scanning`.
    2. **Alignment**: AR layer detects a marker and calls `onMarkerDetected`.
    3. **Computation**: `RouteNavigationUseCase` uses `RouteEngine` to find the path. State -> `Navigating`.
    4. **Active**: User follows AR arrows (logic in `ArNavigationCoordinator`).
    5. **Arrival**: Proximity detection triggers `onArrived`. State -> `Arrived`.
- **Key Symbols**:
    - `RouteNavigationUseCase`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `NavigationState` machine integration.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [RouteNavigationUseCase.kt](../files/shared_feature-routing_src_commonMain_kotlin_com_vecturai_feature_routing_RouteNavigationUseCase.kt.md)
    - [DijkstraRouteEngine.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_routing_DijkstraRouteEngine.kt.md) (Core)
- **Risks / Notes**:
    - Dependent on `RouteEngine` for the actual pathfinding.
    - Assumes the first entry node of a room is the preferred one.
