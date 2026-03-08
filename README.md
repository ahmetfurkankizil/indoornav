# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor navigation with AR guidance for Android and iOS.

VecturAI guides users through buildings using augmented reality. Users scan an entrance marker, select a destination, and follow 3D arrows overlaid on the camera view — supplemented by textual step-by-step directions and a strong non-AR UI.

**Status:** v1.1.0 — Working pipeline + real AR integration shell

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Mobile Apps                          │
│  ┌──────────────────┐     ┌──────────────────────┐     │
│  │   Android App    │     │      iOS App          │     │
│  │  ARCore + native │     │  ARKit + RealityKit   │     │
│  └────────┬─────────┘     └────────┬─────────────┘     │
│           │    ArNavigationBridge   │                   │
├───────────┼─────────────────────────┼───────────────────┤
│           ▼    Shared (KMP)         ▼                   │
│  ┌──────────────────────────────────────────────┐      │
│  │  ArNavigationCoordinator (state machine)     │      │
│  │  RouteToArrowMapper (path → arrow placements)│      │
│  │  AlignmentTransform (building → AR coords)   │      │
│  │  DijkstraRouteEngine (shortest path)         │      │
│  │  BuildingPackageLoader + Repository          │      │
│  │  SearchUseCase + RoutePreviewUseCase          │      │
│  └──────────────────────────────────────────────┘      │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────┐              │
│  │  nav-preprocessor (JVM CLI)          │              │
│  │  authoring_config → building package │              │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

### ADRs

| # | Decision | Doc |
|---|----------|-----|
| 1 | KMP + Compose MP + native AR shells | [ADR-001](docs/adr/ADR-001-kmp-shared-ui-native-ar.md) |
| 2 | Graph routing (not raw point cloud) | [ADR-002](docs/adr/ADR-002-graph-routing.md) |
| 3 | Offline preprocessing | [ADR-003](docs/adr/ADR-003-offline-preprocessing.md) |
| 4 | Entrance marker initialization | [ADR-004](docs/adr/ADR-004-entrance-marker-init.md) |
| 5 | Single-floor static-map MVP | [ADR-005](docs/adr/ADR-005-single-floor-mvp.md) |
| 6 | Assisted graph authoring | [ADR-006](docs/adr/ADR-006-assisted-authoring.md) |
| 7 | Marker-based AR alignment | [ADR-007](docs/adr/ADR-007-marker-ar-alignment.md) |
| 8 | Shared route, native AR rendering | [ADR-008](docs/adr/ADR-008-shared-route-native-ar-boundary.md) |
| 9 | Marker-init, not global localization | [ADR-009](docs/adr/ADR-009-marker-init-not-global-localization.md) |

---

## How AR Navigation Works

### Alignment Model

```
1. User scans entrance marker → ARKit/ARCore detects reference image
2. Platform provides marker's 6-DoF pose in AR world coords
3. We know marker's position in building coords (from package)
4. Compute: T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹
5. All building-local arrow positions → apply transform → AR world
```

See [docs/contracts/ar-alignment.md](docs/contracts/ar-alignment.md) for the full transform derivation.

### Shared vs Native Boundary

| Shared (KMP) | Native (Swift/Kotlin) |
|--------------|----------------------|
| Route computation | AR session lifecycle |
| Arrow placement generation | Reference image detection |
| State machine (coordinator) | Coordinate transform application |
| Textual guidance | 3D entity creation/rendering |
| Alignment transform math | Camera feed |

### State Machine

```
Idle → WaitingForMarker → MarkerDetected → Aligned → RenderingRoute
                                                    ↕
                                              TrackingLimited
```

### Debug/Simulation Mode

Both iOS and Android include a "Simulate Scan" button that bypasses live marker detection:
- Uses the first entrance marker from the loaded package
- Creates identity alignment (marker at building origin)
- Immediately renders demo arrows
- Essential for development without physical markers

---

## Running the Preprocessor

```bash
./gradlew :tools:nav-preprocessor:run \
  --args="--input sample/demo-building/scan.glb \
          --config sample/demo-building/authoring_config.json \
          --output sample/demo-building/package/ \
          --overwrite"
```

---

## Running Tests

```bash
./gradlew :tools:nav-preprocessor:test
```

~64 tests covering:
- Graph validation, config validation
- Contract serialization roundtrips
- Dijkstra shortest-path correctness
- Package export + full pipeline integration
- **Route-to-arrow mapper** (spacing, turns, destination, edge cases)
- **Alignment transform** (identity, translation, rotation, marker invariants)
- **AR contract models** (serialization roundtrips, defaults)

---

## Marker Asset Workflow

See [docs/contracts/marker-assets.md](docs/contracts/marker-assets.md):
1. Print marker at 21×21 cm on matte paper
2. Mount at measured position → record coords
3. Add to `authoring_config.json`
4. Save reference PNG in `markers/` directory
5. Add to iOS AR Resources / Android `res/drawable/`

---

## What Remains Deferred

- ❌ Multi-floor navigation
- ❌ Production-grade 3D arrow models (using placeholder boxes/spheres)
- ❌ Drift correction / relocalization
- ❌ Multi-marker alignment averaging
- ❌ Dynamic obstacles
- ❌ Voice guidance
- ❌ Cloud anchors
- ❌ Advanced AR occlusion
- ❌ Automatic graph extraction from .glb

All items can be added incrementally on the current architecture.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Shared logic | Kotlin Multiplatform 2.1.10 |
| Shared UI | Compose Multiplatform 1.7.3 |
| Routing | Dijkstra shortest-path |
| Android AR | ARCore 1.46 |
| iOS AR | ARKit + RealityKit |
| DI | Koin 4.0.0 |
| Serialization | kotlinx.serialization 1.7.3 |
| CLI | Clikt 5.0.2 |
