# File Dossier: marker-assets.md

## Path
`docs\contracts\marker-assets.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
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
│   Scan to
```

## Status
Mapped (Pass 3 Normalization)
