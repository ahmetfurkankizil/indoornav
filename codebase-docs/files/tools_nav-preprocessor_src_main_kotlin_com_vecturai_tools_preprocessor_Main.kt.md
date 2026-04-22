# File Dossier: Main.kt (nav-preprocessor)

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/vecturai/tools/preprocessor/Main.kt`
- **Type**: Kotlin Source (CLI Entry point)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
The central entry point for the Navigation Preprocessor CLI tool. It defines the command structure, parses arguments, and dispatches to the appropriate processing pipelines.

## Imports
- `com.github.ajalt.clikt`: For CLI argument parsing and subcommand management.
- `com.vecturai.tools.preprocessor.glb`: For GLB parsing and geometry extraction.
- `com.vecturai.tools.preprocessor.analysis`: For floor estimation.

## Public Surface
- `main(args: Array<String>)`: Application entry point.
- `NavPreprocessorCommand`: Root command.
- `InspectCommand`: Logic for the `inspect` subcommand.
- `GenerateDraftCommand`: Logic for the `generate-draft` subcommand.
- `ExportPackageCommand`: Logic for the `export-package` subcommand.

## Important Logic
- **Subcommand Wiring** (L161-165): Registers the three main processing flows.
- **Inspect Logic** (L42-88): Coordinates `GlbParser`, `GlbGeometryExtractor`, and `FloorPlaneEstimator` to provide a quick summary of an asset.
- **Dispatch to Pipelines** (L105-114, L137-148): Hands off complex multi-step tasks to `DraftPipeline` and `Pipeline` orchestrators.

## Uses
- `DraftPipeline.kt`: For automated draft generation.
- `Pipeline.kt`: For production package export.
- `GlbParser.kt`: For low-level binary parsing.
- `GlbGeometryExtractor.kt`: For mesh data extraction.

## Used By
- **Shell Scripts/CI**: Invoked as a standalone tool during building data preparation.
- **Admin API**: (Indirectly) Targets functionality documented here.

## Notes / Risks
- **Error Handling**: Uses custom `ValidationException` and `SystemExitException` to ensure clean CLI exits with appropriate codes.
