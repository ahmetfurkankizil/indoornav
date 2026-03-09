# Checkpoint Marker Placement Guide

## When to Use Checkpoint Markers

Use checkpoint markers on **controlled demo routes > 20 meters** where VIO drift may become visible. They are **optional** — shorter routes or demo scenarios work fine with just the entrance marker.

## Physical Requirements

- **Print format**: Same as entrance markers (reference image + QR code)
- **Recommended size**: 15×15 cm or 20×20 cm (must match `physicalWidthMeters`/`physicalHeightMeters` in config)
- **Material**: Matte paper or card stock (avoid glossy — causes reflections)
- **Mounting**: Flat on wall at eye height (~1.5 m), perpendicular to walking direction

## Placement Guidelines

1. **Spacing**: Place checkpoints every 15–25 meters along the route
2. **Visibility**: Ensure markers are visible while walking naturally — users don't need to stop
3. **Lighting**: Adequate, consistent lighting near marker location
4. **Avoid**: 
   - Markers near glass/reflective surfaces
   - Markers in extremely bright or dark areas  
   - Markers at sharp turns (place before or after, not at the turn)
5. **Node association**: Each checkpoint's `nearestNodeId` should reference a node the user passes near

## Configuration Example

```json
{
  "checkpointMarkers": [
    {
      "id": "cp-hallway-1",
      "nearestNodeId": "n05",
      "physicalWidthMeters": 0.15,
      "physicalHeightMeters": 0.15,
      "position": { "x": 8.0, "y": 0.0, "z": 0.0 },
      "rotationYDegrees": 0,
      "referenceImageName": "checkpoint_hallway_1",
      "notes": "Placed on left wall of main hallway, 1.5m height"
    }
  ]
}
```

## Validation

After adding checkpoint markers to `authoring_config.json`:
1. Run the preprocessor: `make preprocess`
2. Check `plan_view_debug.svg` — checkpoint markers appear as **purple diamonds** (◆)
3. Verify all `nearestNodeId` references resolve correctly
4. Check the exported `checkpoint_markers.json` in the package output

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Marker not detected | Check image quality, size, lighting |
| Detection but no correction | Check confidence threshold, marker too far |
| Corrections too large | Verify marker `position` values in config |
| Corrections cause jump | Normal for large drift; bounds limit to 2m max |
