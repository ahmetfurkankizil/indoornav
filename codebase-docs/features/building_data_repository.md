# Feature: Building Data Repository & Lifecycle

- **Feature Name**: Building Data Repository & Lifecycle
- **Purpose**: Manages the retrieval, parsing, and caching of building navigation packages.
- **Implemented In**:
    - `shared/core/src/commonMain/kotlin/com/VecturAI/core/loading/`
    - `shared/core/src/commonMain/kotlin/com/VecturAI/core/repository/`
- **Used By**:
    - `NavigationSessionCoordinator`
    - Feature: Search
- **Main Flow**:
    1. `BuildingRepository` identifies available buildings (via `DemoPackageProvider` in MVP).
    2. `BuildingPackageLoader` fetches and deserializes the JSON-based `BuildingPackage`.
    3. The package (NavGraph, Rooms, Markers) is cached in an `InMemoryPackageStore`.
    4. The current active package is provided to the routing and navigation engines.
- **Key Symbols**:
    - `BuildingRepository`: Interface for building data access.
    - `BuildingPackageLoader`: Handles serialization of the building JSON.
    - `InMemoryPackageStore`: Shared singleton for cross-module data access.
- **Config / Env / Flags**:
    - `DemoMode`: Flag to use local assets instead of network requests (MVP default).
- **Data Structures / Protocols**:
    - `BuildingPackage`: The top-level data contract for an indoor space.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [BuildingPackageLoader.kt](../files/shared_core_src_commonMain_kotlin_com_VecturAI_core_loading_BuildingPackageLoader.kt.md)
    - [BuildingRepository.kt](../files/shared_core_src_commonMain_kotlin_com_VecturAI_core_repository_BuildingRepository.kt.md)
- **Risks / Notes**:
    - MVP uses synchronous loading for demo assets; future versions should use asynchronous Ktor-based fetching.
