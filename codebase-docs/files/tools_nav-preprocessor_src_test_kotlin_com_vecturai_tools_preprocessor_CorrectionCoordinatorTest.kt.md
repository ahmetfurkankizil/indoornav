# File Dossier: CorrectionCoordinatorTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/vecturai/tools/preprocessor/CorrectionCoordinatorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_alignment`, `navigation_session_management`
- **Status**: Mapped

## Role
Tests the `CorrectionCoordinator` logic, which is responsible for dynamically adjusting the AR-to-building alignment when a checkpoint marker is detected during a navigation session. This prevents drift accumulation over long routes.

## Public Surface
- `CorrectionCoordinatorTest`: Test class.

## Main Symbols
- `TestCorrector`: Internal mock of the correction engine.
- `onCheckpoint`: The core method that processes a marker observation and updates the global alignment.

## Important Logic
- **Drift Correction Math** (L67-86): Calculates the delta between the current alignment and the "perfect" alignment suggested by the detected marker.
- **Bounding & Safety** (L137-160): Validates that individual corrections are capped by `maxTranslation` (e.g., 2m) and `maxRotation` (e.g., 15°) to prevent massive jumps caused by false marker detections.
- **Confidence Filtering** (L163-172): Rejects marker detections with a confidence score below a configurable threshold.
- **Temporal Deduplication** (L175-185): Ensures that multiple detections of the same marker in quick succession (within 5 seconds) do not lead to over-correction.
- **Normalization** (L95-100): Correctly handles 360-degree wrapping for angular corrections.

## Related Features
- `ar_alignment`: This is the dynamic component of the alignment system.
- `navigation_session_management`: Corrections happen during an active session.

## Notes / Risks
- **Mock Implementation**: Uses `TestCorrector` to replicate the logic of the shared `CorrectionCoordinator`. This allows testing the algorithm's robustness against noise and drift in a controlled environment without requiring a full AR environment.
