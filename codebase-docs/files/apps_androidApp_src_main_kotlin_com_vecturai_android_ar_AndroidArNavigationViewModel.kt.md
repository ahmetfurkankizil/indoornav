# File Dossier: AndroidArNavigationViewModel.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/AndroidArNavigationViewModel.kt`

## Type
Authored Source (Android AR Navigation State)

## Role
Android ViewModel that ports the iOS AR navigation state and guidance math into the Compose/ARCore implementation. It owns route configuration, entrance-poster alignment, progress projection, next-action guidance, tracking labels, haptic events, and arrival state.

## Imports / Includes
- `androidx.lifecycle.ViewModel`, `viewModelScope`
- `com.google.ar.core.Frame`, `Session`, `TrackingState`, `TrackingFailureReason`
- `com.vecturai.android.data.AndroidReviewedPackageLoader`
- `com.vecturai.android.data.ArrowPlacementType`
- Kotlin coroutines `StateFlow`, `MutableStateFlow`, `delay`
- `kotlin.math` helpers

## Exports / Public Surface
- `ArNavigationUiState`: Compose-facing AR screen state.
- `NavigationActionIcon`: semantic next-action icon enum.
- `TrackingStatusIcon`: user-facing tracking badge icon enum.
- `AndroidArNavigationViewModel`: AR navigation coordinator.

## Main Symbols
- `configure(...)`: Initializes route data, resets detector, and prepares the session manager.
- `startSession(activity)`: Orchestrates the ARCore session startup, including marker configuration and error handling.
- `rebuildSession(activity)`: Implements an exponential backoff retry strategy for recovering from camera session failures.
- `onFrame(frame, width, height)`: Per-frame processing for marker detection, tracking status updates, and camera-to-graph projection.
- `computeNextAction(distance)`: Derives user-friendly guidance ("Turn left ahead", etc.) from the navigation graph.

## Important Logic
- **Session Rebuilding**: pro-actively detects session failures (like camera access loss) and attempts up to 3 retries with increasing delay to restore AR tracking.
- **Enhanced Guidance**: Implements sophisticated "next action" logic that calculates distances to upcoming turns and selects appropriate icons/text.
- **Haptic Feedback Coordination**: Triggers haptic events (route start, turn imminent, re-centering, arrival) via `AndroidHapticManager`.
- **Coordinate Mapping**: Handles the transformation between ARCore world space and the building's local coordinate system using the alignment anchor.

## Uses
- `ArSessionManager`: Low-level ARCore lifecycle.
- `ArMarkerDetector`: Entrance marker detection.
- `ArRouteRenderer`: 3D/2D projection of path markers.
- `AndroidHapticManager`: Device-side feedback.

## Used By
- `ArNavigationScreen.kt`: Observes `uiState` for rendering.
- `ArCoreCameraRenderer.kt`: Forwards GL-thread events and frames.

## Notes / Risks
- The `isSimulated` flag allows for testing navigation UI without a physical marker, which is critical for development in non-AR-capable environments.
- Coordinate transformations assume a right-handed system and must be carefully validated against the preprocessor's output.

