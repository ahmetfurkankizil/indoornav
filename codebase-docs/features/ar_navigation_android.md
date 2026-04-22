# Feature: AR Navigation

- **Feature Name**: AR Navigation
- **Purpose**: Provides the immersive AR experience where users see navigation arrows overlaid on the real world.
- **Implemented In**:
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArNavigationActivity.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArRouteRenderer.kt`
    - `apps/androidApp/src/main/kotlin/com/vecturai/android/ar/ArBridge.kt`
- **Used By**:
    - Main App UI (Launch trigger)
- **Main Flow**:
    1. **Initialization**: `ArNavigationActivity` starts and initializes the ARCore session via `ArSessionManager`.
    2. **Scanning**: User points camera at the world. `ArMarkerDetector` searches for known markers.
    3. **Alignment**: Marker detected -> `handleMarkerDetected` establishes the transformation between AR coordinates and building coordinates.
    4. **Guidance**: `ArRouteRenderer` displays 3D arrows at path coordinates.
    5. **Tracking**: `sampleCameraPose` updates progress as the user moves.
    6. **Arrival**: Proximity to destination triggers the arrival overlay.
- **Key Symbols**:
    - `ArNavigationActivity`
    - `ArBridge`
    - `ArRouteRenderer`
- **Config / Env / Flags**:
    - `isSimulated`: Flag to bypass physical marker detection for testing.
- **Data Structures / Protocols**:
    - `ArrowRenderData`: Position, orientation, and type for 3D arrows.
    - `MarkerDetectionEvent`: Payload from image detection.
- **Related Tests**:
    - [TBD]
- **Related File Dossiers**:
    - [ArNavigationActivity.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArNavigationActivity.kt.md)
    - [ArMarkerDetector.kt](../files/apps_androidApp_src_main_kotlin_com_vecturai_android_ar_ArMarkerDetector.kt.md)
- **Risks / Notes**:
    - Highly dependent on lighting and surface texture for ARCore stability.
    - Coordinate alignment is the most sensitive math section (Euler rotation + 3D translation).
