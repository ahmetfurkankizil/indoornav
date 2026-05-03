# File Dossier: GlbParserTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/glb/GlbParserTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the low-level binary parsing of the GLB 2.0 container format. It ensures that the `GlbParser` correctly handles the 12-byte header, chunk structures (JSON and BIN), and binary alignment, while also verifying error handling for malformed or unsupported files.

## Public Surface
- `GlbParserTest`: Test class.

## Main Symbols
- `createMinimalGlb()`: Helper to create a byte array representing a minimal, valid GLB with no geometry.
- `createGlbWithGeometry()`: Helper to create a GLB with 3 vertices forming a triangle.
- `parse()`: The primary method under test.

## Important Logic
- **Header Validation** (L150-181): Specifically checks for the `glTF` magic bytes (`0x46546C67`) and enforces that only Version 2.0 files are accepted.
- **Chunk Extraction** (L98-109): Verifies that JSON metadata (meshes, accessors) and binary buffer data are correctly extracted into the `GlbData` model.
- **Geometry Integration** (L112-127): Tests the interaction between the `GlbParser` and `GlbGeometryExtractor` by parsing a synthetic file and verifying the extracted floating-point vertex coordinates.
- **Robustness** (L130-147): Ensures the parser fails predictably on non-existent files, truncated files, or files with incorrect headers.

## Uses
- `GlbParser`: The component being tested.
- `GlbGeometryExtractor`: For cross-component integration testing.
- `ByteBuffer`: For building synthetic binary assets.

## Related Features
- `preprocessing`: The entry point for all 3D environment processing.

## Notes / Risks
- **Alignment**: The GLB spec requires 4-byte alignment for chunks. These tests manually apply this padding (L21-25) to ensure the parser correctly handles spec-compliant files.
