# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor AR navigation for Android & iOS.

**v1.7.0-rc1** | [First-Day Setup](docs/setup/first-day-setup.md) | [Architecture](docs/handoff/architecture-summary.md)

---

## Quick Start

```bash
git clone <repo-url> && cd vecturai
make help                  # show all targets
make test-preprocessor     # run ~170 tests
make verify-all            # full verification (tests + build + package)
make android-debug         # build Android APK
make ios-open              # open Xcode
```

New? Start with the [First-Day Setup Guide](docs/setup/first-day-setup.md).

## Architecture

```
Authoring Config → Preprocessor → Building Package
                                        ↓
Native AR (ARKit/ARCore)          PackageLoader → RouteEngine → ArrowMapper
  │ CameraPose @ 2Hz                                                ↓
  ↓                                                          ProgressEstimator
AlignmentTransform ← MarkerDetector                                ↓
  ↓                                                          ArrivalDetector
CorrectionCoordinator ← Checkpoint Observations                    ↓
  ↓                                                          SessionCoordinator
OffRouteDetector → Recovery Recommendations                        ↓
                                                              HistoryRepository
```

Full architecture with Mermaid diagram: [Architecture Summary](docs/handoff/architecture-summary.md)

## What Works

| Feature | Demo | Live |
|---------|------|------|
| Search + preview | ✓ | ✓ |
| AR route arrows | ✓ | ✓ |
| Progress tracking | Buttons | Camera |
| Remaining distance | ✓ | ✓ |
| Arrival detection | ≥95% | Position + progress |
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

## Verification

```bash
make verify-all          # everything: tests + Android build + package
make verify-package      # demo package integrity only
make verify-ios          # local iOS build
make test-preprocessor   # unit tests only (~170)
```

CI runs automatically on push/PR to `main` via GitHub Actions.

## Documentation

### Getting Started
- [First-Day Setup](docs/setup/first-day-setup.md) — clone → build → run in 15 min

### Demo & Presentation
- [Presenter Guide](docs/demo/presenter-guide.md) — what to say / show
- [Operator Guide](docs/demo/operator-guide.md) — setup / troubleshoot
- [Presentation Cheatsheet](docs/demo/presentation-cheatsheet.md) — 30s / 2m / 5m scripts
- [Demo Risks & Fallbacks](docs/demo/demo-risks-and-fallbacks.md) — what can go wrong

### QA & Testing
- [Demo Smoke Checklist](docs/qa/demo-smoke-checklist.md) — scripted path
- [Live AR Checklist](docs/qa/live-ar-smoke-checklist.md) — real device
- [Checkpoint Checklist](docs/qa/checkpoint-marker-smoke-checklist.md) — checkpoint flow
- [Regression Matrix](docs/qa/regression-matrix.md) — 10 demo-critical features
- [RC Checklist](docs/release/rc-checklist.md) — release candidate gates

### Release
- [Release Process](docs/release/release-process.md) — versioning + RC flow
- [Release Notes Template](docs/release/release-notes-template.md)

### Architecture
- [Architecture Summary](docs/handoff/architecture-summary.md) — diagram + components
- [Marker Placement](docs/demo/checkpoint-marker-placement.md) — physical setup

### ADRs (1–24)

| 1–9 | KMP, routing, preprocessing, marker alignment, single-floor, assisted authoring |
|-----|---|
| 10–12 | Arrow-progress arrival, local history, demo-first UX |
| 13–15 | Build strategy, visual polish, QA testing |
| 16–18 | Controlled-route hardening, route-relative progress, recenter/rescan |
| 19–21 | Checkpoint-marker correction, confidence-scored progress, recovery/off-route |
| 22–24 | RC stabilization, CI/regression policy, presentation/operator handoff |

## Known Limitations (v1.7)

- VIO drift: ~1–2% on routes < 50m (checkpoint markers reduce visible impact)
- Checkpoint correction bounded to max 2m / 15° per observation
- Single floor only
- Progress is route-relative projection, not absolute indoor localization
- 3D arrows: placeholder geometry
- History: local JSON file, no cloud sync
- Controlled-route assumption: authored nav-graph, no dynamic obstacles
