# File Dossier: RelocalizerTest.kt

## Path
app/src/test/java/com/example/vecturai/ar/RelocalizerTest.kt

## Type
test

## Role
Unit tests for the `Relocalizer` coordinate alignment logic.

## Imports / Includes
- `com.google.ar.core.Pose`
- `org.junit.Test`

## Exports / Public Surface
N/A (JUnit tests)

## Main Symbols
- `fitGraphToSessionMapsMultipleCorrespondences`: Verifies that the relocalizer can recover a known transform from multiple noisy or clean points.
- `rejectOutliersDropsLargeResiduals`: Verifies that outliers are correctly identified and removed.

## Important Logic by Line Range
- L11-35: Synthetic test data generation with rotation and translation.

## Uses
- `Relocalizer.kt`
- `PoseUtils.kt`

## Used By
N/A

## Config / Constants / Protocol Details
N/A

## Related Tests
N/A

## Notes / Risks
- Uses synthetic `Pose` objects; does not test real-world ARCore anchor behavior.
