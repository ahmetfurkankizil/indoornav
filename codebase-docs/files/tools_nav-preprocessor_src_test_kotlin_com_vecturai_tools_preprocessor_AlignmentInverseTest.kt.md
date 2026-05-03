# File Dossier: AlignmentInverseTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/AlignmentInverseTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_alignment`
- **Status**: Mapped

## Role
Tests the mathematical correctness of the inverse coordinate transformation (AR-world back to building-local space). This is critical for translating user AR poses back into the building's navigation graph.

## Public Surface
- `AlignmentInverseTest`: Test class containing JUnit-style tests.

## Main Symbols
- `Transform`: Internal data class used for testing transformation logic (mirrors the production `AlignmentTransform` logic).

## Important Logic
- **Inverse Mathematical Verification** (L38-84): Ensures that the `inverse` function correctly cancels out the `forward` function across various combinations of translations and Y-axis rotations.
- **Identity Check** (L31-35): Verifies that a zero-transform behaves as an identity matrix.
- **Rotation Symmetry** (L46-51): Confirms that a 90-degree rotation inverse correctly restores the original basis vectors.

## Uses
- `kotlin.math`: For trigonometric operations.

## Related Tests
- `AlignmentTransformTest.kt`: Tests the forward transformation logic.

## Notes / Risks
- **Testing Logic vs Production**: Uses a local `Transform` class rather than importing one from `shared`. This is likely to keep the test standalone or to validate the mathematical model before implementation.
