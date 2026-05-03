# File Dossier: DebugExporter.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/Vectura AI/tools/preprocessor/DebugExporter.kt`
- **Type**: Kotlin Source (Export Logic)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Generates visual and structured debug artifacts to allow developers to verify the accuracy of the preprocessing pipeline and the resulting navigation graph.

## Public Surface
- `export(config: AuthoringConfig, outputDir: String)`: Exports production-stage debug artifacts.
- `exportDraftDebug(...)`: Exports draft-stage debug artifacts (occupancy grid, draft graph, geometry stats).

## Important Logic
- **SVG Generation** (L65-190, L217-267, L269-335): Programmatically constructs SVG strings for the occupancy grid and navigation graph. Uses color-coding for zones and different node types (junction vs. room entry).
- **Geometry Stats** (L387-419): Summarizes the technical details of the scan (vertex count, floor height, zone count) into `geometry_stats.json`.
- **Debug JSON** (L31-61): Generates a `graph_debug.json` that includes extra calculated metadata like Euclidean distances for edges, helping detect cost/geometry mismatches.

## Uses
- `AuthoringConfig.kt`: Primary input model.
- `kotlinx.serialization`: For JSON export.

## Used By
- `Pipeline.kt`: Step 5 of the production flow.
- `DraftPipeline.kt`: Step 8 of the draft flow.

## Notes / Risks
- **XML Escaping**: Includes manual XML escaping for labels to prevent malformed SVG files.
- **Large Files**: SVG generation for very large buildings (1000+ nodes) can result in large files; resolution is fixed to avoid excessive file sizes.
