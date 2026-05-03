# File Dossier: AssetInspector.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/AssetInspector.kt`
- **Type**: Kotlin Source (Utility)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Performs a lightweight sanity check on .glb asset files. It validates magic bytes and header metadata without performing a full mesh extraction, providing immediate feedback on asset validity.

## Public Surface
- `inspect(glbPath: String): AssetInfo`: Returns file size, version, and any warnings.

## Important Logic
- **Header Parsing** (L51-77): Manually reads the first 12 bytes of the file to extract the `magic` (glTF), `version`, and `totalLength`.
- **Validation** (L60-76): Confirms the file is a valid GLB v2.0.
- **Heuristic Warnings** (L85-87): Flags exceptionally large files (>500MB) which might cause memory issues during full extraction.

## Used By
- `Pipeline.kt`: Step 1 of the production flow.

## Notes / Risks
- **Endianness**: Implementation correctly handles little-endian byte order for header fields.
- **Minimal Impact**: Designed to be extremely fast; used for pre-flight checks before intensive processing.
