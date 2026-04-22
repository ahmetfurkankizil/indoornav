# File Dossier: ArRouteRenderer.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`

## Type
Authored Source (AR Visuals)

## Role
Manages the positioning and transformation of 3D navigation arrows in the AR world. Handles the transition from building-local to AR-world coordinates.

## Imports / Includes
- `android.opengl.Matrix`
- `kotlin.math.cos`, `kotlin.math.sin`

## Exports / Public Surface
- `ArRouteRenderer`: Main rendering coordinator.
- `ArrowRenderData`: Metadata for arrow visuals.

## Main Symbols
- `setAlignmentTransform(...)`: Sets the global offset and rotation for world mapping.
- `updateArrows(arrows)`: Updates the list of currently rendered navigation markers.
- `transformToAR(...)`: Projects building coords into AR space using the alignment matrix.

## Important Logic by Line Range
- **50-70**: Matrix scaling based on `ArrowRenderType` (e.g., Turn arrows are larger).
- **76-88**: Rotation-aware point transformation (Y-axis only).

## Uses
- `android.opengl.Matrix`: For coordinate transformations.

## Used By
- `ArNavigationActivity.kt`: Primary consumer for visualization.

## Config / Constants / Protocol Details
- Arrow scales: FOLLOW (0.08f), TURN (0.12f), DESTINATION (0.15f).

## Related Tests
- None.

## Notes / Risks
- Rendering logic is currently data-only; requires an external engine (Sceneform/Filament) for actual GPU draw calls.
