# File Dossier: ADR-027-rolling-lookahead-ar-guidance.md

## Path
`docs\adr\ADR-027-rolling-lookahead-ar-guidance.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-027: AR Guidance Renders a Rolling Forward Route Slice Driven by Route Progress

**Status:** Accepted
**Date:** 2026-03-22
**Deciders:** Vectura AI iOS team
**Relates to:** ADR-026, ADR-025

## Context

Phase 2 established the reviewed package as runtime truth, but the AR renderer still placed every arrow at full opacity simultaneously — a "static carpet" from entrance to destination. This caused:

1. Visual clutter: dozens of arrows visible at once, making it hard to see the next step.
2. No sense of progress: the user couldn't tell which arrows they had passed.
3. No arrival detection: navigation ended only when a timer or manual action triggered it.
4. Rendering config in `route_rendering.json` was loaded but not used at runtime.

## Decision

### Rolling lookahead with fade-behind

The renderer maintains three arrow states:

| State | Condition | Visual |
|---|---|---|
| **Active** | Within `[userDistance, userDistance + lookahead]` | Full size, full opacity |
| **Fading** | Wi
```

## Status
Mapped (Pass 3 Normalization)
