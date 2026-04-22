# File Dossier: ADR-007-marker-ar-alignment.md

## Path
`docs\adr\ADR-007-marker-ar-alignment.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-007: Marker-Based AR World Alignment for V1

**Status:** Accepted  
**Date:** 2026-03-08

## Context

Indoor AR navigation requires mapping between the building's coordinate system (used by the navigation graph) and the AR runtime's world coordinate system (established when the AR session starts). Without this alignment, computed routes cannot be rendered at correct physical locations.

## Decision

V1 uses a **single entrance marker** as the alignment anchor:

1. A physical marker with known dimensions is placed at a **measured position** in the building
2. The marker's building-local coordinates and orientation are stored in `entrance_markers.json`
3. At runtime, ARKit/ARCore detects the marker as a **tracked reference image**
4. The platform provides the marker's 6-DoF pose in AR world coordinates
5. We compute the alignment transform: `T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹`
6. All subsequent building-local points are transformed through this to get AR-world positions

###
```

## Status
Mapped (Pass 3 Normalization)
