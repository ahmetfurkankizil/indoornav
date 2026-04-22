# File Dossier: ArRouteRenderer.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`

## Type
Authored Source (AR Visuals)

## Role
Manages Android AR route arrow state after alignment. Converts building-local arrow placements into AR-world coordinates, applies the rolling lookahead/fade-behind window, and projects visible arrows into screen coordinates for the Compose overlay.

## Imports / Includes
- `android.opengl.Matrix`
- `com.vecturai.android.data.ArrowPlacementData`
- `com.vecturai.android.data.ArrowPlacementType`
- `kotlin.math.cos`, `kotlin.math.sin`

## Exports / Public Surface
- `ArRouteRenderer`: Main rendering coordinator.
- `ArrowState`: `HIDDEN`, `ACTIVE`, or `FADING`.
- `VisibleArrow`: AR-world arrow projection state.
- `ProjectedArrow`: screen-space arrow used by Compose.

## Main Symbols
- `configureRendering(...)`: Sets the lookahead and fade distances.
- `setAlignmentTransform(...)`: Sets the global offset and rotation for world mapping.
- `placeAllArrows(...)` / `updateArrows(...)`: Stores full route arrow list after alignment.
- `updateVisibility(userCumulativeDistance)`: Applies active lookahead and fade-behind state.
- `projectVisibleArrows(...)`: Projects visible AR-world arrows through ARCore view/projection matrices.
- `transformToAR(...)`: Projects building coords into AR space using the alignment matrix.
- `hideAllArrows()` / `clearArrows()`: Used on arrival/end navigation.

## Important Logic
- Rolling lookahead shows arrows from current route progress to `current + lookaheadDistance`.
- Fade-behind keeps recently passed arrows visible with reduced alpha/scale for `fadeDistance`.
- `transformToAR` applies the yaw-only alignment transform established by the AR ViewModel.
- `projectVisibleArrows` emits only arrows in/near normalized device coordinates.

## Uses
- `android.opengl.Matrix`: For view/projection transforms.
- `ArrowPlacementData`: Route arrow source data from reviewed package routing.

## Used By
- `AndroidArNavigationViewModel.kt`: Owns alignment and visibility updates.
- `ArNavigationScreen.kt`: Reads `ProjectedArrow` values through UI state.

## Config / Constants / Protocol Details
- Lookahead distance is configured from `route_rendering.json`.
- Default fade distance: 3m.

## Related Tests
- None.

## Notes / Risks
- This renderer does not draw 3D meshes directly; it projects AR positions for the Compose arrow overlay on top of the ARCore camera feed.
