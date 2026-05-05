# File Dossier: DraftConfigGenerator.kt

## Metadata
- **Path**: `tools/nav-preprocessor/src/main/kotlin/com/VecturAI/tools/preprocessor/draft/DraftConfigGenerator.kt`
- **Type**: Kotlin Source (Config Generation)
- **Feature**: `preprocessing`
- **Status**: Mapped

## Role
Assembles the final results from the drafting pipeline into the standardized `AuthoringConfig` JSON format. It prepares the file for human review by including metadata and placeholders.

## Public Surface
- `generate(...): String`: Writes `authoring_config.generated.json` and `generation_metadata.json` to the output directory.

## Main Symbols
- `GeneratedMetadata`: A summary of the generation process (counts, confidence, timestamp).

## Important Logic
- **Room Mapping** (L80-91): Creates `AuthoringRoom` entries for every zone except the largest (hallway), mapping each zone to its centroid node.
- **Marker Placeholder** (L93-107): Places a single placeholder `AuthoringMarker` at the first node to remind users to configure a real entry point.
- **JSON Serialization** (L109-129): Maps the internal draft structures to the shared `AuthoringConfig` model and writes the pretty-printed JSON.

## Uses
- `AuthoringConfig.kt`: The shared domain model for building configuration.

## Used By
- `DraftPipeline.kt`: Step 7 of the extraction flow.

## Notes / Risks
- **Confidence Level**: All auto-generated configs are explicitly marked as "low confidence" to ensure developers don't accidentally deploy them without review.
- **Metadata Persistence**: Includes `generation_metadata.json` to track the technical parameters (floor height, vertex counts) used during the automated pass.
