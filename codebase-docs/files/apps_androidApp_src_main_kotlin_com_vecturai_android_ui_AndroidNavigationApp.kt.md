# File Dossier: AndroidNavigationApp.kt

## Path
`apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/AndroidNavigationApp.kt`

## Type
Authored Source (Android Compose UI)

## Role
Compose root for the Android Home/PackageError surface plus reusable visitor-flow child screens used by `ArCameraActivity`.

## Imports / Includes
- Jetpack Compose foundation/material/icons/runtime APIs
- `AndroidNavigationFlowModel`
- `ArCameraFlowViewModel`
- `AndroidReviewedPackageLoader`
- `VecturAITheme`

## Exports / Public Surface
- `AndroidNavigationApp(...)`
- `DestinationSelectScreen(...)`
- `RoutePreviewScreen(...)`
- `EntranceConfirmedSheet(...)`
- `DotGridBackground(...)`

## Main Symbols
- `AndroidNavigationApp`: Renders MainActivity's Home or PackageError only.
- `HomeScreen`: Dark welcome screen with `DotGridBackground`, `FeaturePill` labels, and a gear icon for admin tools.
- `DestinationSelectScreen`: Activity-scoped destination picker with `SearchField`, `DestinationFilter` chips, and `RecentDestinationCard`.
- `RoutePreviewScreen`: Activity-scoped summary showing walking ETA, distance, and origin-to-destination path info.
- `EntranceConfirmedSheet`: Camera-flow bottom sheet shown over the still-running ARCore preview.
- `DotGridBackground`: Custom Canvas drawing for the dark theme aesthetics.

## Important Logic
- The welcome screen implements a premium "dark mode" aesthetic with vibrant primary colors and micro-animations (dotted backgrounds).
- Destination selection uses category-based grouping (`classroom`, `lab`, `cafe`, etc.) and pre-calculated route summaries.
- `RouteSummary` integration: room cards and the preview screen show estimated walking time (distance / 1.2 m/s).
- Admin tools accessibility is moved to a top-right settings icon, separating management from the visitor flow.

## Uses
- `AndroidNavigationFlowModel`
- `ArCameraFlowViewModel`
- `AndroidReviewedPackageLoader`

## Used By
- `MainActivity.kt`: Root home content.
- `ArCameraActivity.kt`: Reuses destination, route preview, and entrance-confirmed overlays.

## Related Tests
- None.
