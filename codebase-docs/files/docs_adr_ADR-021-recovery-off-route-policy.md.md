# File Dossier: ADR-021-recovery-off-route-policy.md

## Path
`docs\adr\ADR-021-recovery-off-route-policy.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-021: Recovery and Off-Route Policy for v1.6

**Status:** Accepted  
**Date:** 2026-03-10

## Context

Users may wander off the controlled route due to VIO drift, wrong turns, or environmental confusion. v1.5 has no structured recovery flow.

## Decision

Implement pragmatic off-route detection based on:
- Lateral deviation from route polyline (thresholds: 2 m / 4 m / 6 m)
- Stale pose updates (> 3 s without new pose)
- Tracking-limited signals from native AR runtime
- Repeated large correction magnitudes

Recovery is **recommendation-based**, not auto-cancellation:
- `CONTINUE` — minor drift, likely recoverable
- `RESCAN_MARKER` — suggest user scan nearest marker
- `MOVE_TOWARD_ROUTE` — user appears off-route
- `USE_DEMO_MODE` — debug/demo fallback (only in debug builds)

## Consequences

- Sessions are never auto-cancelled due to drift
- User-facing UI shows helpful guidance
- Operators can use debug panel to diagnose issues
- False-positive rate is controlled by conservative th
```

## Status
Mapped (Pass 3 Normalization)
