# Marker Assets Workflow

This document defines the controlled entrance marker: artwork, physical requirements, metadata structure, and platform integration.

## Marker Requirements

| Property | Requirement |
|----------|------------|
| Artwork | High-contrast, asymmetric pattern (NOT symmetric) |
| Minimum physical size | 15×15 cm |
| Recommended size | 21×21 cm |
| Material | Matte print (avoid gloss/reflections) |
| Placement | Flat surface, perpendicular to floor |
| Height | Eye-level preferred (1.2–1.5m above floor) |
| Lighting | Evenly lit, avoid direct glare |

## Marker Artwork Structure

```
┌────────────────────────┐
│   ┌──────────────┐     │
│   │  QR Code     │     │
│   │  (VecturAI   │     │
│   │   ://b1/m1)  │     │
│   └──────────────┘     │
│                        │
│   ┌──────────────┐     │
│   │  Visual AR   │     │
│   │  Reference   │     │
│   │  Pattern     │     │
│   └──────────────┘     │
│                        │
│   VecturAI             │
│   Scan to Navigate     │
└────────────────────────┘
```

The marker serves dual purpose:
1. **QR code**: Carries `VecturAI://{buildingId}/{markerId}` payload for building identification
2. **Visual pattern**: High-contrast image used as ARKit/ARCore reference image for pose detection

## Metadata Mapping

The marker's properties map to `entrance_markers.json` fields:

| Marker property | JSON field |
|----------------|-----------|
| Marker ID | `id` |
| QR payload | `qrPayload` (auto-generated: `VecturAI://{buildingId}/{markerId}`) |
| Position in building | `positionX`, `positionY`, `positionZ` |
| Y-rotation | `rotationYDegrees` |
| Forward direction | `forwardBasis` (e.g., `-z`) |
| Nearest graph node | `nearestNodeId` |
| Physical width | `physicalWidthMeters` |
| Physical height | `physicalHeightMeters` |
| Reference image name | `referenceImageName` |

## Asset Storage

```
sample/demo-building/
├── authoring_config.json     # Marker metadata authored here
├── scan.glb
└── markers/
    └── entrance_marker_main.png  # Reference image for AR detection

assets/markers/                    # App-bundled marker images
└── entrance_marker_main.png
```

## Platform Integration

### iOS (ARKit)

1. Add marker image to Xcode Asset Catalog → "AR Resources" group
2. Set physical width in the asset catalog (0.21m)
3. ARKit loads reference images via `ARReferenceImage.referenceImages(inGroupNamed:)`
4. On detection, `ARImageAnchor` provides 6-DoF pose

### Android (ARCore)

1. Add marker image to `res/drawable/` or load from assets
2. Create `AugmentedImageDatabase` and add image with physical width
3. ARCore detects the image and provides `AugmentedImage` with `centerPose`

## Authoring Checklist

1. Print marker at 21×21 cm on matte paper
2. Mount at measured position in building (record X, Y, Z in meters)
3. Record orientation (which building axis the marker faces)
4. Add entry to `authoring_config.json` → `entranceMarkers[]`
5. Save reference image PNG in `markers/` directory
6. Add to platform AR resource catalogs
7. Run preprocessor → verify marker appears in debug SVG
