# File Dossier: MappingViewModel.kt

## Path
app/src/main/java/com/example/vecturai/ui/mapping/MappingViewModel.kt

## Type
source

## Role
Business logic and state management for the indoor mapping process.

## Imports / Includes
- `com.example.vecturai.ar.CloudAnchorHelper`
- `com.example.vecturai.graph.MapGraph`
- `com.example.vecturai.persistence.GraphRepository`

## Exports / Public Surface
- `MappingViewModel` (class)
- `uiState` (StateFlow<MappingUiState>)
- `dropManualAnchor()` (function)
- `saveGraph(onSaved)` (function)

## Main Symbols
- `AUTO_DROP_DISTANCE_M`: Constant (4m) for automatic anchor hosting.
- `hostAnchorAt`: Core method for hosting Cloud Anchors and updating the local graph structure.
- `publishMarkers`: Synchronizes internal node/edge lists with the UI state.

## Important Logic by Line Range
- L79-98: `onSessionUpdated` logic. Triggers `autoDrop` if distance threshold is met and tracking is active.
- L161-229: `hostAnchorAt` implementation. Handles async hosting, origin establishment, relative pose calculation, and edge creation between consecutive nodes.
- L188: Localizes nodes relative to the first hosted anchor (`graphOriginPose`).

## Uses
- `CloudAnchorHelper.kt`
- `GraphRepository.kt`
- `MapGraph.kt`
- `PoseUtils.kt`

## Used By
- `MappingScreen.kt`

## Config / Constants / Protocol Details
- `HOST_TIMEOUT_MS`: 20 seconds for Cloud Anchor hosting operations.
- Uses `UUID` for unique node identification.

## Related Tests
N/A

## Notes / Risks
- If hosting fails during `autoDrop`, the distance counter resets, which might leave gaps in the graph.
- First node in a session establishes the origin for that entire mapping run.
