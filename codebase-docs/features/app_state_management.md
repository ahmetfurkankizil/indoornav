# Feature: App State & Event Bus

- **Feature Name**: App State & Event Bus
- **Purpose**: Provides a unified, reactive source of truth for the application's global state and screen-level Android visitor flow phases.
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/Vectura AI/core/store/AppStore.kt`
    - `shared/core/src/commonMain/kotlin/com/Vectura AI/core/domain/NavigationState.kt`
    - `apps/androidApp/src/main/kotlin/com/Vectura AI/android/navigation/AndroidNavigationFlowModel.kt`
- **Used By**:
    - All feature modules (Search, History, etc.)
    - UI Layer (Compose/SwiftUI)
    - `NavigationSessionCoordinator`
    - Android home flow and Activity-scoped AR camera flow screens
- **Main Flow**:
    1. Components observe the `AppStore` for state changes (e.g., current navigation status, selected building).
    2. Components dispatch updates to the `AppStore` via specific setter methods.
    3. UI automatically reacts to state changes using `collectAsState` (Compose) or similar observers.
    4. Android splits visitor flow state by Activity ownership: `AndroidNavigationFlowModel` keeps MainActivity at Home/PackageError, while `ArCameraFlowViewModel` owns QrScan -> EntranceConfirmed -> DestinationSelect -> RoutePreview -> ArNavigation inside `ArCameraActivity`.
- **Key Symbols**:
    - `AppStore`: Central singleton holding the `StateFlow`s.
    - `NavigationState`: Sealed class representing the app's current mode.
    - `AndroidNavigationFlowModel.HomeState`: MainActivity package readiness state.
    - `ArCameraFlowViewModel.Phase`: Activity-scoped QR-to-AR navigation state machine.
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `kotlinx.coroutines.flow.StateFlow`
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [AppStore.kt](../files/shared_core_src_commonMain_kotlin_com_Vectura AI_core_store_AppStore.kt.md)
    - [AndroidNavigationFlowModel.kt](../files/apps_androidApp_src_main_kotlin_com_Vectura AI_android_navigation_AndroidNavigationFlowModel.kt.md)
- **Risks / Notes**:
    - Thread-safety is managed by `MutableStateFlow`.
    - Excessive state in the global store can lead to unnecessary UI recompositions; keep state granular where possible.
