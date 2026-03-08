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

### Simplification for V1

Since v1 is single-floor with Y=0, the alignment reduces to:
- **Translation offset**: AR marker position minus building marker position (XZ plane)
- **Rotation**: Y-axis rotation difference between AR and building frames
- **Y offset**: Fixed height offset (marker height above floor)

Both ARKit and ARCore use Y-up, meters — no axis swapping needed.

## Consequences

### Positive
- Simple, deterministic alignment from a single detection event
- No dependency on GPS, WiFi fingerprinting, or cloud anchors
- Works offline after initial package download
- Consistent across both platforms

### Negative
- Accuracy degrades with distance from marker (AR drift)
- Single alignment point — no correction for accumulated drift
- Requires physical marker installation

### Future Upgrades
- Multiple markers for drift correction
- Periodic re-detection for realignment
- Visual-inertial relocalization
