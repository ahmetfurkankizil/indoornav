# File Dossier: Pipeline.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/VecturAI/tools/preprocessor/Pipeline.kt`
- **Type**: Kotlin Source (Orchestrator)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Coordinates the production export pipeline. It validates a human-refined authoring configuration and the associated GLB asset to ensure the final building package is functionally correct.

## Public Surface
- `execute(glbPath: String, configPath: String, outputDir: String, overwrite: Boolean): Int`: Runs the 5-step validation and export sequence.

## Important Logic
- **Execution Sequence** (L33-78):
    1. `assetInspector.inspect` (vitals check)
    2. `configLoader.load` & `validateStructure` (schema check)
    3. `graphValidator.validate` (connectivity/referential integrity)
    4. `packageExporter.export` (production files generation)
    5. `debugExporter.export` (SVG visualizations)

## Uses
- `AssetInspector.kt`
- `AuthoringConfigLoader.kt`
- `GraphValidator.kt`
- `PackageExporter.kt`
- `DebugExporter.kt`

## Used By
- `Main.kt`: Specifically the `ExportPackageCommand`.

## Notes / Risks
- **Integrity Gating**: Step 2 and 3 act as strict gates; any schema or graph error aborts the export to prevent corrupt data from reaching mobile clients.
