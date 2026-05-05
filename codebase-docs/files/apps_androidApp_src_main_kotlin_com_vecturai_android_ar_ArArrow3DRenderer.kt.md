# `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArArrow3DRenderer.kt`

## Overview
`ArArrow3DRenderer` is responsible for rendering 3D navigational arrows within the AR session using OpenGL ES. It replaces the previous 2D projected arrows by drawing volumetric shapes (chevrons) directly into the AR world space, allowing for true depth, lighting simulation, and more immersive route guidance. It utilizes geometries and colors defined in `Arrow3DGeometry`.

## Role
- **Rendering**: Uses OpenGL ES 2.0 to draw solid 3D arrows and drop shadows beneath them.
- **Lighting**: Implements a simple directional lighting model using normals to give depth to the arrows.
- **Animation**: Animates arrows along the vertical axis (floating effect) using elapsed frame time.

## Key Symbols
- `ArArrow3DRenderer`: Main class that handles shader compilation, buffer allocation, and drawing.
- `onSurfaceCreated()`: Initializes OpenGL programs, vertex/index buffers, and uniform/attribute locations.
- `draw()`: Takes the current snapshot of `ArRouteArrow` data and view/projection matrices, applying transformations, animation, and rendering calls for each arrow's shadow and body.

## Features
- **3D Navigation Vectors**: Renders volumetric arrows for route guidance.
- **Visual Polish**: Includes floating animations and drop shadows.

## Dependencies
- `Arrow3DGeometry`: Provides vertex data, indices, and color schemes.
- OpenGL ES APIs (`android.opengl.GLES20`, `android.opengl.Matrix`).
- Core AR models (`com.VecturAI.android.data.ArrowPlacementType`, `ArRouteArrow`).

## Used By
- `UnifiedArRenderer`: Initializes and invokes the `draw` method per frame.
