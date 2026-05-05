# ADR-032: Admin Room Editing and Reviewed Package Export

**Status:** Accepted
**Date:** 2026-03-30
**Deciders:** VecturAI team
**Relates to:** ADR-030 (admin draft-ingestion pipeline), ADR-031 (admin draft review read-only), ADR-026 (reviewed package runtime truth)

## Context

Phases 1 and 2 (ADR-030, ADR-031) established the admin draft-ingestion pipeline and read-only review. After a GLB scan is processed, the admin can see the generated draft summary, 2D previews, and auto-generated room candidates. However, the auto-generated room names (Zone A, Zone B, etc.) and categories ("unknown") are not suitable for visitor-facing navigation. The admin needs to correct room metadata before the draft can become a reviewed package.

## Decision

### 1. Room edits are stored as overrides, not mutations

Room metadata edits are persisted in a separate `room_overrides.json` file under the job directory, keyed by room ID. The generated `authoring_config.generated.json` in `output/` is never mutated.

**Why:** The generated artifacts are the raw preprocessor output. Keeping them immutable means we can always compare the original draft against what the admin changed. If the preprocessor is re-run or improved, the overrides can be re-applied cleanly. This also prevents accidental data loss from a bad edit.

### 2. Reviewed package export is allowed in this phase

The admin can export a reviewed package derived from the generated draft plus room overrides. The export produces the standard 5-file reviewed package format (`manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, `route_rendering.json`) under a `reviewed-package/` subdirectory within the job directory.

**Why:** The export is a natural next step after review. Without export, room edits have no downstream use. The export uses the same reviewed package format already consumed by the iOS app, making it immediately usable if an admin later copies it into the bundle manually.

### 3. Package activation and runtime switching are still deferred

The exported reviewed package is a derived artifact only. It is not automatically copied into the iOS app bundle, not served to visitors, and not used as a runtime data source. The existing bundled reviewed package remains the sole runtime truth per ADR-026.

**Why:** Runtime package switching requires careful consideration of versioning, cache invalidation, and error recovery. Adding it prematurely would increase regression risk for the visitor navigation flow. The export step proves the authoring pipeline works end-to-end without touching live navigation.

### 4. Editable fields are limited to room metadata

Only `displayName`, `category`, and `description` are editable in this phase. Graph structure (nodes, edges), entrance markers, route rendering config, and checkpoint markers are not editable. These structural edits require different validation logic and UI patterns.

## Consequences

- Generated draft artifacts remain immutable raw output.
- Room overrides are simple JSON, easy to inspect and debug.
- The summary endpoint merges overrides on read, so the iOS UI always shows the effective state.
- Export produces a complete reviewed package that matches the existing format.
- No runtime activation means zero regression risk for visitor navigation.
- Future phases can add graph editing, marker editing, and runtime switching incrementally.

## Alternatives Considered

- **Mutate authoring_config.generated.json in-place:** Simpler but destructive. Loses the original draft for comparison and re-processing.
- **Skip export, edit only:** Possible but makes room edits useless without a manual JSON copy step.
- **Full package activation:** Would require runtime package switching infrastructure, increasing scope and risk.
