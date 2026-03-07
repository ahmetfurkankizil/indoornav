# ADR-005: Single-Floor Static-Map MVP Scope

**Status:** Accepted  
**Date:** 2026-03-08

## Context

Building a full indoor navigation system covering arbitrary buildings with multiple floors, dynamic obstacles, real-time updates, and advanced AR features would take years. We need to define a tight MVP scope that demonstrates value to investors while remaining achievable by a 5-person team.

## Decision

The MVP is scoped to:

### In Scope
- **One building** at a time
- **One floor** only (ground floor)
- **Static map** — building layout does not change at runtime
- **Offline preprocessing** — Polycam scan → JSON contracts via CLI
- **Entrance marker scanning** for localization
- **Shortest-path routing** (Dijkstra)
- **3D arrow overlay** in AR camera view
- **Textual guidance** alongside AR view
- **Room search** by name
- **Visit history** persistence
- **Route preview** (2D, non-AR)
- **Building data caching** after first download

### Explicitly Out of Scope (Deferred)
- **Multi-floor navigation** — no stairs, elevators, floor switching
- **Admin tools** — no web dashboard for building management
- **Dynamic obstacles** — no real-time blockage reporting
- **Cloud anchors** — no Google/Apple cloud localization services
- **Advanced occlusion** — AR arrows do not hide behind real walls
- **Complex failure recovery** — basic error states only
- **Accessibility routing** — wheelchair-friendly paths
- **Voice guidance** — text-to-speech navigation instructions
- **Multi-building search** — no cross-building routing
- **Live user tracking** — no position sharing with others

## Consequences

### Positive
- Achievable in 3–4 months with a small team
- Demo-ready for investor presentations
- Core architecture is extensible — every out-of-scope item can be added later
- Modular code structure supports incremental feature addition

### Negative
- Limited to simple building layouts
- Users must restart navigation if they switch floors
- No real-time adaptability to building changes

### Strategy
Ship MVP → gather investor/user feedback → prioritize v2 features based on demand.
