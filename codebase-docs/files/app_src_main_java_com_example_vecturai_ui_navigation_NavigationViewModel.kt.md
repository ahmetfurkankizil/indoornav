# File Dossier: NavigationViewModel.kt

## Path
app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt

## Type
source

## Role
Orchestrates localization consensus, pathfinding, path projection, and real-time guidance during navigation.

## Imports / Includes
- `com.example.vecturai.ar.Relocalizer`
- `com.example.vecturai.ar.CloudAnchorHelper`
- `com.example.vecturai.graph.Pathfinder`
- `com.example.vecturai.persistence.GraphRepository`

## Exports / Public Surface
- `NavigationViewModel` (class)
- `uiState` (StateFlow<NavigationUiState>)
- `selectBuilding(name)` (function)
- `selectDestination(id)` (function)
- `relocalizeNow()` (function)

## Main Symbols
- `recomputeGraphToSession`: Uses `Relocalizer` to compute a weighted Procrustes fit between the session and graph.
- `refreshDisplayedNodePoses`: Lerps node poses for smooth UI transitions during localization updates.
- `updateNavigationProgress`: Handles waypoint advancement and drives the 3D arrow.
- `projectToPath`: Calculates the user's progress along the path segments even when distant from anchors.
- `maybeReroute`: Triggers a new `Pathfinder` call if the user deviates from the path.
- `startResolveLoopIfReady`: Manages "First Fix" and "Maintenance" resolve loops.

## Important Logic by Line Range
- L328-371: Multi-phase resolve loop (First Fix vs Maintenance).
- L491-528: Consensus fitting logic using `Relocalizer` with outlier rejection.
- L638-672: lerp-based smoothing of node poses for the UI.
- L723-785: Navigation progress logic including path projection and advancement.
- L787-814: Automatic rerouting logic.

## Uses
- `Relocalizer.kt`
- `CloudAnchorHelper.kt`
- `Pathfinder.kt`
- `GraphRepository.kt`
- `ArrowRenderer.kt`
- `PoseUtils.kt`

## Used By
- `NavigationScreen.kt`

## Config / Constants / Protocol Details
- `GRAPH_FIT_CHANGE_EPSILON_M`: Threshold to update the display after fit change.
- `DISPLAY_POSE_CATCHUP_HZ`: smoothing frequency.
- `CONFIDENCE_DISTANCE_DECAY_M`: Spatial decay for anchor confidence.

## Related Tests
N/A (Integration level)

## Notes / Risks
- `Relocalizer` integration significantly improves stability by using all visible anchors.
- Path projection ensures the arrow remains useful even when far from known nodes.
- Lerping prevents "jumping" when new anchors resolve.
