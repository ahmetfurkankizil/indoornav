# File Dossier: README.md

## Path
`docs\contracts\README.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# Vectura AI Building Package — Contract Reference

## Coordinate Conventions

All spatial data in Vectura AI uses a single, consistent coordinate system:

| Property | Convention |
|----------|-----------|
| **Unit** | Meters (all coordinates, distances, dimensions) |
| **Handedness** | Right-handed |
| **Up axis** | Y-up (`+Y` points toward ceiling) |
| **Forward** | `-Z` (conventional for Y-up right-handed systems) |
| **Origin** | Building-local origin, chosen during authoring |
| **Floor plane** | `Y = 0` for single-floor MVP |
| **Rotation unit** | Degrees (clockwise when viewed from above) |

### Why Y-up?

Y-up is the default coordinate system for:
- glTF / .glb files (Polycam exports in Y-up)
- ARKit (uses Y-up by default)
- RealityKit (Y-up)
- Most 3D tooling (Blender defaults to Z-up but exports glTF as Y-up)

ARCore uses Y-up as well. Using Y-up avoids coordinate transforms between the .glb asset, the navigation graph, and both AR runtimes.

### Origin Placement

The building-l
```

## Status
Mapped (Pass 3 Normalization)
