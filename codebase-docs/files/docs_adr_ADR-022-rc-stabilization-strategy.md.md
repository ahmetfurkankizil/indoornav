# File Dossier: ADR-022-rc-stabilization-strategy.md

## Path
`docs\adr\ADR-022-rc-stabilization-strategy.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-022: Release-Candidate Stabilization Strategy for v1.7

**Status:** Accepted  
**Date:** 2026-03-10

## Context

VecturAI has evolved through 7 development phases, adding features progressively. The codebase is functional but lacks formal release processes, CI automation, and team-ready handoff documentation. A release-candidate process is needed for reliable demo deployments.

## Decision

Adopt a lightweight RC stabilization strategy:

1. **Versioning**: Single source of truth in `AppVersion.kt` (semantic version + phase + date). Android/iOS version strings reference this.
2. **RC flow**: `main` → tag `v1.7.0-rc1` → validation → tag `v1.7.0` when promoted.
3. **Validation gates**: Automated (CI tests + Android build) + manual (iOS build, live demo, marker scan).
4. **No feature work** during RC — only fixes, docs, and stability improvements.
5. **Demo-critical regression** is the primary quality signal — if the demo breaks, the RC fails.

## Consequences

- Predictable, repeata
```

## Status
Mapped (Pass 3 Normalization)
