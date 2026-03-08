# ADR-017: Route-Relative Progress Estimation

**Status:** Accepted  
**Date:** 2026-03-08

## Decision

Estimate navigation progress by projecting the user's building-local position onto the route polyline:

1. Native AR sends `CameraPose` (AR-world coords) periodically (~2 Hz)
2. Shared code applies `AlignmentTransform.inverseTransformPoint()` → building-local position
3. `ProgressEstimator` finds nearest point on route polyline
4. Cumulative distance to nearest point / total route distance → progress fraction
5. Monotonic guard: progress only increases (with small tolerance for noise)

## Why Not Camera-Position-Based Proximity?

VIO drift means absolute position accuracy degrades. Route-relative projection is more robust because it only needs *relative* forward motion along the route corridor, which VIO handles well.

## Consequences
- Progress advances when user walks forward along route
- Small lateral deviations tolerated
- Backwards movement suppressed by monotonic guard
- Not accurate for large off-route detours (acceptable for controlled demo)
