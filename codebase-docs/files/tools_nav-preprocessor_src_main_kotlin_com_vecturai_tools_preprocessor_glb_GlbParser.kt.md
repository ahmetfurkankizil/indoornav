# File Dossier: GlbParser.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/glb/GlbParser.kt`
- **Type**: Kotlin Source (Parser)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
A low-level binary parser for the GLB 2.0 (GL Transmission Format) container. It extracts the structured JSON metadata and the raw binary buffers (mesh data) from the file.

## Public Surface
- `parse(glbPath: String): GlbData`: Reads the file, validates the 12-byte header, and extracts the primary chunks.

## Main Symbols
- `GlbData`: Container for the parsed `GltfJson` model and the `binChunk` byte array.

## Important Logic
- **Header Validation** (L55-70): Verifies the magic bytes (`glTF`) and version (2.0).
- **Chunk Traversal** (L75-98): Iteratively reads chunks from the file, identifying the JSON chunk (Type `0x4E4F534A`) and the BIN chunk (Type `0x004E4942`).
- **JSON Decoding** (L105-110): Uses `kotlinx.serialization` to map the JSON chunk to the `GltfJson` data class.

## Uses
- `GltfModels.kt`: For the glTF schema mapping.
- `ValidationException.kt`: For reporting malformed files.

## Used By
- `Main.kt`: For the `inspect` command.
- `DraftPipeline.kt`: Step 1 of the extraction flow.

## Notes / Risks
- **Endianness**: Strictly uses Little-Endian as required by the GLB specification.
- **Resource Management**: Uses `RandomAccessFile` for efficient jumping between chunks.
