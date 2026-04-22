# File Dossier: ADR-005-single-floor-mvp.md

## Path
`docs\adr\ADR-005-single-floor-mvp.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-005: Single-Floor Static-Map MVP Scope

**Status:** Accepted  
**Date:** 2026-03-08

## Context

Building a full indoor navigation system covering arbitrary buildings with multiple floors, dynamic obstacles, real-time updates, and advanced AR features would take years. We need to define a tight MVP scope that demonstrates value to investors while remaining achievable by a 5-person team.

## Decision

The MVP is scoped to:

### In Scope
- **One building** at a time
- **One floor** only (ground floor)
- **Static map** — building layout does not change at runtime
- **Offline preprocessing** — Polycam scan → JSON contracts via CLI
- **Entrance marker scanning** for localization
- **Shortest-path routing** (Dijkstra)
- **3D arrow overlay** in AR camera view
- **Textual guidance** alongside AR view
- **Room search** by name
- **Visit history** persistence
- **Route preview** (2D, non-AR)
- **Building data caching** after first download

### Explicitly Out of Scope (Deferred)
- **Multi-
```

## Status
Mapped (Pass 3 Normalization)
