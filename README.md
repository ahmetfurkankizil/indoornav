# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor navigation with AR guidance for Android and iOS.

VecturAI guides users through buildings using augmented reality. Users scan an entrance marker, select a destination, and follow 3D arrows overlaid on the camera view — supplemented by textual directions and a strong non-AR UI.

**Status:** v1.2.0 — End-to-end MVP demo flow

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                  Mobile Apps                         │
│  ┌───────────────┐       ┌─────────────────────┐    │
│  │  Android App  │       │     iOS App          │    │
│  │  ARCore       │       │  ARKit + RealityKit  │    │
│  └──────┬────────┘       └──────┬──────────────┘    │
├─────────┼───────────────────────┼────────────────────┤
│         ▼   Shared (KMP)        ▼                    │
│  ┌────────────────────────────────────────────┐     │
│  │  NavigationSessionCoordinator              │     │
│  │  ├─ ArNavigationCoordinator (AR state)     │     │
│  │  ├─ ArrivalDetector (progress-based)       │     │
│  │  ├─ RouteToArrowMapper (path → arrows)     │     │
│  │  ├─ DijkstraRouteEngine                    │     │
│  │  ├─ HistoryRepository + VisitRecord        │     │
│  │  └─ DemoMode (one-tap demo flow)           │     │
│  └────────────────────────────────────────────┘     │
├──────────────────────────────────────────────────────┤
│  nav-preprocessor (JVM CLI)                          │
│  authoring_config → building package                 │
└──────────────────────────────────────────────────────┘
```

### ADRs

| # | Decision |
|---|----------|
| 1–6 | KMP, graph routing, preprocessing, marker init, single-floor MVP, assisted authoring |
| 7 | Marker-based AR world alignment |
| 8 | Shared route, native AR rendering boundary |
| 9 | Marker-init, not global localization |
| 10 | Arrow-progress arrival detection |
| 11 | Local-first visit history |
| 12 | Demo-first UX prioritization |

---

## End-to-End Flow

```
Home → Search → Route Preview → Start AR
                                    │
                         Simulate / Real Scan
                                    │
                              Navigating
                           (arrows + progress)
                                    │
                          Approaching → Arrived
                                    │
                           Session Summary
                                    │
                          History (persisted)
```

### Session Lifecycle

| State | Trigger |
|-------|---------|
| Created | User taps "Start AR" from route preview |
| WaitingForMarker | AR session running, scanning |
| Aligned | Marker detected (or simulated) |
| Navigating | Route rendered, progress updating |
| Approaching | ≥80% progress |
| Arrived | ≥95% progress or distance < 1.5m |
| Ended | User manually ends or arrives |

### Completion Statuses

`CompletedAtDestination`, `EndedManually`, `CancelledBeforeAlignment`, `CancelledAfterAlignment`, `LostTrackingEnded`, `DemoCompleted`

---

## AR Alignment

`T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹` — see [ar-alignment.md](docs/contracts/ar-alignment.md).

### V1 Arrival Detection (Honest)

Progress-based, not position-based. V1 cannot rely on camera position due to VIO drift. Arrow-index progress serves as a deterministic proxy. Architecture allows later upgrade to true proximity detection.

---

## Running

```bash
# Preprocessor
./gradlew :tools:nav-preprocessor:run \
  --args="--input scan.glb --config authoring_config.json --output ./package/ --overwrite"

# Tests (~92 tests)
./gradlew :tools:nav-preprocessor:test
```

---

## What Remains Deferred

- ❌ Production 3D arrow models (placeholder boxes/spheres)
- ❌ Position-based arrival (using progress proxy)
- ❌ Drift correction / relocalization
- ❌ Backend history sync
- ❌ Multi-floor, dynamic obstacles, voice, cloud anchors
- ❌ Persistent (SqlDelight) history storage

All items can be added incrementally on the current architecture.
