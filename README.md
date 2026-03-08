# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor navigation with AR guidance for Android & iOS.

**v1.2.0 — Demo Ready** | [Demo Script](docs/demo/demo-script.md) | [Demo Setup](docs/demo/demo-setup.md)

---

## Quick Start

```bash
git clone <repo-url> && cd vecturai
make help                  # show all build targets
make test-preprocessor     # run ~107 tests
make android-debug         # build Android APK
make ios-open              # open Xcode for iOS
```

## Build Targets

| Command | What it does |
|---------|-------------|
| `make test-all` | Run all tests |
| `make test-preprocessor` | Run nav-preprocessor tests (~107) |
| `make android-debug` | Build Android debug APK |
| `make android-release` | Build Android release APK |
| `make android-install` | Install debug APK to device |
| `make ios-open` | Open Xcode project |
| `make ios-framework` | Build shared framework for iOS |
| `make preprocess` | Run preprocessor on sample building |
| `make clean` | Clean all build outputs |

---

## Architecture

```
┌────────────────────────────────────────────┐
│              Mobile Apps                   │
│  Android (ARCore)    iOS (ARKit/RealityKit)│
├────────────────────────────────────────────┤
│          Shared KMP                        │
│  NavigationSessionCoordinator              │
│  ├─ ArNavigationCoordinator               │
│  ├─ ArrivalDetector                       │
│  ├─ RouteToArrowMapper                    │
│  ├─ DijkstraRouteEngine                   │
│  ├─ HistoryRepository                     │
│  └─ DemoMode + DemoPackageProvider        │
├────────────────────────────────────────────┤
│  nav-preprocessor (JVM CLI)               │
└────────────────────────────────────────────┘
```

## End-to-End Flow

```
Home → Search → Route Preview → AR Navigation → Arrived → Summary → History
```

## Config Profiles

| Profile | Debug Overlays | Simulate Scan | Preload Package | Demo Label |
|---------|---------------|--------------|-----------------|------------|
| **Dev** | ✓ | ✓ | ✓ | ✓ |
| **Demo** | ✗ | ✓ | ✓ | ✓ |
| **Release** | ✗ | ✗ | ✗ | ✗ |

Set via `AppConfig.current = AppConfig.Demo` at startup.

## History Persistence

JSON-file-backed (`JsonFileHistoryRepository`) — survives app restarts. Platform provides read/write functions for local storage path. Graceful recovery from malformed data.

## ADRs

| # | Decision |
|---|----------|
| 1–6 | KMP, graph routing, preprocessing, marker init, single-floor, assisted authoring |
| 7–9 | Marker alignment, shared/native boundary, marker-init over global localization |
| 10 | Arrow-progress arrival detection |
| 11 | Local-first visit history |
| 12 | Demo-first UX prioritization |
| 13 | Demo-build and release strategy |
| 14 | Visual polish priorities |
| 15 | QA and deterministic demo testing |

## QA

- [Demo Smoke Checklist](docs/qa/demo-smoke-checklist.md)
- [Release Checklist](docs/qa/release-checklist.md)
- [Demo Script (2–3 min)](docs/demo/demo-script.md)
- [Demo Setup](docs/demo/demo-setup.md)

## Known Limitations (v1)

- Arrival: progress-based proxy, not camera-position-based
- 3D arrows: placeholder geometry (boxes/spheres)
- History: local JSON file, no cloud sync
- Marker: requires physical 21cm printed marker (or Simulate Scan)
- Drift: no relocalization or multi-marker correction
- Single floor, single building
