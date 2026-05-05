# File Dossier: OffRouteDetectorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/OffRouteDetectorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_session_management`
- **Status**: Mapped

## Role
Tests the logic for detecting when a user has significantly deviated from the planned navigation path. It validates the state machine that transitions between varying levels of confidence and drift, providing actionable recommendations (e.g., "Move toward route" or "Rescan marker") to the mobile client.

## Public Surface
- `OffRouteDetectorTest`: Test class.

## Main Symbols
- `TestDetector`: Mock implementation of the off-route detection engine.
- `onPoseUpdate()`: The primary entry point for providing user position data.
- `onCorrectionApplied()`: Feedback loop for coordinate alignment corrections.

## Important Logic
- **Drift Classification** (L43-50): Maps lateral distance to status levels:
    - `0m - 2m`: `ON_ROUTE`
    - `2m - 4m`: `MINOR_DRIFT`
    - `4m - 6m`: `LOW_CONFIDENCE`
    - `> 6m`: `LIKELY_OFF_ROUTE`
- **Compound State Detection** (L103-107): Escalates `LIKELY_OFF_ROUTE` to `RECOVERY_RECOMMENDED` if tracking quality is limited (e.g., poor lighting) or if the pose data is stale (sensor lag).
- **Correction Stability** (L120-136): Implements a "flapping" check where consecutive large coordinate corrections (> 1.5m) trigger a recovery recommendation, indicating that the alignment itself is likely incorrect.
- **Deduplication & Staleness** (L110-117): Ensures that lack of sensor updates for more than 3 seconds results in a confidence drop.

## Related Features
- `navigation_session_management`: This detector is a core part of the session safety logic.

## Notes / Risks
- **Redundant Implementation**: Like other tests in this suite, it replicates the detection logic in a mock class to validate the state machine behavior in isolation.
- **Hardcoded Thresholds**: The thresholds (2m, 4m, 6m) are tested as part of the contract. Any change to production thresholds must be reflected here.
