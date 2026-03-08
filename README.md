# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor AR navigation for Android & iOS.

**v1.5.0 — Live Progress** | [Demo Script](docs/demo/demo-script.md) | [Marker Guide](docs/demo/marker-guide.md)

---

## Quick Start

```bash
git clone <repo-url> && cd vecturai
make help                  # show all targets
make test-preprocessor     # run ~123 tests
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
| History persistence | ✓ | ✓ |

## Config Profiles

| Profile | Debug | Simulate | Preload | 
|---------|-------|----------|---------|
| Dev | ✓ | ✓ | ✓ |
| Demo | ✗ | ✓ | ✓ |
| Release | ✗ | ✗ | ✗ |

## ADRs (1–18)

| 1–9 | KMP, routing, preprocessing, marker alignment, single-floor, assisted authoring |
|-----|---|
| 10–12 | Arrow-progress arrival, local history, demo-first UX |
| 13–15 | Build strategy, visual polish, QA testing |
| 16–18 | Controlled-route hardening, route-relative progress, recenter/rescan |

## QA Docs

- [Demo Smoke Checklist](docs/qa/demo-smoke-checklist.md) — scripted path
- [Live AR Checklist](docs/qa/live-ar-smoke-checklist.md) — real device
- [Release Checklist](docs/qa/release-checklist.md)
- [Demo Script](docs/demo/demo-script.md) — 2–3 min presentation
- [Marker Guide](docs/demo/marker-guide.md) — print + placement

## Known Limitations (v1.5)

- VIO drift: ~1–2% on routes < 50m (acceptable for demo)
- Single marker, single floor
- Progress is route-relative projection, not absolute indoor localization
- 3D arrows: placeholder geometry
- History: local JSON file, no cloud sync
- No relocalization, multi-marker correction, or SLAM
