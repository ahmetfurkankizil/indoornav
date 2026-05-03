# File Dossier: AndroidNavigationApp.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt`

## Type
Authored Source (Android Compose UI)

## Role
Compose root for the Android Home/PackageError surface plus reusable visitor-flow child screens used by `ArCameraActivity`.

## Imports / Includes
- Jetpack Compose foundation/material/icons/runtime APIs
- `AndroidNavigationFlowModel`
- `ArCameraFlowViewModel`
- `AndroidReviewedPackageLoader`
- `VecturaiTheme`

## Exports / Public Surface
- `AndroidNavigationApp`: Entry point with `VecturaiTheme` and `VecturaiHapticsGate`.
- `HomeScreen`: Premium landing with `AuroraBackground` and animated feature pills.
- `DestinationSelectScreen`: Grouped rooms with search, filtering, and recent locations.
- `RoutePreviewScreen`: Comprehensive summary with `RouteHeroCard` and `RouteTimelineCard`.
- `EntranceConfirmedSheet`: Full-screen success state with `AnimatedCheckMark`.
- `BrandMark`: Animated brand identity component.

## Main Symbols
- `AndroidNavigationApp`: Manages the root home/error state; provides the design system context.
- `HomeScreen`: Implements the "dark visitor flow" with `AuroraBackground`, `GradientText`, and a `VecturaiPrimaryButton` for scanning.
- `DestinationSelectScreen`: Uses `LazyColumn` with sticky-like headers for categories; features a collapsing search header.
- `RoutePreviewScreen`: Renders the route geometry preview and walking stats; includes a scrollable timeline of the journey.
- `EntranceConfirmedSheet`: High-confidence confirmation overlay shown after a successful QR/Marker scan.

## Important Logic
- **Shared Design System**: Migrated from ad-hoc styling to `VecturaiTheme` and centralized components (`VecturaiPrimaryButton`, `VecturaiCard`).
- **Motion Policy**: Uses `MotionUtils` (`rememberAuroraIntensity`) to disable or dampen animations based on system settings or battery saver mode.
- **Haptic Feedback**: Integrates `VecturaiHapticsGate` and `AndroidHapticManager` to provide tactile feedback on interactions (via `vecturaiTap`).
- **Category Grouping**: Rooms are grouped by semantic categories (e.g., "Rooms", "Labs", "Food") in the destination picker for better UX.
- **Route Hero**: The preview screen features a "Hero Card" that combines walking time, distance, and a mini-map visualization.

## Uses
- `VecturaiTheme`, `VecturaiColors`, `VecturaiBrush`, `VecturaiShapes`, `Spacing`, `VecturaiTypography`
- `AuroraBackground`, `VecturaiPrimaryButton`, `IconChip`, `GradientText`, `StatPill`, `VecturaiCard`
- `MotionUtils` (`rememberReduceMotion`, `rememberAuroraIntensity`)
- `AndroidNavigationFlowModel`, `ArCameraFlowViewModel`

## Used By
- `MainActivity.kt`: Root home content.
- `ArCameraActivity.kt`: Overlays for destination selection, route preview, and confirmation.

## Related Tests
- None.
