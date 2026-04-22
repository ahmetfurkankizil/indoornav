# File Dossier: ADR-009-marker-init-not-global-localization.md

## Path
`docs\adr\ADR-009-marker-init-not-global-localization.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-009: Marker-Initialized Navigation, Not Continuous Global Localization

**Status:** Accepted  
**Date:** 2026-03-08

## Context

Indoor localization is an active research area with approaches including:
- WiFi/BLE fingerprinting
- UWB ranging
- Visual place recognition
- Continuous visual-inertial SLAM with map persistence
- Cloud anchors (ARCore Cloud Anchors, Azure Spatial Anchors)

All of these add complexity, infrastructure cost, or both.

## Decision

V1 uses **marker-initialized, drift-tolerant navigation**:

1. The user's position is established **once** by scanning the entrance marker
2. After alignment, AR tracking relies on the platform's visual-inertial odometry (VIO)
3. No continuous global localization is attempted
4. Drift is acceptable for building-scale routes (typically < 100m total path)

### Why This Is Sufficient for MVP

- Building paths are short (< 100m typical)
- Modern VIO (ARKit/ARCore) drifts ~1-2% of distance traveled
- For a 50m route: ~0.5-1m drift —
```

## Status
Mapped (Pass 3 Normalization)
