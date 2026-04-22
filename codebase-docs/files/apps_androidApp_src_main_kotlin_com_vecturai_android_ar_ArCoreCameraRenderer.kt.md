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
- `onSurfaceCreated(...)`: Initializes OpenGL state, creates the external OES texture, and binds it to the AR session.
- `onSurfaceChanged(...)`: Updates viewport and display geometry in the AR session.
- `onDrawFrame(...)`: Orchestrates the per-frame lifecycle: updates the AR session, handles camera recovery if `session.update()` fails, and triggers background rendering.

## Important Logic
- **Recovery Logic**: If `session.update()` fails (e.g. `CameraNotAvailableException`), it triggers a session rebuild via the ViewModel.
- **Frame Validation**: Tracks if camera frames are actually being received; if not, it attempts recovery or surfaces an error after a timeout.
- **Coordinate Transformation**: Uses `Frame.transformCoordinates2d` to ensure the camera feed is correctly oriented and scaled to the viewport.

## Uses
- `AndroidArNavigationViewModel`: Receives frames and handles session lifecycle.
- ARCore `Session` and `Frame`.

## Used By
- `ArNavigationScreen.kt`: Embedded in Compose via `AndroidView`.

## Related Tests
- None.

## Notes / Risks
- OpenGL operations are performed on the GL thread; any UI updates from the renderer must be dispatched to the main thread (via ViewModel).
- Added logic to detect "black screen" states where ARCore is running but not delivering frames.

