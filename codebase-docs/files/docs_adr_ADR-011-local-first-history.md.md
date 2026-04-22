# File Dossier: ADR-011-local-first-history.md

## Path
`docs\adr\ADR-011-local-first-history.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-011: Local-First Visit History

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1 persists visit history locally using an in-memory list with JSON serialization. No backend sync. History is a first-class user-facing feature for demo quality, not just debug logging.

## Consequences

- Instant read/write, no network dependency
- Data lost on app reinstall (acceptable for MVP)
- Future: add SqlDelight persistence + backend sync

```

## Status
Mapped (Pass 3 Normalization)
