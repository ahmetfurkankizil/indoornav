# File Dossier: ADR-032-admin-room-editing-and-export.md

## Path
`docs\adr\ADR-032-admin-room-editing-and-export.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-032: Admin Room Editing and Reviewed Package Export

**Status:** Accepted
**Date:** 2026-03-30
**Deciders:** Vectura AI team
**Relates to:** ADR-030 (admin draft-ingestion pipeline), ADR-031 (admin draft review read-only), ADR-026 (reviewed package runtime truth)

## Context

Phases 1 and 2 (ADR-030, ADR-031) established the admin draft-ingestion pipeline and read-only review. After a GLB scan is processed, the admin can see the generated draft summary, 2D previews, and auto-generated room candidates. However, the auto-generated room names (Zone A, Zone B, etc.) and categories ("unknown") are not suitable for visitor-facing navigation. The admin needs to correct room metadata before the draft can become a reviewed package.

## Decision

### 1. Room edits are stored as overrides, not mutations

Room metadata edits are persisted in a separate `room_overrides.json` file under the job directory, keyed by room ID. The generated `authoring_config.generated.json` in `output/` is never mut
```

## Status
Mapped (Pass 3 Normalization)
