# File Dossier: UnifiedArRenderer.kt

## Path
`apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt`

## Type
Authored Source (ARCore Camera Rendering)

## Role
`GLSurfaceView.Renderer` that creates the external OES camera texture, draws the ARCore camera background, renders 3D navigation arrows, and forwards frames to the Activity-scoped flow.

## Imports / Includes
- Android OpenGL: `GLES11Ext`, `GLES20`, `GLSurfaceView`
- ARCore `Coordinates2d`, `Frame`, `Session`
- Java NIO buffers
- EGL/GL interfaces

## Exports / Public Surface
- `UnifiedArRenderer`

## Main Symbols
- `onSurfaceCreated(...)`: Initializes GL state, creates camera texture, compiles shaders, and reports the texture id to `ArCameraActivity`.
- `onSurfaceChanged(...)`: Updates viewport and ARCore display geometry.
- `onDrawFrame(...)`: Calls `Session.update()`, draws the camera background, draws 3D arrows via `ArArrow3DRenderer`, and forwards the frame with viewport and rotation metadata.

## Important Logic
- This renderer has no camera recovery or session rebuild logic.
- Any `Session.update()` or display-geometry exception is routed to the Activity fatal handler.
- The Activity/ViewModel decides whether a frame is used for QR scanning or AR navigation.

## Uses
- `UnifiedArSession`
- ARCore `Session` and `Frame`
- `ArArrow3DRenderer`
- `ArRouteRenderer`

## Used By
- `ArCameraActivity.kt`

## Related Tests
- None.
