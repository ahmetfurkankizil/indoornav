# File Dossier: ADR-015-qa-demo-testing.md

## Path
`docs\adr\ADR-015-qa-demo-testing.md`

## Type
Unit/Integration Test

## Role
Unit/Integration Test for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-015: QA and Deterministic Demo Testing Strategy

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

V1 QA uses: (1) documented smoke-test checklist for manual verification, (2) automated unit tests for shared logic (~92+ tests), (3) deterministic demo mode that follows a scripted path. No UI test framework in v1 — the demo script IS the acceptance test.

## Consequences
- Fast, practical QA for MVP
- Demo presentations are rehearsable
- No Espresso/XCUITest overhead

```

## Status
Mapped (Pass 3 Normalization)
