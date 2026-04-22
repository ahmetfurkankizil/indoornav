# Feature: Navigation Session Management

- **Feature Name**: Navigation Session Management
- **Purpose**: Orchestrates the end-to-end lifecycle of a navigation session, from building selection to destination arrival.
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/navigation/`
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/domain/NavigationState.kt`
- **Used By**:
    - Feature modules (Search, Routing, etc.)
    - Mobile App Shells (Compose UI, AR ViewModels)
- **Main Flow**:
    1. `NavigationSessionCoordinator` initializes the state to `Idle`.
    2. Transition to `Scanning` when a building and target room are selected.
    3. Transition to `Navigating` once the AR marker is detected and alignment is established.
    4. `ProgressEstimator` continuously calculates the user's position along the route.
    5. `ArrivalDetector` triggers transition to `Arrived` when within the target threshold.
    6. `NavigationSessionCoordinator` records the visit history upon completion.
- **Key Symbols**:
    - `NavigationSessionCoordinator`: Central state machine controller.
    - `NavigationState`: Sealed class defining the flow states.
    - `ProgressEstimator`: Projects 3D position onto the route polyline.
    - `ArrivalDetector`: Monitors distance to the target door node.
- **Config / Env / Flags**:
    - `monotonicToleranceMeters`: Jitter tolerance for progress estimation.
    - `offRouteThresholdMeters`: Threshold for "low confidence" tracking.
- **Data Structures / Protocols**:
    - `NavigationSession`: Mutable state holding current route, alignment, and progress.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [NavigationSessionCoordinator.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_navigation_NavigationSessionCoordinator.kt.md)
    - [ProgressEstimator.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_navigation_ProgressEstimator.kt.md)
    - [ArrivalDetector.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_navigation_ArrivalDetector.kt.md)
- **Risks / Notes**:
    - State is held in-memory; app restarts will lose the current session state.
    - Progress estimation requires a valid `AlignmentTransform`.
