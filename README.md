# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor navigation app with AR guidance for Android and iOS.

VecturAI guides users through buildings using augmented reality. Users scan an entrance marker, select a destination, and follow 3D arrows overlaid on the camera view — supplemented by textual step-by-step directions and a strong non-AR UI.

**Status:** v0.1.0 — Architecture skeleton & code scaffold (MVP in development)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Mobile Apps                          │
│  ┌──────────────────┐     ┌──────────────────────┐     │
│  │   Android App    │     │      iOS App          │     │
│  │  ┌────────────┐  │     │  ┌────────────────┐  │     │
│  │  │  Compose   │  │     │  │   Compose MP   │  │     │
│  │  │  MP Shell  │  │     │  │   Shell (UIKit │  │     │
│  │  │            │  │     │  │   hosted)      │  │     │
│  │  └────────────┘  │     │  └────────────────┘  │     │
│  │  ┌────────────┐  │     │  ┌────────────────┐  │     │
│  │  │  ARCore    │  │     │  │  ARKit /       │  │     │
│  │  │  Native AR │  │     │  │  RealityKit    │  │     │
│  │  └────────────┘  │     │  └────────────────┘  │     │
│  └──────────────────┘     └──────────────────────┘     │
├─────────────────────────────────────────────────────────┤
│                   Shared (KMP)                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │   core   │ │ feature- │ │ feature- │ │ feature- │  │
│  │ (domain, │ │  search  │ │ routing  │ │ history  │  │
│  │ routing, │ │          │ │          │ │          │  │
│  │ store)   │ │          │ │          │ │          │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ feature- │ │  data-   │ │  data-   │ │ design-  │  │
│  │ preview  │ │  local   │ │  remote  │ │ system   │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Tools                                │
│  ┌──────────────────────────────────────┐              │
│  │  nav-preprocessor (JVM CLI)          │              │
│  │  Polycam .glb → JSON contracts       │              │
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

---

## Module Responsibilities

| Module | Type | Responsibility |
|--------|------|---------------|
| `apps/androidApp` | Android | Android entry point, Compose host, ARCore native AR |
| `apps/iosApp` | iOS | SwiftUI entry point, Compose host, ARKit native AR |
| `shared/core` | KMP | Domain models, routing engine, repositories, app store |
| `shared/feature-search` | KMP | Room search use case |
| `shared/feature-routing` | KMP | Route computation orchestration |
| `shared/feature-history` | KMP | Visit history persistence |
| `shared/feature-preview` | KMP | 2D route preview data |
| `shared/data-local` | KMP | SQLite cache via SqlDelight |
| `shared/data-remote` | KMP | HTTP client via Ktor |
| `shared/designsystem` | KMP + Compose | Theme, components, screens, navigation |
| `tools/nav-preprocessor` | JVM CLI | .glb scan → JSON contract conversion |

---

## Shared / Native Boundaries

The app has a clear boundary between shared KMP code and platform-native code:

**Shared (Kotlin Multiplatform):**
- All domain models and business logic
- Route computation (Dijkstra)
- State management (`AppStore` with `StateFlow`)
- All non-AR UI (via Compose Multiplatform)
- Networking (Ktor) and caching (SqlDelight)

**Native (per platform):**
- AR camera session management (ARCore / ARKit)
- 3D arrow rendering on camera view
- Entrance marker detection (QR + image reference)
- Coordinate transformation (nav-graph ↔ AR world space)

**Bridge pattern:** Native AR code observes `AppStore.navigationState` (a `StateFlow<NavigationState>`) and reports events (marker detected, destination reached) back to the shared layer via bridge classes (`ArBridge` on Android, `ARBridge` on iOS).

---

## Build & Run

### Prerequisites

- **JDK 17+**
- **Android Studio Arctic Fox+** with Android SDK 35
- **Xcode 15+** (for iOS)
- **Kotlin 2.1.10**

### Build Shared Modules

```bash
./gradlew :shared:core:build
```

### Run Android App

```bash
./gradlew :apps:androidApp:installDebug
```

### Run iOS App

Open `apps/iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

### Run Nav Preprocessor

```bash
./gradlew :tools:nav-preprocessor:run --args="--input scan.glb --output ./output/"
```

---

## Data Contracts

All contract schemas are in [`docs/contracts/`](docs/contracts/):

| Contract | Description |
|----------|-------------|
| [`manifest.schema.json`](docs/contracts/manifest.schema.json) | Building package metadata and version |
| [`nav_graph.schema.json`](docs/contracts/nav_graph.schema.json) | Navigation nodes and edges |
| [`rooms.schema.json`](docs/contracts/rooms.schema.json) | Room/POI definitions |
| [`entrance_markers.schema.json`](docs/contracts/entrance_markers.schema.json) | AR alignment markers |
| [`route_rendering.schema.json`](docs/contracts/route_rendering.schema.json) | Arrow and path rendering configuration |

---

## What Is Intentionally Deferred from v1

The following features are **explicitly out of scope** for the MVP (see [ADR-005](docs/adr/ADR-005-single-floor-mvp.md)):

- ❌ Multi-floor navigation (stairs, elevators)
- ❌ Admin tools / building management dashboard
- ❌ Dynamic obstacles / real-time blockage reporting
- ❌ Cloud anchors (Google/Apple cloud localization)
- ❌ Advanced AR occlusion (arrows hiding behind walls)
- ❌ Complex failure recovery
- ❌ Accessibility-weighted routing
- ❌ Voice / TTS guidance
- ❌ Multi-building search and cross-building routing
- ❌ Live user position sharing

All deferred items are designed to be addable incrementally thanks to the modular architecture. Search for `TODO` across the codebase to find all documented extension points.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Shared logic | Kotlin Multiplatform 2.1.10 |
| Shared UI | Compose Multiplatform 1.7.3 |
| Android AR | ARCore 1.46 |
| iOS AR | ARKit + RealityKit |
| Networking | Ktor 3.0.3 |
| Local storage | SqlDelight 2.0.2 |
| DI | Koin 4.0.0 |
| Serialization | kotlinx.serialization 1.7.3 |
| CLI | Clikt 5.0.2 |

---

## License

See [LICENSE](LICENSE).
