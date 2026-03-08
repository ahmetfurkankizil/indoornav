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
- **Live mode**: Periodic progress advancement based on elapsed time and route distance (approximate, honest about limitations)

### Future Upgrade

Replace progress estimation with true camera-position-based proximity when relocalization or multi-marker correction improves positional accuracy.

## Consequences

- Arrival is deterministic and testable
- Works in demo/simulated mode identically to live
- Not position-accurate — user might trigger arrival slightly before/after physical arrival
- Sufficient for investor demo quality
