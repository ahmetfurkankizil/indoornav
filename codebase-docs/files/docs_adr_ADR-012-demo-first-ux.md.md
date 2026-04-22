# File Dossier: ADR-012-demo-first-ux.md

## Path
`docs\adr\ADR-012-demo-first-ux.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-012: Demo-First Navigation UX Prioritization

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1 UX priorities: smooth demo flow > absolute correctness. The happy path (search → preview → AR → arrive → summary → history) must be bulletproof. Edge cases (drift, off-path, tracking loss) are handled gracefully but not perfectly.

Demo mode provides deterministic one-tap flow for investor presentations.

## Consequences

- Polished, predictable demo experience
- Real navigation approximately correct but not production-grade
- Clear debug labeling prevents confusion

```

## Status
Mapped (Pass 3 Normalization)
