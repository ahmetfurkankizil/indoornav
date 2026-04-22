# Feature: Route Preview

- **Feature Name**: Route Preview
- **Purpose**: Provides users with a summary of the path and instructions before they commit to an AR navigation session.
- **Implemented In**:
    - `shared/feature-preview/src/commonMain/kotlin/com/vecturai/feature/preview/RoutePreviewUseCase.kt`
- **Used By**:
    - Destination Detail Screen
    - Route Preview Screen
- **Main Flow**:
    1. User selects a destination.
    2. UI calls `RoutePreviewUseCase.getRoutePreview`.
    3. Route is computed and returned as a list of text instructions and distances.
    4. UI displays the summary (Distance, Time, Steps).
- **Key Symbols**:
    - `RoutePreviewUseCase`
    - `RoutePreview`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `RoutePreview` (route, stepsDescription, totalDistance, estimatedTime).
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [RoutePreviewUseCase.kt](../files/shared_feature-preview_src_commonMain_kotlin_com_vecturai_feature_preview_RoutePreviewUseCase.kt.md)
- **Risks / Notes**:
    - Does not include a visual 2D map yet.
    - Purely instructional preview.
