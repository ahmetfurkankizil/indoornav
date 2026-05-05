# ADR-027: AR Guidance Renders a Rolling Forward Route Slice Driven by Route Progress

**Status:** Accepted
**Date:** 2026-03-22
**Deciders:** VecturAI iOS team
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
| **Fading** | Within `[userDistance - fadeDistance, userDistance]` | Shrinking, fading opacity |
| **Hidden** | All other arrows | Scale = 0 (invisible) |

All arrows are placed into the RealityKit scene at startup (scale zero). Each frame, `updateVisibility()` reclassifies arrows based on the user's cumulative distance along the route polyline.

### Cumulative distance per arrow

Each `ArrowPlacementData` stores its `cumulativeDistance` — the distance along the route from the start to that arrow's position. This is computed once during route generation in `BuildingPackageLoader`.

### Distance-based arrival detection

Arrival triggers when the user's 2D distance (x, z) to the destination door node is within `destinationThresholdMeters` (from `route_rendering.json`, default 1.5m). This replaces any progress-percentage-based approach.

### Runtime config from reviewed package

`lookaheadDistanceMeters`, `destinationThresholdMeters`, and `arrowHeightOffsetMeters` are read from `route_rendering.json` and used at runtime. Changing navigation feel requires only editing the JSON — no code changes.

## Consequences

- The user sees only the next few meters of arrows, mimicking turn-by-turn navigation.
- Passed arrows fade smoothly instead of disappearing abruptly.
- Arrival is detected spatially, not by progress percentage — works even if the user takes a non-ideal path.
- Rendering parameters are data-driven from the reviewed package.
- All arrows are pre-placed (not spawned dynamically), keeping the entity count stable and predictable.
