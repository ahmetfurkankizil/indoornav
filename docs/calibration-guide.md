# Calibration Guide — Reviewed Package to Physical House

How to calibrate the reviewed navigation package coordinates to match a real physical space.

## Overview

The reviewed package contains node positions in a **building-local coordinate system**. The origin is the entrance marker, with:
- **+X** = right (when facing into the building)
- **+Y** = up
- **+Z** = behind you (away from the building interior)
- **-Z** = forward (into the building)

Calibration means measuring the real-world distances and updating the JSON files so arrows appear in the right place and arrival triggers at the right door.

## What to Calibrate

### 1. Entrance Marker Position (`entrance_markers.json`)

| Field | What to set |
|---|---|
| `position.x` | Always `0.0` (marker is the origin) |
| `position.y` | Height of marker center from floor (typically 1.2m) |
| `position.z` | Always `0.0` (marker is the origin) |
| `rotationYDegrees` | Yaw offset if marker doesn't face -Z. Usually `0.0`. |
| `physicalWidthMeters` | Actual printed width of the marker image (e.g. 0.21 for 21cm) |
| `physicalHeightMeters` | Same as width for a square marker |

### 2. Corridor/Junction Nodes (`nav_graph.json`)

Measure from the entrance marker to each corridor junction:
- Walk along the main path, counting paces or using a tape measure.
- Set `x` to the lateral offset (right = positive).
- Set `z` to the depth into the building (forward = negative).
- Keep `y` at `0.0` (single floor).

Example: if the first junction is 2.5m straight ahead from the entrance:
```json
{"id": "corridor_1", "x": 0.0, "y": 0.0, "z": -2.5, ...}
```

### 3. Door Nodes (`nav_graph.json`)

For each room door, measure its position relative to the nearest corridor junction:
- Mutfak door: offset from corridor_1
- Salon door: offset from corridor_2
- Yatak Odası door: offset from corridor_3

The door node is where the user should be standing when they "arrive" — typically 1m in front of the actual doorframe.

### 4. Edge Costs (`nav_graph.json`)

Set `cost` to the walking distance in meters between connected nodes. Measure or estimate by walking the path.

### 5. Route Rendering (`route_rendering.json`)

| Field | Typical value | Adjust if... |
|---|---|---|
| `destinationThresholdMeters` | `1.5` | Arrival triggers too early/late |
| `arrowSpacingMeters` | `1.5` | Arrows feel too dense/sparse |
| `lookaheadDistanceMeters` | `8.0` | Too many/few arrows visible ahead |
| `arrowHeightOffsetMeters` | `0.05` | Arrows clip the floor or float too high |

## Calibration Procedure

1. **Print and place the entrance marker.** Tape it to the entrance wall at the measured height.
2. **Stand at the marker.** This is your origin (0, y, 0).
3. **Walk to each corridor junction.** Measure lateral (x) and forward (-z) distances.
4. **Walk to each door.** Measure from the nearest junction node.
5. **Update `nav_graph.json`** with the measured coordinates.
6. **Update edge costs** with the measured walking distances.
7. **Run the validator**: `./scripts/validate-reviewed-package.sh sample/reviewed-house-package/`
8. **Copy to iOS bundle**: `cp sample/reviewed-house-package/*.json apps/iosApp/iosApp/reviewed-package/`
9. **Build and test on device.**
10. **Iterate**: if arrows feel offset, re-measure and adjust. Focus on the corridor centerline and door positions.

## Tips

- **Use a tape measure** for the first calibration. Pacing works for rough checks.
- **Forward is -Z.** Most buildings have the corridor going forward from the entrance, so corridor Z values should be negative.
- **Rooms branch left/right.** Left = negative X, right = positive X.
- **Arrival threshold** of 1.5m works well for residential doors. Increase to 2.0m if the user naturally stops further back.
- **Don't aim for centimeter precision.** 10-20cm accuracy is sufficient for believable guidance. The arrows are intentionally floating above the floor to tolerate alignment drift.
- **Test all three destinations** after calibration. Walk the full route for each.

## Current Demo House Coordinates

The current `nav_graph.json` uses a straight corridor along +X with rooms branching on ±Z. If the physical house has a different layout, adjust the coordinate axes accordingly.

See [manual-package-review-workflow.md](manual-package-review-workflow.md) for the full package editing workflow.
