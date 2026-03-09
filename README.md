# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor AR navigation for Android & iOS.

**v1.6.0 — Checkpoint Correction** | [Demo Script](docs/demo/demo-script.md) | [Marker Guide](docs/demo/marker-guide.md) | [Checkpoint Placement](docs/demo/checkpoint-marker-placement.md)

---

## Quick Start

```bash
git clone <repo-url> && cd vecturai
make help                  # show all targets
make test-preprocessor     # run ~130 tests
make android-debug         # build Android APK
make ios-open              # open Xcode
```

## Architecture

```
Native AR (ARKit / ARCore)
  │ CameraPose @ 2Hz
  ▼
AlignmentTransform.inverseTransformPoint()
  │ Building-local position
  ▼
ProgressEstimator (polyline projection)
  │ Progress fraction + remaining distance
  ▼
CorrectionCoordinator ← checkpoint marker observations
  │ Bounded alignment correction (max 2m / 15°)
  ▼
OffRouteDetector → NavigationConfidenceState
  │ Recovery recommendations
  ▼
ArrivalDetector → Session → History
```

## What Works

| Feature | Scripted | Live |
|---------|----------|------|
| Search + preview | ✓ | ✓ |
| AR route arrows | ✓ | ✓ |
| Progress tracking | Manual buttons | Camera movement |
| Remaining distance | ✓ | ✓ |
| Arrival detection | ≥95% progress | Position + progress |
| Rescan/recenter | — | ✓ |
| Checkpoint correction | — | ✓ (optional) |
| Confidence/off-route | — | ✓ |
| History persistence | ✓ | ✓ |

## Config Profiles

| Profile | Debug | Simulate | Preload | 
|---------|-------|----------|---------| 
| Dev | ✓ | ✓ | ✓ |
| Demo | ✗ | ✓ | ✓ |
| Release | ✗ | ✗ | ✗ |

## ADRs (1–21)

| 1–9 | KMP, routing, preprocessing, marker alignment, single-floor, assisted authoring |
|-----|---|
| 10–12 | Arrow-progress arrival, local history, demo-first UX |
| 13–15 | Build strategy, visual polish, QA testing |
| 16–18 | Controlled-route hardening, route-relative progress, recenter/rescan |
| 19–21 | Checkpoint-marker correction, confidence-scored progress, recovery/off-route |

## QA Docs

- [Demo Smoke Checklist](docs/qa/demo-smoke-checklist.md) — scripted path
- [Live AR Checklist](docs/qa/live-ar-smoke-checklist.md) — real device
- [Checkpoint Marker Checklist](docs/qa/checkpoint-marker-smoke-checklist.md) — checkpoint flow
- [Release Checklist](docs/qa/release-checklist.md)
- [Demo Script](docs/demo/demo-script.md) — 2–3 min presentation
- [Marker Guide](docs/demo/marker-guide.md) — print + placement
- [Checkpoint Placement](docs/demo/checkpoint-marker-placement.md) — checkpoint marker guide

## Known Limitations (v1.6)

- VIO drift: ~1–2% on routes < 50m (checkpoint markers reduce visible impact)
- Checkpoint correction bounded to max 2m / 15° per observation
- Single floor only
- Progress is route-relative projection, not absolute indoor localization  
- No full relocalization, SLAM, or global visual localization
- 3D arrows: placeholder geometry
- History: local JSON file, no cloud sync
- Controlled-route assumption: authored nav-graph, no dynamic obstacles
