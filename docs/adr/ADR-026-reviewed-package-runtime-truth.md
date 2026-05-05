# ADR-026: Reviewed Package Is Runtime Truth; Generated Draft Is Authoring Input Only

**Status:** Accepted
**Date:** 2026-03-22
**Deciders:** VecturAI iOS team
**Relates to:** ADR-025, ADR-003, ADR-012

## Context

Phase 1 (ADR-025) established the correct iOS user flow but still loaded building data from `authoring_config.generated.json` — a draft auto-generated from GLB preprocessing. This caused:

1. Generic "Zone A / Zone B" labels appearing as destination names.
2. A dense graph with unrealistic shortcut edges through walls.
3. Auto-generated room data treated as runtime truth without human review.
4. No clear separation between authoring artifacts and production data.

## Decision

### Reviewed package is the only runtime truth

The iOS app loads exclusively from a **reviewed multi-file package** containing:

| File | Purpose |
|---|---|
| `manifest.json` | Package metadata, version, review status |
| `rooms.json` | Destination rooms with real display names |
| `nav_graph.json` | Minimal, human-verified navigation graph |
| `entrance_markers.json` | Entrance marker positions |
| `route_rendering.json` | Arrow rendering configuration |

### Generated draft is authoring input only

The `authoring_config.generated.json` remains as an **offline authoring artifact** produced by the preprocessor. It is:
- Never loaded at runtime by the iOS app.
- Used only as a starting point for human review.
- Kept in `sample/demo-building/draft/` for reference.

### Package loading must fail explicitly

If the reviewed package is missing or corrupt, the app shows a clear error state. There is no silent fallback to draft data or invented destinations.

### Validation is mandatory before bundling

A validation script (`scripts/validate-reviewed-package.sh`) must pass before the package is bundled into the iOS app. It checks graph connectivity, node references, and data integrity.

## Consequences

- Destination list shows only real, human-verified room names.
- The navigation graph reflects actual walkable paths, not mesh-derived shortcuts.
- Package changes are reviewable in git (human-readable JSON files).
- A documented manual correction workflow exists for creating/updating packages.
- The preprocessor's role is clearly scoped to draft generation, not runtime truth.
