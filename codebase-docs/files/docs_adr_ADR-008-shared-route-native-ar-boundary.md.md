# File Dossier: ADR-008-shared-route-native-ar-boundary.md

## Path
`docs\adr\ADR-008-shared-route-native-ar-boundary.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
Shared Code               
```

## Status
Mapped (Pass 3 Normalization)
