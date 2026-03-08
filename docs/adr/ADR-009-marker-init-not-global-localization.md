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
- For a 50m route: ~0.5-1m drift — arrows still point in correct direction
- The primary UX is textual turn-by-turn; arrows are supplementary
- Users follow the route sequentially, so absolute position accuracy matters less than relative direction accuracy

## Consequences

### Positive
- Zero infrastructure beyond a printed marker
- Works offline
- No cloud service dependency
- No WiFi/BLE beacon installation
- Predictable behavior

### Negative
- Accuracy degrades on very long routes
- No recovery if user wanders far off-path
- Single entry point (must start from marker)

### Future Upgrades
- Multiple markers along route for periodic re-alignment
- Visual relocalization using stored keyframes
- BLE beacon proximity for coarse position correction
