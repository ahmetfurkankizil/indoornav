# Feature: App State & Event Bus

- **Feature Name**: App State & Event Bus
- **Purpose**: Provides a unified, reactive source of truth for the application's global state and facilitates decoupled communication between components.
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/store/AppStore.kt`
    - `shared/core/src/commonMain/kotlin/com/vecturai/core/domain/NavigationState.kt`
- **Used By**:
    - All feature modules (Search, History, etc.)
    - UI Layer (Compose/SwiftUI)
    - `NavigationSessionCoordinator`
- **Main Flow**:
    1. Components observe the `AppStore` for state changes (e.g., current navigation status, selected building).
    2. Components dispatch updates to the `AppStore` via specific setter methods.
    3. UI automatically reacts to state changes using `collectAsState` (Compose) or similar observers.
- **Key Symbols**:
    - `AppStore`: Central singleton holding the `StateFlow`s.
    - `NavigationState`: Sealed class representing the app's current mode.
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `kotlinx.coroutines.flow.StateFlow`
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [AppStore.kt](../files/shared_core_src_commonMain_kotlin_com_vecturai_core_store_AppStore.kt.md)
- **Risks / Notes**:
    - Thread-safety is managed by `MutableStateFlow`.
    - Excessive state in the global store can lead to unnecessary UI recompositions; keep state granular where possible.
