# File Dossier: ArRouteRenderer.kt

## Path
`apps/androidApp/src/main/kotlin/com/Vectura AI/android/ar/ArRouteRenderer.kt`

## Type
Authored Source (AR Visuals)

## Role
Manages Android AR route arrow state after alignment. Converts building-local arrow placements into AR-world coordinates, applies the rolling lookahead/fade-behind window, and provides a snapshot of visible arrows in 3D AR space.

## Imports / Includes
- `android.opengl.Matrix`
- `com.Vectura AI.android.data.ArrowPlacementData`
- `com.Vectura AI.android.data.ArrowPlacementType`
- `kotlin.math.cos`, `kotlin.math.sin`

## Exports / Public Surface
- `ArRouteRenderer`: Main rendering coordinator.
- `ArrowState`: `HIDDEN`, `ACTIVE`, or `FADING`.
- `RenderableArrow3D`: AR-world arrow projection state for 3D rendering.

## Main Symbols
- `configureRendering(...)`: Sets the lookahead and fade distances.
- `setAlignmentTransform(...)`: Sets the global offset and rotation for world mapping.
- `placeAllArrows(...)` / `updateArrows(...)`: Stores full route arrow list after alignment.
- `updateVisibility(userCumulativeDistance)`: Applies active lookahead and fade-behind state.
- `snapshot()`: Provides the latest `RenderableArrow3D` states.
- `transformToAR(...)`: Projects building coords into AR space using the alignment matrix.
- `hideAllArrows()` / `clearArrows()`: Used on arrival/end navigation.

## Important Logic
- Rolling lookahead shows arrows from current route progress to `current + lookaheadDistance`.
- Fade-behind keeps recently passed arrows visible with reduced alpha/scale for `fadeDistance`.
- `transformToAR` applies the yaw-only alignment transform established by the AR ViewModel.
- `snapshot()` provides world coordinates and heading for `ArArrow3DRenderer` instead of 2D screen projections.

## Uses
- `android.opengl.Matrix`: For view/projection transforms.
- `ArrowPlacementData`: Route arrow source data from reviewed package routing.

## Used By
- `AndroidArNavigationViewModel.kt`: Owns alignment and visibility updates.
- `UnifiedArRenderer`: Reads `snapshot()` to render 3D arrows.

## Config / Constants / Protocol Details
- Lookahead distance is configured from `route_rendering.json`.
- Default fade distance: 3m.

## Related Tests
- None.

## Notes / Risks
- This renderer provides world positions for `ArArrow3DRenderer` to draw 3D meshes in `UnifiedArRenderer`.
