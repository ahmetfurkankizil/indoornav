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

1. **Initial alignment:** Marker detection accuracy (~1cm). Small; well-calibrated.
2. **VIO drift:** Grows over time/distance. ~1-2% on short routes (<50m).
3. **Route simplification:** Polyline is an approximation of actual walkable path.
4. **Projection ambiguity:** Near turns, the nearest point could jump between segments.

### Assumptions

- Route is short (< 50m total)
- User walks approximately along the expected path
- Single floor (Y ≈ 0 plane)
- Single marker initialization
- Monotonic guard masks small VIO drift backwards

### Future Upgrade Path

- Multi-marker correction: periodic re-alignment reduces drift
- Pose fusion: combine VIO + marker observations
- True indoor positioning: BLE beacons, WiFi fingerprinting
