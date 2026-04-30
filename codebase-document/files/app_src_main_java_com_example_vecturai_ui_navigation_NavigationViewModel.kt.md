# File Dossier: NavigationViewModel.kt

## Path
app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt

## Type
source

## Role
Orchestrates the localization, pathfinding, and real-time guidance during navigation.

## Imports / Includes
- `com.example.vecturai.ar.ArrowRenderer`
- `com.example.vecturai.ar.CloudAnchorHelper`
- `com.example.vecturai.graph.Pathfinder`
- `com.example.vecturai.persistence.GraphRepository`

## Exports / Public Surface
- `NavigationViewModel` (class)
- `uiState` (StateFlow<NavigationUiState>)
- `selectBuilding(name)` (function)
- `selectDestination(id)` (function)

## Main Symbols
- `NavigationPhase` (Enum): Lifecycle of a navigation session.
- `MAX_PARALLEL_RESOLVES`: Constant (40) for concurrent anchor resolution.
- `RELOCALIZE_INTERVAL_MS`: Constant (15s) for periodic drift correction.
- `startResolveLoopIfReady`: Manages the background coroutine for resolving Cloud Anchors.
- `updateNavigationProgress`: Drives the arrow rendering and waypoint advancement.

## Important Logic by Line Range
- L185-208: `onSessionUpdated` hook. Triggers progress updates and handles tracking loss recovery.
- L210-240: Path selection logic using `Pathfinder`.
- L275-297: The resolve loop. Chunks nodes for parallel resolution to improve localization speed.
- L313: Updates the global `graphToSessionPose` upon any successful anchor resolution.
- L382-403: Waypoint arrival logic using `WAYPOINT_ADVANCE_DISTANCE_M` (1.2m).

## Uses
- `CloudAnchorHelper.kt`
- `Pathfinder.kt`
- `GraphRepository.kt`
- `ArrowRenderer.kt`
- `PoseUtils.kt`

## Used By
- `NavigationScreen.kt`

## Config / Constants / Protocol Details
- `RESOLVE_TIMEOUT_MS`: 20 seconds per anchor resolve attempt.

## Related Tests
N/A

## Notes / Risks
- Parallel resolution is resource-intensive; capped at 40 to avoid session instability.
- Drift correction depends on finding at least one anchor that was hosted in a similar lighting/feature environment.
