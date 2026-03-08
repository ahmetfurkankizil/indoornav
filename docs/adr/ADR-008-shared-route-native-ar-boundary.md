# ADR-008: Shared Route Semantics, Native AR Rendering Boundary

**Status:** Accepted  
**Date:** 2026-03-08

## Context

The app must render navigation arrows in AR on both iOS and Android. Route computation and arrow placement logic should not be duplicated per platform, but AR rendering APIs (ARKit/RealityKit vs ARCore/Sceneform) are fundamentally different.

## Decision

**Shared code** (Kotlin Multiplatform) owns:
- Route computation (Dijkstra)
- Route-to-arrow placement generation (interpolation, turn markers, spacing)
- AR session state machine (`ArNavigationCoordinator`)
- Building-local coordinates of every arrow placement
- Textual guidance for non-AR UI

**Native code** (Swift / Kotlin-Android) owns:
- AR session lifecycle (start, pause, resume, stop)
- Reference image configuration and detection
- Coordinate transform application (building-local → AR-world)
- 3D entity creation and scene graph management
- Camera feed rendering

### Data Flow

```
Shared Code                    │  Native Code
                               │
RouteEngine.computeRoute() ───►│
RouteToArrowMapper.map() ──────│──► ArrowPlacement[] (building-local)
ArNavigationCoordinator ───────│──► ArSessionState
                               │
                               │  On marker detect:
                     ◄─────────│── onMarkerAligned(AlignmentTransform)
                               │
                               │  Each frame:
getRenderableRoute() ──────────│──► apply transform → place entities
                               │
```

## Consequences

### Positive
- Arrow placement logic written once, tested once
- Platform renderers are thin adapters
- State machine testable without AR hardware
- Future: swap renderer (e.g., Metal custom) without touching shared logic

### Negative
- Slight indirection for native developers reading the code
- Coordinate transform must be applied per-frame on native side
