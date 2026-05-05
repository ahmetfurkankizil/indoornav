# Feature: Offline Data & Caching

- **Feature Name**: Offline Data & Caching
- **Purpose**: Ensures the application remains functional in areas with poor connectivity (e.g., building basements) by caching navigation packages locally.
- **Implemented In**:
    - `shared/data-local/src/commonMain/kotlin/com/VecturAI/data/local/LocalCacheDataSource.kt`
    - `shared/core/src/commonMain/kotlin/com/VecturAI/core/repository/BuildingRepository.kt` (Orchestration)
- **Used By**:
    - `DefaultBuildingRepository`
    - Pre-download / Sync managers
- **Main Flow**:
    1. App checks if building data is cached via `LocalCacheDataSource.hasCachedData`.
    2. If not cached, it triggers a download via `RemoteBuildingDataSource`.
    3. Downloaded JSON components are saved to the local database/file system.
    4. Subsequent sessions load data directly from the cache.
- **Key Symbols**:
    - `LocalCacheDataSource`
    - `SqlDelightCacheDataSource`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - JSON-based storage of `NavGraph`, `Room` lists, etc.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [LocalCacheDataSource.kt](../files/shared_data-local_src_commonMain_kotlin_com_VecturAI_data_local_LocalCacheDataSource.kt.md)
- **Risks / Notes**:
    - Currently in stub phase; persistent storage logic via SqlDelight is pending.
    - Version management is critical to ensure users don't navigate with outdated building graphs.
