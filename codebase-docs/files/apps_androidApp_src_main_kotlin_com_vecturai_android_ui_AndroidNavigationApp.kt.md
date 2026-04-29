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
- `AndroidNavigationApp(...)`
- `DestinationSelectScreen(...)`
- `RoutePreviewScreen(...)`
- `EntranceConfirmedSheet(...)`
- `GradientPrimaryButton(...)`

## Main Symbols
- `AndroidNavigationApp`: Renders MainActivity's Home or PackageError only.
- `HomeScreen`: Dark welcome screen with dotted background, centered logo/title/feature pills, bottom-anchored CTA group, and gear icon; CTA launches `ArCameraActivity` through a callback.
- `DestinationSelectScreen`: Activity-scoped destination picker, search, category grouping, rich room cards/badges.
- `RoutePreviewScreen`: Activity-scoped walking estimate, route type badge, and start action.
- `EntranceConfirmedSheet`: Camera-flow bottom sheet shown over the still-running ARCore preview.

## Important Logic
- Home no longer transitions through QR, destination selection, preview, or AR navigation in-place.
- The welcome screen uses a local Compose drawing helper for the dotted background; it does not alter navigation behavior.
- The destination and preview composables are reusable overlays for `ArCameraActivity`; they operate on `ArCameraFlowViewModel`.
- Walking time is calculated as `distance / 1.2 m/s`, matching iOS route preview copy.
- Search flattens room display while non-search mode groups rooms by category.

## Uses
- `AndroidNavigationFlowModel`
- `ArCameraFlowViewModel`
- `AndroidReviewedPackageLoader`

## Used By
- `MainActivity.kt`: Root home content.
- `ArCameraActivity.kt`: Reuses destination, route preview, and entrance-confirmed overlays.

## Related Tests
- None.
