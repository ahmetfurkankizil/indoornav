# File Dossier: ArrivalDetectorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/ArrivalDetectorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `navigation_session_management`
- **Status**: Mapped

## Role
Tests the logic for detecting when a user has arrived at their destination. It validates the state transitions between "Not Arrived", "Approaching", and "Arrived" based on route progress and Euclidean distance to the destination node.

## Public Surface
- `ArrivalDetectorTest`: Test class.

## Main Symbols
- `TestArrivalStatus`: Sealed class representing the detection state.
- `TestDetector`: Internal detector implementation used for validation.

## Important Logic
- **Progress-Based Detection** (L64-79): Verifies that reaching the `arrivalThreshold` (default 95%) triggers the "Arrived" state.
- **Distance-Based Override** (L88-91): Ensures that if the user is within a physical distance threshold (default 1.5m), they are marked as "Arrived" even if their route progress is low (handling shortcuts or sensor drift).
- **Approaching State** (L50-61): Validates that the "Approaching" state is triggered at the correct threshold (80%) and correctly calculates remaining distance.
- **Clamping Logic** (L75-85): Ensures that out-of-bounds progress values (negative or > 100%) are handled gracefully.

## Related Features
- `navigation_session_management`: This logic drives the UI/UX for ending a navigation session.

## Notes / Risks
- **Redundant Logic**: Again, uses a local `TestDetector` instead of shared code. This ensures the arrival detection contract is validated independently of any bugs that might be introduced in the KMP shared module.
