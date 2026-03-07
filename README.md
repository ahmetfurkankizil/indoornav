# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor navigation with AR guidance for Android and iOS.

VecturAI guides users through buildings using augmented reality. Users scan an entrance marker, select a destination, and follow 3D arrows overlaid on the camera view — supplemented by textual step-by-step directions and a strong non-AR UI.

**Status:** v1.0.0 — Working authoring → preprocessing → loading → routing pipeline

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Mobile Apps                          │
│  ┌──────────────────┐     ┌──────────────────────┐     │
│  │   Android App    │     │      iOS App          │     │
│  │  Compose MP + AR │     │  Compose MP + AR      │     │
│  └──────────────────┘     └──────────────────────┘     │
├─────────────────────────────────────────────────────────┤
│                   Shared (KMP)                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │   core   │ │ feature- │ │ feature- │ │ feature- │  │
│  │ (domain, │ │  search  │ │ routing  │ │ history  │  │
│  │ routing, │ │          │ │          │ │          │  │
│  │ loading) │ │          │ │          │ │          │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Tools                                │
│  ┌──────────────────────────────────────┐              │
│  │  nav-preprocessor (JVM CLI)          │              │
│  │  authoring_config.json + .glb        │              │
│  │  → building package + debug SVG      │              │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

### Key Decisions

| # | Decision | ADR |
|---|----------|-----|
| 1 | KMP + Compose MP + native AR shells | [ADR-001](docs/adr/ADR-001-kmp-shared-ui-native-ar.md) |
| 2 | Graph routing (not raw point cloud) | [ADR-002](docs/adr/ADR-002-graph-routing.md) |
| 3 | Offline preprocessing from Polycam | [ADR-003](docs/adr/ADR-003-offline-preprocessing.md) |
| 4 | Entrance marker-based initialization | [ADR-004](docs/adr/ADR-004-entrance-marker-init.md) |
| 5 | Single-floor static-map MVP | [ADR-005](docs/adr/ADR-005-single-floor-mvp.md) |
| 6 | Assisted graph authoring for v1 | [ADR-006](docs/adr/ADR-006-assisted-authoring.md) |

---

## Authoring Workflow (V1)

V1 uses a **semi-automatic assisted workflow** (see [ADR-006](docs/adr/ADR-006-assisted-authoring.md)):

```
1. Scan building with Polycam → export .glb
2. Author authoring_config.json (nodes, edges, rooms, markers)
3. Run nav-preprocessor CLI
4. Inspect plan_view_debug.svg in browser
5. Iterate until graph matches the building
6. Deploy package/ to backend
```

### Creating an Authoring Config

The `authoring_config.json` defines the building's navigation data. See [docs/contracts/README.md](docs/contracts/README.md) for full format details.

Key sections:
- **nodes** — waypoints in the building (x, y, z in meters, Y-up)
- **edges** — connections between nodes with traversal cost
- **rooms** — searchable destinations linked to entry nodes
- **entranceMarkers** — AR alignment markers with physical dimensions
- **routeRendering** — arrow spacing, lookahead, thresholds

Example: see [`sample/demo-building/authoring_config.json`](sample/demo-building/authoring_config.json)

### Coordinate Conventions

| Property | Convention |
|----------|-----------|
| Unit | Meters |
| Handedness | Right-handed |
| Up axis | Y-up |
| Floor plane | Y = 0 |
| Rotation | Degrees (CW from above) |

See [docs/contracts/README.md](docs/contracts/README.md) for full details.

---

## Running the Preprocessor

```bash
# Process sample building
./gradlew :tools:nav-preprocessor:run \
  --args="--input sample/demo-building/scan.glb \
          --config sample/demo-building/authoring_config.json \
          --output sample/demo-building/package/ \
          --overwrite"
```

Output:
```
╔══════════════════════════════════════════════╗
║   VecturAI Navigation Preprocessor v1.0      ║
╚══════════════════════════════════════════════╝

[1/5] Inspecting asset... ✓ (12 B, GLB v2)
[2/5] Loading authoring config... ✓ (12 nodes, 11 edges, 6 rooms)
[3/5] Validating graph... ✓
[4/5] Exporting package... ✓ (6 files)
[5/5] Exporting debug artifacts... ✓

Package exported successfully to: sample/demo-building/package/
```

### Package Output

```
package/
├── manifest.json           # Metadata, version, file list
├── nav_graph.json          # Nodes and edges for pathfinding
├── rooms.json              # Searchable room definitions
├── entrance_markers.json   # AR alignment markers
├── route_rendering.json    # Arrow/path rendering config
├── preview.glb             # Copy of Polycam scan
├── graph_debug.json        # Debug: computed graph metadata
└── plan_view_debug.svg     # Debug: visual plan-view
```

---

## How the App Consumes a Package

```
JSON files → BuildingPackageLoader → BuildingPackage
                                          ↓
                                    InMemoryPackageStore
                                          ↓
                                  DefaultBuildingRepository
                                    ↓           ↓
                             SearchUseCase  RoutePreviewUseCase
                                                ↓
                                         DijkstraRouteEngine
```

1. Package JSON files are loaded (from bundled assets or remote download)
2. `BuildingPackageLoader` deserializes into typed Kotlin models
3. `InMemoryPackageStore` holds the loaded package in memory
4. `DefaultBuildingRepository` exposes rooms, nav graph, markers
5. Feature use cases query the repository for real data
6. `DijkstraRouteEngine` computes shortest paths on the nav graph

---

## Running Tests

```bash
./gradlew :tools:nav-preprocessor:test
```

43 tests covering:
- Graph validation (duplicates, connectivity, invalid refs)
- Authoring config structural validation
- Contract serialization roundtrips
- Dijkstra shortest-path correctness
- Package export file generation
- Full pipeline integration

---

## Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `shared/core` | Domain models, routing engine, repositories, loading, store |
| `shared/feature-search` | Room search with keyword/alias scoring |
| `shared/feature-routing` | Route computation orchestration |
| `shared/feature-preview` | 2D route preview data |
| `shared/designsystem` | Theme, components, 5 Compose screens |
| `apps/androidApp` | Android entry + ARCore placeholder |
| `apps/iosApp` | iOS entry + ARKit placeholder |
| `tools/nav-preprocessor` | CLI: authoring config + .glb → building package |
| `sample/demo-building` | Sample building for development |

---

## What Remains Deferred

- ❌ Multi-floor navigation
- ❌ Admin tools / building management dashboard
- ❌ Dynamic obstacles / real-time blockage
- ❌ Cloud anchors
- ❌ Advanced AR occlusion
- ❌ Automatic graph extraction from .glb geometry
- ❌ Voice guidance
- ❌ Multi-building search

All deferred items can be added incrementally. Future automatic extraction plugs into the same pipeline by generating `authoring_config.json` format.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Shared logic | Kotlin Multiplatform 2.1.10 |
| Shared UI | Compose Multiplatform 1.7.3 |
| Routing | Dijkstra shortest-path |
| Android AR | ARCore 1.46 (placeholder) |
| iOS AR | ARKit + RealityKit (placeholder) |
| Networking | Ktor 3.0.3 |
| DI | Koin 4.0.0 |
| Serialization | kotlinx.serialization 1.7.3 |
| CLI | Clikt 5.0.2 |
