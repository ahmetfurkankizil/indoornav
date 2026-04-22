# File Dossier: AndroidNavigationApp.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ui/AndroidNavigationApp.kt`

## Type
Authored Source (Android Compose UI)

## Role
Compose root and visitor-facing Android screens for the iOS-parity navigation flow: home, package error, destination selection, route preview, and entrance confirmation.

## Imports / Includes
- Jetpack Compose foundation/material/icons/runtime APIs
- `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- `com.vecturai.android.navigation.AndroidNavigationFlowModel`
- `com.vecturai.android.ar.AndroidArNavigationViewModel`
- `com.vecturai.android.data.AndroidReviewedPackageLoader`

## Exports / Public Surface
- `AndroidNavigationApp(...)`
- `GradientPrimaryButton(...)`

## Main Symbols
- `AndroidNavigationApp`: Switches on `FlowState` and composes the active screen.
- `HomeScreen`: Polished home with gear icon and gradient CTA.
- `DestinationSelectScreen`: Search, category grouping, rich room cards/badges.
- `RoutePreviewScreen`: Walking estimate, route type badge, and start action.
- `EntranceConfirmedSheet`: Confirmation before destination selection.

## Important Logic
- The flow state machine lives in `AndroidNavigationFlowModel`; this file renders and dispatches UI actions.
- Walking time is calculated as `distance / 1.2 m/s`, matching iOS route preview copy.
- Search flattens room display while non-search mode groups rooms by category.

## Uses
- `AndroidNavigationFlowModel`
- `AndroidArNavigationViewModel`
- `QRScanScreen`
- `ArNavigationScreen`

## Used By
- `MainActivity.kt`: Root content.

## Related Tests
- None.
