# File Dossier: ADR-026-reviewed-package-runtime-truth.md

## Path
`docs\adr\ADR-026-reviewed-package-runtime-truth.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
| `nav_graph.json` 
```

## Status
Mapped (Pass 3 Normalization)
