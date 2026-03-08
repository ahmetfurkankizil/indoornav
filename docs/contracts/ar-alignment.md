# AR Coordinate Alignment

This document describes how building-local coordinates map to AR world coordinates after entrance marker detection.

## Coordinate Systems

| System | Origin | Up | Unit | Handedness |
|--------|--------|----|------|------------|
| Building-local | Building origin (author-defined) | Y-up | Meters | Right-handed |
| ARKit world | Session start position | Y-up | Meters | Right-handed |
| ARCore world | Session start position | Y-up | Meters | Right-handed |

Both AR runtimes use Y-up, meters, right-handed — no axis swapping needed.

## Alignment Transform

When the entrance marker is detected, we compute a transform from building-local to AR-world:

```
T_ar_bldg = T_ar_marker × T_bldg_marker⁻¹
```

### Step-by-step

1. **Known from package** (`entrance_markers.json`):
   - Marker position in building: `(bX, bY, bZ)`
   - Marker Y-rotation in building: `bRot` degrees

2. **Detected at runtime** (ARKit/ARCore):
   - Marker pose in AR world: `(aX, aY, aZ)`
   - Marker Y-rotation in AR world: `aRot` degrees

3. **Compute transform**:
   ```
   rotationYDeg = aRot - bRot
   
   # Rotate building marker position
   rotBldgX = bX × cos(rot) + bZ × sin(rot)
   rotBldgZ = -bX × sin(rot) + bZ × cos(rot)
   
   # Translation offset
   offsetX = aX - rotBldgX
   offsetY = aY - bY
   offsetZ = aZ - rotBldgZ
   ```

4. **Apply to any building point P**:
   ```
   pArX = P.x × cos(rot) + P.z × sin(rot) + offsetX
   pArY = P.y + offsetY
   pArZ = -P.x × sin(rot) + P.z × cos(rot) + offsetZ
   ```

## Implementation

| Layer | File | Role |
|-------|------|------|
| Shared | `AlignmentTransform.kt` | Transform math + factory |
| iOS | `ARRouteRenderer.swift` | `transformToAR()` method |
| Android | `ArRouteRenderer.kt` | `transformToAR()` method |

## V1 Simplifications

- Single floor (Y ≈ 0 for all navigation nodes)
- Single marker (no multi-marker averaging)
- No drift correction after initial alignment
- Y-rotation only (no tilt/roll compensation)

## Why Arrows Appear Where They Appear

```
Building Config:           AR Scene:
                          
Node at (3, 0, 5)         Arrow at transformToAR(3, 0, 5)
  in building coords        = rotated + translated to AR world
                          
The transform ensures that the marker's known building
position maps exactly to its detected AR position.
All other points are transformed consistently.
```
