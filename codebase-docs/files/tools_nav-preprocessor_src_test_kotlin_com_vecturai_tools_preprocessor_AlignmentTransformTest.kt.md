# File Dossier: AlignmentTransformTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/vecturai/tools/preprocessor/AlignmentTransformTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `ar_alignment`
- **Status**: Mapped

## Role
Tests the building-local to AR-world coordinate transformation logic. It validates that point transformations and marker-based alignment calculations are numerically accurate and handle rotations correctly.

## Public Surface
- `AlignmentTransformTest`: Test class.

## Main Symbols
- `Transform`: Internal representation of the transformation model.
- `Transform.fromMarker`: Factory method to compute the transform given building and AR marker states.

## Important Logic
- **Pure Translation Test** (L62-68): Verifies that simple offsets are applied without distorting coordinates.
- **Rotation Geometry** (L71-79, L133-140): Validates the Y-axis rotation matrix logic for 90-degree and 45-degree angles.
- **Marker Alignment Roundtrip** (L83-119): Crucial test that simulates the real-world scenario of detecting a marker and ensuring the resulting transform maps the building's marker coordinates exactly to the detected AR coordinates.
- **Directional Preservation** (L122-130): Ensures that direction vectors (implicitly handled by the rotation component) are independent of translation.

## Uses
- `kotlin.math`: For sine and cosine functions.

## Related Tests
- `AlignmentInverseTest.kt`: Tests the opposite direction of the transform.

## Notes / Risks
- **Redundant Implementation**: Like `AlignmentInverseTest`, this file contains a local `Transform` class. This ensures the test suite is isolated from changes in the shared library, effectively acting as a formal specification of the coordinate system contract.
