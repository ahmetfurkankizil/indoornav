# Feature Documentation: Data Preprocessing

## Purpose
The Data Preprocessing system is a backend CLI tool responsible for transforming raw 3D environment scans (GLB/glTF format) into structured, production-ready navigation data. It provides an automated pipeline for floor estimation, obstacle detection, and graph generation, followed by a validation and packaging stage.

## Implemented In
- `tools/nav-preprocessor/`: A standalone Kotlin command-line application using the Clikt library for CLI structure.

## Used By
- **Developer/Admin Workflow**: Used manually by developers to prepare building data.
- **Admin API**: The `AdminAPIClient` on iOS/Android communicates with a Ktor server that wraps this preprocessor to handle remote scan processing.

## Main Flow

### 1. Draft Generation Pipeline (`DraftPipeline`)
1. **GLB Parsing**: Reads binary GLB 2.0 files and extracts the JSON and BIN chunks.
2. **Geometry Extraction**: Parses the mesh hierarchy to extract all vertex positions (`float32` VEC3).
3. **Floor Estimation**: Uses a Y-histogram analysis to find the dominant horizontal plane (floor).
4. **Occupancy Grid**: Projects floor-level vertices onto a 2D XZ grid to identify walkable vs. occupied space.
5. **Zone Discovery**: Uses BFS flood-fill on the grid to group connected walkable cells into labeled zones (e.g., "Zone A").
6. **Graph Drafting**: Places nodes at zone centroids and along major axes, connecting them based on spatial adjacency.
7. **Draft Config**: Outputs an `authoring_config.generated.json` containing the initial graph and metadata.

### 2. Production Export Pipeline (`Pipeline`)
1. **Asset Inspection**: Verifies the consistency between the GLB asset and its authoring configuration.
2. **Structural Validation**: Ensures the JSON configuration adheres to the `AuthoringConfig` schema.
3. **Graph Validation**: Performs connectivity analysis (ensuring the graph is a single connected component) and validates node/edge references.
4. **Package Export**: Generates the final 5-file production set (`manifest.json`, `nav_graph.json`, `rooms.json`, `entrance_markers.json`, `route_rendering.json`).
5. **Debug Export**: Generates SVG visualizations of the occupancy grid and navigation graph for human review.

## Key Symbols
- `NavPreprocessorCommand`: CLI entry point.
- `DraftPipeline`: Orchestrator for the automated extraction flow.
- `GlbParser`: Low-level binary parser for GLB files.
- `FloorPlaneEstimator`: Histogram-based floor detection logic.
- `NavigationGraphDrafter`: Heuristic-based node placement and edge creation logic.
- `GraphValidator`: Integrity checker for the navigation graph.

## Config / Env / Flags
- `--input`, `-i`: Path to the input GLB file.
- `--output`, `-o`: Directory for output artifacts.
- `--config`, `-c`: Path to the authoring configuration JSON (for export).
- `--overwrite`: Flag to allow overwriting existing output directories.

## Data Structures / Protocols
- `AuthoringConfig`: The intermediate JSON format allowing human refinement between draft and production.
- `GlbData`: In-memory representation of parsed GLB chunks.
- `OccupancyGrid`: 2D representation of the walkable environment.

## Related File Dossiers
- [Main.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_Main.kt.md)
- [DraftPipeline.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_DraftPipeline.kt.md)
- [Pipeline.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_Pipeline.kt.md)
- [GlbParser.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_glb_GlbParser.kt.md)
- [FloorPlaneEstimator.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_analysis_FloorPlaneEstimator.kt.md)
- [OccupancyGridGenerator.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_analysis_OccupancyGridGenerator.kt.md)
- [ZoneSuggester.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_analysis_ZoneSuggester.kt.md)
- [NavigationGraphDrafter.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_draft_NavigationGraphDrafter.kt.md)
- [GraphValidator.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_GraphValidator.kt.md)
- [PackageExporter.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_PackageExporter.kt.md)
- [DebugExporter.kt](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/tools_nav-preprocessor_src_main_kotlin_com_VecturAI_tools_preprocessor_DebugExporter.kt.md)

## Risks / Notes
- **Coordinate System**: Assumes a Y-up coordinate system (standard for GLTF/Polycam).
- **Manual Review**: The "Draft" output is heuristic-based and strictly requires manual review/renaming of zones before production export.
- **Graph Complexity**: The drafting logic uses simple adjacency thresholds; complex multi-room layouts may require manual edge addition in the authoring config.
