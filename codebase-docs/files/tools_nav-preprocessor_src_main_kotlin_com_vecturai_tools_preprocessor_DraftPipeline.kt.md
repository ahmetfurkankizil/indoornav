# File Dossier: DraftPipeline.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/DraftPipeline.kt`
- **Type**: Kotlin Source (Orchestrator)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Coordinates the multi-stage automated process of extracting navigation data from a raw GLB scan. It handles data flow between the low-level parsers, analysis engines, and draft generators.

## Public Surface
- `execute(glbPath: String, outputDir: String): Int`: Runs the 8-step draft generation sequence. Returns an exit code (0 for success).

## Important Logic
- **Execution Sequence** (L41-144):
    1. `glbParser.parse`
    2. `geometryExtractor.extract`
    3. `floorEstimator.estimate`
    4. `gridGenerator.generate`
    5. `zoneSuggester.suggest`
    6. `graphDrafter.draft`
    7. `configGenerator.generate`
    8. `debugExporter.exportDraftDebug`

## Uses
- `GlbParser.kt`
- `GlbGeometryExtractor.kt`
- `FloorPlaneEstimator.kt`
- `OccupancyGridGenerator.kt`
- `ZoneSuggester.kt`
- `NavigationGraphDrafter.kt`
- `DraftConfigGenerator.kt`
- `DebugExporter.kt`

## Used By
- `Main.kt`: Specifically the `GenerateDraftCommand`.

## Notes / Risks
- **Data Dependency**: The pipeline is strictly sequential; a failure in an early stage (e.g., floor estimation) prevents all subsequent analysis.
- **Diagnostics**: Provides verbose console output for each step to aid in debugging failed scan processing.
