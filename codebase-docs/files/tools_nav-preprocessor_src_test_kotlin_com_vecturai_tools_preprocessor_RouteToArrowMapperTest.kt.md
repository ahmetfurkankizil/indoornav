# File Dossier: RouteToArrowMapperTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/RouteToArrowMapperTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_visuals`
- **Status**: Mapped

## Role
Tests the logic for transforming a sequence of navigation nodes into a series of AR "arrows" (render instructions). It ensures that arrows are correctly spaced along corridors, turns are accurately identified (left, right, U-turn), and a destination marker is always placed at the end of the route.

## Public Surface
- `RouteToArrowMapperTest`: Test class.

## Main Symbols
- `mapRoute()`: The primary method under test, mirroring the production arrow generation logic.
- `computeTurnAngle()`: Geometry utility to calculate the signed angle between two connected path segments.
- `TestArrowType`: Enumeration of arrow styles (FOLLOW, TURN_LEFT, TURN_RIGHT, U_TURN, DESTINATION).

## Important Logic
- **Interpolation** (L67-80): Validates the linear interpolation logic that places "Follow" arrows at a configurable interval (e.g., every 1.5m) along path segments.
- **Turn Detection** (L53-65, L88-99): Tests the angular threshold logic (default >30°) that decides when to replace a standard arrow with a turn indicator.
- **U-Turn Logic** (L196-205): Specifically verifies that extreme turns (near 180°) are correctly classified as U-turns.
- **Vertical Offset** (L185-193): Confirms that all generated arrows are assigned a consistent height offset (e.g., 5cm above the floor) to prevent Z-fighting in the AR view.
- **Edge Cases** (L160-182): Ensures the mapper handles very short routes (producing at least one arrow and a destination) and gracefully exits for empty or single-node paths.

## Related Features
- `ar_visuals`: This is the core logic for the "Magic Arrow" navigation UI.

## Notes / Risks
- **Trigonometry**: Uses `atan2` for turn angle calculations, which handles the full 360-degree range correctly. The tests confirm the signed directionality (positive for right, negative for left).
- **Redundant Implementation**: Like other tests in this batch, it replicates the mapper logic to ensure isolation from KMP compilation issues.
