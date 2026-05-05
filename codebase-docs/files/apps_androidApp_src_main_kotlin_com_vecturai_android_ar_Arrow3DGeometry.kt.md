# `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/Arrow3DGeometry.kt`

## Overview
`Arrow3DGeometry` is an internal object that provides the static vertex data, indices, normals, and color schemes for the 3D chevron arrows rendered in the AR navigation view. By centralizing the geometry generation, it decouples the mesh definition from the rendering logic in `ArArrow3DRenderer`.

## Role
- **Mesh Generation**: Calculates vertices for a 3D prism shape (chevron) used as a navigational arrow.
- **Normal Calculation**: Computes face and side normals to enable directional lighting.
- **Color Definitions**: Defines gradients and color pairs for different types of navigational arrows (e.g., straight, turns, destination).

## Key Symbols
- `Arrow3DGeometry`: The singleton object holding static geometric arrays.
- `arrowVertices` / `arrowIndices`: The primary mesh data for the 3D chevron.
- `shadowVertices`: The mesh data for the circular drop shadow beneath the arrow.
- `colorsFor(type: ArrowPlacementType)`: Returns the top and bottom colors for a specific arrow action.
- `buildArrowMesh()`: Generates the 3D geometry of the arrow.

## Features
- **3D Navigation Vectors**: Defines the physical shape of the AR guidance arrows.

## Dependencies
- `ArrowPlacementType`: To map actions to colors.
- Kotlin Math functions.

## Used By
- `ArArrow3DRenderer`: Reads the static arrays and color methods during its OpenGL drawing routines.
