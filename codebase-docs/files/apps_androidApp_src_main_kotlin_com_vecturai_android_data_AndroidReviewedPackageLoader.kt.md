# File Dossier: AndroidReviewedPackageLoader.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/data/AndroidReviewedPackageLoader.kt`

## Type
Authored Source (Android Navigation Data)

## Role
Loads the Android bundled reviewed package from assets and computes route packages for the visitor flow. Mirrors the iOS five-file reviewed package contract.

## Imports / Includes
- `android.content.Context`
- `kotlinx.serialization.Serializable`
- `kotlinx.serialization.json.Json`
- `kotlin.math.abs`, `max`, `sqrt`

## Exports / Public Surface
- `AndroidReviewedPackageLoader`
- Reviewed package models: `PackageNode`, `PackageEdge`, `PackageMarker`, `PackageRoom`, `RouteRenderingConfig`, `Manifest`
- `ReviewedConfig`
- `LoadedPackage`
- `PackageError`
- `ArrowPlacementData`
- `ArrowPlacementType`

## Main Symbols
- `loadReviewedPackage()`: Reads and decodes `manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, and `route_rendering.json` from `assets/reviewed-package/`.
- `computeRoute(config, destinationRoomId)`: Runs Dijkstra from the first entrance marker start node to the room destination node and returns route geometry/arrows.

## Important Logic
- Manifest file names drive the asset reads, matching the runtime contract used by iOS.
- Dijkstra honors bidirectional edges and edge cost.
- Arrow generation places follow arrows by spacing, adds turn markers based on segment direction cross product, and marks the final destination arrow.
- `LoadedPackage` carries route points, total distance, destination metadata, and the entrance marker for AR startup.

## Uses
- Android assets.
- Kotlinx Serialization JSON.

## Used By
- `AndroidNavigationFlowModel.kt`
- `QRPayload.kt`
- `AndroidArNavigationViewModel.kt`
- `AndroidModule.kt`

## Config / Constants / Protocol Details
- Reviewed package root: `assets/reviewed-package/`.
- Required files are the five reviewed package JSONs exported by the admin/iOS pipeline.

## Related Tests
- None.

## Notes / Risks
- Android currently has its own route/arrow computation instead of reusing shared core or iOS Swift logic, so parity should be regression-tested with the same reviewed package.
