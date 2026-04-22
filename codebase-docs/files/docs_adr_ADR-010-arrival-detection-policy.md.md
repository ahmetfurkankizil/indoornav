# File Dossier: ADR-010-arrival-detection-policy.md

## Path
`docs\adr\ADR-010-arrival-detection-policy.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-010: Arrival Detection and Session Completion Policy for V1

**Status:** Accepted  
**Date:** 2026-03-08

## Context

The app needs to know when a user has reached their destination to complete the navigation session, update the UI, and persist a visit record.

## Decision

V1 uses **arrow-progress-based arrival detection**:

1. Track the user's progress as a fraction of total arrow placements
2. ≥80% arrows passed → `ApproachingDestination`
3. ≥95% arrows passed (or distance to dest node < threshold) → `Arrived`

### Why Not Camera Position?

Live camera position from ARKit/ARCore VIO accumulates drift. For a 50m route, 1-2% drift means 0.5-1m error — too unreliable for precise proximity detection. Arrow-progress is deterministic and works identically in debug/simulated mode.

### V1 Progress Source

In v1, progress is driven by:
- **Simulated mode**: Explicit `advanceProgress()` calls (for demo)
- **Live mode**: Periodic progress advancement based on elapsed time and route dist
```

## Status
Mapped (Pass 3 Normalization)
