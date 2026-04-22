# File Dossier: ArNavigationActivity.kt

## Path
`apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArNavigationActivity.kt`

## Type
Authored Source (Android ComponentActivity)

## Role
Main AR navigation interface. Manages the lifecycle of the AR session, marker detection, route rendering, and user progress tracking on Android.

## Imports / Includes
- `androidx.activity.ComponentActivity`
- `com.vecturai.android.ar.ArSessionManager`
- `com.vecturai.android.ar.ArMarkerDetector`
- `com.vecturai.android.ar.ArRouteRenderer`
- `android.widget.*` (UI components)

## Exports / Public Surface
- `ArNavigationActivity`: The primary activity for AR navigation.

## Main Symbols
- `ArNavigationActivity`: Class managing AR session and UI.
- `handleMarkerDetected(event)`: Logic for aligning AR world with building coordinates.
- `sampleCameraPose()`: Periodic task to project camera position onto the navigation graph.
- `updateUI()`: Updates progress bar and labels.

## Important Logic by Line Range
- **60-202**: UI initialization and marker detector configuration.
- **228-237**: Demo simulation logic.
- **252-291**: Alignment math (AR-to-Building transformation).
- **309-349**: Progress tracking logic (Nearest-segment projection).
- **358-375**: Arrival detection and overlay trigger.

## Uses
- `ArSessionManager`: For AR session lifecycle.
- `ArMarkerDetector`: For detecting entrance markers.
- `ArRouteRenderer`: For placing 3D arrows in the AR scene.

## Used By
- `MainActivity.kt`: Starts this activity via intent.

## Config / Constants / Protocol Details
- Uses `marker-main` as the hardcoded target for initial alignment.
- Total distance for demo route is hardcoded as `13.0` meters.

## Related Tests
- None (Manual UI testing in AR).

## Notes / Risks
- **Coordinate Drift**: Relies on a single initial marker detection.
- **Hardcoded Route**: Uses a demo route (`demoArrows`) for visualization.
