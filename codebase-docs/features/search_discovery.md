# Feature: Search & Discovery

- **Feature Name**: Search & Discovery
- **Purpose**: Allows users to find rooms, amenities, and workspaces within a building through text search or category browsing.
- **Implemented In**:
    - `shared/feature-search/src/commonMain/kotlin/com/vecturai/feature/search/SearchUseCase.kt`
- **Used By**:
    - Search Screen UI
    - Quick-search components
- **Main Flow**:
    1. User enters text in the search bar.
    2. `SearchUseCase.searchRooms` fetches all available rooms from the `BuildingRepository`.
    3. Rooms are scored based on proximity of match (Name > Alias > Keyword).
    4. Results are sorted by score and displayed to the user.
- **Key Symbols**:
    - `SearchUseCase`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - Weighted scoring algorithm (100/50/30/20/10/5).
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [SearchUseCase.kt](../files/shared_feature-search_src_commonMain_kotlin_com_vecturai_feature_search_SearchUseCase.kt.md)
- **Risks / Notes**:
    - Minimal ranking logic; doesn't support fuzzy matching or typo correction yet.
    - Dependent on high-quality metadata (keywords, aliases) in the building data package.
