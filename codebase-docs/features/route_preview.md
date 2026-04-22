# Feature: Route Preview

- **Feature Name**: Route Preview
- **Purpose**: Provides users with a summary of the path and instructions before they commit to an AR navigation session.
- **Implemented In**:
    - `shared/feature-preview/src/commonMain/kotlin/com/vecturai/feature/preview/RoutePreviewUseCase.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCameraActivity.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/data/AndroidReviewedPackageLoader.kt`
- **Used By**:
    - Destination Detail Screen
    - Route Preview Screen
    - Android route preview overlay in the Activity-scoped AR camera flow
- **Main Flow**:
    1. User selects a destination.
    2. UI calls `RoutePreviewUseCase.getRoutePreview`.
    3. Route is computed and returned as a list of text instructions and distances.
    4. UI displays the summary (Distance, Time, Steps).
    5. Android's camera-owned flow computes `LoadedPackage` on destination selection and shows a polished summary card with distance, walking ETA (`distance / 1.2 m/s`), and a "Walking" badge as an opaque Compose overlay above the still-running ARCore session.
- **Key Symbols**:
    - `RoutePreviewUseCase`
    - `RoutePreview`
    - `AndroidNavigationApp.RoutePreviewScreen`
    - `AndroidReviewedPackageLoader.LoadedPackage`
- **Config / Env / Flags**: N/A
- **Data Structures / Protocols**:
    - `RoutePreview` (route, stepsDescription, totalDistance, estimatedTime).
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [RoutePreviewUseCase.kt](../files/shared_feature-preview_src_commonMain_kotlin_com_vecturai_feature_preview_RoutePreviewUseCase.kt.md)
    - [AndroidNavigationApp.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ui_AndroidNavigationApp.kt.md)
- **Risks / Notes**:
    - Does not include a visual 2D map yet.
    - Purely instructional preview.
