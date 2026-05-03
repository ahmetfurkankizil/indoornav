# File Dossier: DraftPipelineIntegrationTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/Vectura AI/tools/preprocessor/DraftPipelineIntegrationTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the entire automated drafting pipeline end-to-end. It uses synthetically generated GLB files to simulate floor geometry and verifies that the pipeline correctly performs parsing, analysis, and generation of both authoring configurations and debug visualizations.

## Public Surface
- `DraftPipelineIntegrationTest`: Test class.

## Main Symbols
- `createSyntheticFloorGlb()`: Generates a valid binary GLB 2.0 file containing a simple L-shaped floor mesh (manually constructing the JSON and BIN chunks).
- `execute()`: Invokes the `DraftPipeline` orchestrator.

## Important Logic
- **Full Flow Execution** (L90-139):
    1.  Writes the synthetic GLB to disk.
    2.  Runs the `DraftPipeline`.
    3.  Validates that `authoring_config.generated.json` was created and contains consistent graph data (node/edge integrity).
    4.  Confirms the existence and basic validity (XML header) of SVG debug artifacts.
- **Error Handling** (L141-170): Verifies that the pipeline fails gracefully (with exit code 1) when provided with a GLB that contains a valid header but no mesh geometry.
- **Binary Data Construction** (L50-86): Demonstrates a deep understanding of the GLB container format (magic bytes, versioning, chunk alignment, and Little-Endian byte order).

## Uses
- `DraftPipeline`: The primary component being integrated.
- `AuthoringConfig`: The output model.
- `ByteBuffer`: For binary GLB construction.

## Related Features
- `preprocessing`: This is the primary integration test for the preprocessor's automated drafting capability.

## Notes / Risks
- **Synthetic Data**: While effective for testing algorithms, synthetic data may lack the noise and artifacts found in real Polycam/ARKit scans.
- **File System Dependency**: Requires disk I/O; uses temporary directories to ensure clean test state.
