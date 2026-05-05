# File Dossier: PipelineIntegrationTest.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/test/kotlin/com/VecturAI/tools/preprocessor/PipelineIntegrationTest.kt`
- **Type**: Kotlin Source (Integration Test)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Tests the complete production pipeline of the `nav-preprocessor` utility. It ensures that given a valid authoring configuration and a GLB asset, the pipeline can successfully execute all stages: validation, transformation, visualization (SVG), and final package export.

## Public Surface
- `PipelineIntegrationTest`: Test class.

## Important Logic
- **End-to-End Execution** (L17-94):
    1.  Sets up a mock environment with a temp directory.
    2.  Writes a synthetic `AuthoringConfig` and a minimal valid GLB file (12-byte header).
    3.  Runs the `Pipeline.execute()` method.
    4.  Verifies the presence of all production output files (JSON packages) and debug artifacts (SVG, debug JSON).
    5.  Deep-checks the contents of the generated `nav_graph.json` and the validity of the SVG visualization.
- **Error Propagation** (L97-123): Confirms that the pipeline correctly returns a non-zero exit code (1) when critical inputs (like the GLB file) are missing or malformed.
- **Asset Integrity**: Verifies that the `.glb` asset is correctly associated with the config (L49-56).

## Uses
- `Pipeline`: The primary component being integrated.
- `AuthoringConfig`: The input data model.
- `PackageNavGraph`: Used for output verification.

## Related Features
- `preprocessing`: This is the primary integration test for the entire preprocessor toolset.

## Notes / Risks
- **IO Sensitivity**: This test requires a functioning file system and correctly handling absolute vs relative paths.
- **Binary Assets**: Uses a minimal binary GLB stub to avoid the overhead of parsing large meshes, focusing instead on the tool's orchestration logic.
