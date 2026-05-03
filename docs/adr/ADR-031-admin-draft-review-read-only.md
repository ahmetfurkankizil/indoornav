# ADR-031: Read-Only Admin Draft Review

**Status:** Accepted
**Date:** 2026-03-29
**Deciders:** Vectura AI team
**Relates to:** ADR-030 (admin draft-ingestion pipeline), ADR-026 (reviewed package runtime truth)

## Context

Phase 1 (ADR-030) established the admin draft-ingestion pipeline: GLB upload, preprocessing job, and job status visibility. However, after a job succeeds the admin cannot inspect the generated draft — they can only see artifact filenames. To decide whether a draft is worth editing and promoting to a reviewed package, the admin needs to see:

1. The 2D floor plan preview (occupancy grid)
2. The navigation graph overlay
3. The generated room candidates with their auto-assigned labels
4. Basic counts and generation statistics

This review step must exist before any editing or export capability is added, because editing without visual context leads to blind corrections.

## Decision

### Read-only draft review via API and iOS admin UI

Add a summary endpoint (`GET /admin/draft-jobs/{jobId}/summary`) that parses the generated `authoring_config.generated.json`, `generation_metadata.json`, and `geometry_stats.json` to produce a structured review summary. The summary includes building metadata, graph counts, room candidates, artifact availability flags, and warnings.

Add an artifact content endpoint (`GET /admin/draft-jobs/{jobId}/artifacts/{name}/content`) that serves generated SVG and JSON files with correct content types. Path traversal is prevented by validating the artifact name against the job's known artifact list.

The iOS admin UI adds a review screen accessible from the job detail view. It shows:
- A segmented preview toggle (Plan / Graph) rendering SVGs via WKWebView
- A room candidate list with id, display name, category, and destination node
- Generation statistics and counts

### Strictly read-only — no mutations

This phase adds zero write endpoints. The admin cannot rename rooms, edit categories, modify the graph, export a reviewed package, or activate any draft content for runtime navigation. The reviewed-package runtime truth (ADR-026) remains completely unchanged.

### Why separate review from editing

1. **Risk isolation**: Review is pure read — no data corruption risk. Editing introduces mutation semantics, validation requirements, and conflict handling that deserve their own ADR and testing.
2. **Incremental verification**: We can verify that draft parsing works correctly before building editing on top of it.
3. **UX clarity**: The admin sees the raw draft output before being asked to modify it, establishing a clear mental model of what the preprocessor generated vs. what they need to change.

## Consequences

### Positive
- Admin can visually inspect draft output without terminal access
- Room candidate list provides clear starting point for future editing
- SVG preview via WKWebView requires no additional dependencies
- Zero risk to visitor navigation flow (read-only, additive)

### Negative
- Admin cannot act on what they see (intentionally deferred)
- SVG rendering quality depends on the preprocessor's debug output format

### Intentionally deferred
- Room name/category editing
- Graph node/edge editing
- Reviewed package export
- Runtime package activation
- Android admin UI
