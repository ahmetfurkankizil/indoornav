# File Dossier: ar-alignment.md

## Path
`docs\contracts\ar-alignment.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# AR Alignment Contract — Extended for Live Movement

## Initial Alignment (unchanged)

```
T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹
```

After marker detection, building-local points transform to AR-world via:
```
P_ar = rotate(P_bldg, θ) + offset
```

## Live Movement Mapping (Phase 6)

### AR-World → Building-Local

To convert a live camera position back to building coordinates:
```
P_bldg = rotate(P_ar - offset, -θ)
```

This is implemented as `AlignmentTransform.inverseTransformPoint()`.

### Progress Estimation Pipeline

```
Native AR Runtime
  │ CameraPose (arX, arY, arZ) @ ~2Hz
  ▼
AlignmentTransform.inverseTransformPoint()
  │ (buildingX, buildingY, buildingZ)
  ▼
ProgressEstimator.update()
  │ Project onto route polyline
  │ Nearest segment + cumulative distance
  │ Monotonic guard
  ▼
ProgressUpdate {
  progressFraction,
  remainingDistanceMeters,
  nearestSegmentIndex,
  distanceFromRouteMeters,
  isLowConfidence
}
```

### Where Error Enters

1. **Initial alignment:** Mar
```

## Status
Mapped (Pass 3 Normalization)
