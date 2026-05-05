# Feature: Visit History

- **Feature Name**: Visit History
- **Purpose**: Tracks previously visited rooms and locations for quick re-navigation.
- **Implemented In**:
    - `shared/feature-history/src/commonMain/kotlin/com/VecturAI/feature/history/HistoryUseCase.kt`
- **Used By**:
    - History Screen
    - Search Screen (Recent items)
- **Main Flow**:
    1. Navigation session completes (Arrived state).
    2. A `VisitRecord` is created and stored via `HistoryUseCase.recordVisit`.
    3. User opens the History screen; `getRecentVisits` fetches the list.
- **Key Symbols**:
    - `HistoryUseCase`
    - `VisitRecord`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `VisitRecord` (id, buildingId, roomId, timestamp).
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [HistoryUseCase.kt](../files/shared_feature-history_src_commonMain_kotlin_com_VecturAI_feature_history_HistoryUseCase.kt.md)
    - [InMemoryHistoryRepository.kt](../files/shared_core_src_commonMain_kotlin_com_VecturAI_core_repository_InMemoryHistoryRepository.kt.md) (Core)
- **Risks / Notes**:
    - Persistence depends on the `HistoryRepository` implementation (currently in-memory for MVP).
