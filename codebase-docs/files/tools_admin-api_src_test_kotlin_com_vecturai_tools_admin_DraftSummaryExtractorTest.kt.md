# File Dossier: DraftSummaryExtractorTest.kt

## Metadata
- **Path**: `tools/admin-api/src/test/kotlin/com/Vectura AI/tools/admin/DraftSummaryExtractorTest.kt`
- **Type**: Kotlin Source (Unit Test)
- **Feature**: `admin_orchestration`, `preprocessing`
- **Status**: Mapped

## Role
Validates the data aggregation logic of `DraftSummaryExtractor`. It ensures that metadata, statistics, and graph counts are correctly parsed from the synthetic output of the preprocessor pipeline and that missing files are handled with appropriate warnings.

## Main Symbols
- `DraftSummaryExtractorTest`: Test suite class.
- `extracts building metadata...`: Verifies top-level building identification.
- `counts nodes edges and rooms...`: Validates the mapping of graph entities.
- `parses generation metadata`: Checks extraction of pipeline heuristics (`floorY`, `confidence`).
- `reports artifact availability`: Verifies the existence flags for UI preview files (SVGs).

## Important Logic
- **Stub Generation** (L24-32): Helper methods to write synthetic JSON content to the test directory, simulating the output of the real pipeline.
- **Resilience Testing** (L101-108, L172-182): Ensures the extractor doesn't crash when files are missing and provides actionable feedback to the user via the `warnings` list.
- **Geometry Stat Parsing** (L134-153): Validates the deep nesting extraction from `geometry_stats.json`, including bounding box extents and occupancy grid dimensions.

## Uses
- `DraftSummaryExtractor`: The service under test.
- `DraftSummary`, `JobStatus`: For data structure verification.

## Used By
- CI/CD: Automated unit testing of the summary generation layer.

## Related Features
- `admin_orchestration`: Ensures the admin UI receives accurate and complete data for job reviews.

## Notes / Risks
- **Schema Stability**: Tests use hardcoded JSON strings. If the `nav-preprocessor` output schema changes, these tests must be updated to match.
