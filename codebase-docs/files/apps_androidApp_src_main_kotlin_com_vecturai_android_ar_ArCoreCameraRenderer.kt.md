# File Dossier: ArCoreCameraRenderer.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArCoreCameraRenderer.kt`

## Type
Authored Source (ARCore Camera Rendering)

## Role
GLSurfaceView renderer that draws the ARCore camera feed as a full-screen external OES texture and forwards each AR frame to `AndroidArNavigationViewModel`.

## Imports / Includes
- Android OpenGL: `GLES11Ext`, `GLES20`, `GLSurfaceView`
- ARCore `Coordinates2d`
- Java NIO buffers
- EGL/GL interfaces

## Exports / Public Surface
- `ArCoreCameraRenderer`: `GLSurfaceView.Renderer` implementation.

## Main Symbols
- `onSurfaceCreated(...)`: Creates external texture/shader program, starts AR session, and binds ARCore camera texture.
- `onSurfaceChanged(...)`: Updates viewport dimensions.
- `onDrawFrame(...)`: Calls `session.update()`, renders the camera background, and passes the frame to the ViewModel.

## Important Logic
- Uses `Frame.transformCoordinates2d` to map OpenGL NDC quad coordinates to ARCore texture coordinates.
- Draws camera feed with a small vertex/fragment shader pair using `samplerExternalOES`.
- Defers route arrow rendering to the Compose overlay via the ViewModel.

## Uses
- `AndroidArNavigationViewModel`
- ARCore `Session` and `Frame`

## Used By
- `ArNavigationScreen.kt`: Embedded in Compose via `AndroidView` / `GLSurfaceView`.

## Related Tests
- None.

## Notes / Risks
- Shader compile/link status is not surfaced; visual validation on device is still required.
