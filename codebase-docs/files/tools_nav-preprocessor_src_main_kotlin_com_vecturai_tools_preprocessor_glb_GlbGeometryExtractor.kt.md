# File Dossier: GlbGeometryExtractor.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/VecturAI/tools/preprocessor/glb/GlbGeometryExtractor.kt`
- **Type**: Kotlin Source (Geometry Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Responsible for walking the glTF mesh hierarchy and extracting raw vertex positions from the binary buffer. This is the foundation for all spatial analysis.

## Public Surface
- `extract(glbData: GlbData): GeometryResult`: Returns a list of `Vec3` vertices and the computed 3D bounding box.

## Main Symbols
- `GeometryResult`: Container for vertices, bounding box, and primitive counts.

## Important Logic
- **Accessor Traversal** (L40-74): Walks through meshes -> primitives, looking for the `POSITION` attribute.
- **Binary Reading** (L63-71): Interprets the `binChunk` as `float32` triples based on the accessor's `count`, `byteOffset`, and `byteStride`.
- **Bounding Box Calculation** (L86-97): Computes the min/max extents of the entire vertex cloud.

## Uses
- `GltfModels.kt`: For accessing the glTF structures and `Vec3` type.

## Used By
- `Main.kt`: For the `inspect` command.
- `DraftPipeline.kt`: Step 2 of the extraction flow.

## Notes / Risks
- **Supported Types**: Currently only supports `VEC3` positions with `FLOAT` (5126) component types.
- **Buffer Safety**: Includes checks to prevent buffer overruns if a malformed GLB references memory outside the BIN chunk.
