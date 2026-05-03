# File Dossier: ADR-031-admin-draft-review-read-only.md

## Path
`docs\adr\ADR-031-admin-draft-review-read-only.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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

Add a summary endpoint (`GET /admin
```

## Status
Mapped (Pass 3 Normalization)
