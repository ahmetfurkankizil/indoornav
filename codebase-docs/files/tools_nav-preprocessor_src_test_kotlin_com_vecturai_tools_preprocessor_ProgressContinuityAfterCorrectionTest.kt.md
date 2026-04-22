# File Dossier: ProgressContinuityAfterCorrectionTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/vecturai/tools/preprocessor/ProgressContinuityAfterCorrectionTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_alignment`, `navigation_session_management`
- **Status**: Mapped

## Role
Tests the interaction between the coordinate alignment system (AR-to-building) and the navigation progress estimator. It ensures that when a correction is applied (e.g., after scanning a checkpoint marker), the user's reported progress remains smooth, monotonic, and doesn't "jump" backward even if the physical coordinate shift suggests a regression.

## Public Surface
- `ProgressContinuityAfterCorrectionTest`: Test class.

## Main Symbols
- `TestEstimator`: Mock implementation of the progress projection engine with support for dynamic alignment offsets.
- `update()`: Projects AR coordinates onto the building route.
- `setAlignment()`: Simulates an alignment update from the correction coordinator.

## Important Logic
- **Progress Monotonicity** (L103-116): The core test. Verifies that the "Monotonic Guard" correctly holds the progress at its peak value even if a coordinate correction suggests the user is suddenly "behind" where they previously were.
- **Correction Stability** (L72-84): Confirms that applying a correction and then moving to the equivalent new AR coordinate results in a continuous (non-decreasing) progress value.
- **Arrival Sensitivity** (L119-132): Ensures that the arrival detection logic (100% progress / 0m remaining) still functions correctly after multiple alignment shifts.
- **Identity Case** (L135-146): Validates that a zero-correction update has no side effects on the estimation state.

## Related Features
- `ar_alignment`: Tested via the alignment offset support.
- `navigation_session_management`: Tested via the progress and arrival logic.

## Notes / Risks
- **Monotonic Tolerance**: The estimator uses a tolerance (default 0.5m) for its guard logic. This allows for slight sensor jitter while still preventing significant regressions.
- **Redundant Implementation**: Replicates the math of the shared progress engine to test the specific interaction with alignment shifts.
