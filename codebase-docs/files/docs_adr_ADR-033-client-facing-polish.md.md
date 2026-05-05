# File Dossier: ADR-033-client-facing-polish.md

## Path
`docs\adr\ADR-033-client-facing-polish.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-033: Client-Facing Polish — AR Overlay, Haptics, Microcopy

**Status:** Accepted
**Date:** 2026-04-13
**Deciders:** VecturAI team
**Relates to:** ADR-025 (iOS truth-path navigation flow), ADR-027 (rolling lookahead AR guidance), ADR-012 (demo-first UX)

## Context

The visitor-facing flow works end-to-end (Phases 1–6), but the screens use technical wording ("Detecting entrance poster...", "Follow the arrows"), the AR overlay shows basic progress without proactive turn guidance, and there is no haptic feedback at navigation milestones. Before client demos, the product needs to feel more premium and trustworthy without changing any runtime mechanics.

## Decision

### 1. Next-action guidance derived from existing arrow data

The `ArrowPlacementData` array in `LoadedPackage.arrows` already contains the arrow `type` (`.turnLeft`, `.turnRight`, `.destination`, `.follow`) and `cumulativeDistance`. The ViewModel scans ahead of `userCumulativeDistance` to find the next non-follow arrow a
```

## Status
Mapped (Pass 3 Normalization)
