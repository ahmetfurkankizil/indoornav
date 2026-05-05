# Feature Documentation: AR Navigation (iOS)

## Overview
The AR Navigation feature on iOS provides a RealityKit-based immersive guidance experience. It uses ARKit for world tracking and image marker detection, and RealityKit for rendering 3D navigation cues. The system follows a strict "Reviewed Package" rule, ensuring navigation only occurs using authorized building data.

## Architecture
The iOS AR Navigation subsystem is composed of several key components:

- **ARNavigationView**: The SwiftUI container hosting the AR session.
- **ARNavigationViewModel**: Orchestrates the navigation logic, marker detection, and progress tracking.
- **ARSessionManager**: Manages the `ARSession` lifecycle and configuration (plane detection, image tracking).
- **ARMarkerDetector**: Processes `ARImageAnchor` events to establish the spatial relationship between the physical world and the building's coordinate system.
- **ARRouteRenderer**: Manages the 3D scene graph, rendering arrows with distance-based visibility.
- **BuildingPackageLoader**: Performs route calculation (Dijkstra) and arrow placement generation.

## Key Workflows

### 1. Marker Alignment
1. The app displays a "Point camera at entrance sign" instruction.
2. `ARSessionManager` starts an `ARWorldTrackingConfiguration` with the expected marker image.
3. When ARKit detects the image, `ARMarkerDetector` calculates the marker's 3D pose (Position + Y-Rotation).
4. `ARNavigationViewModel` uses this pose to establish an alignment transform (`offsetX, offsetY, offsetZ, rotationY`).
5. The system transitions from "Scanning" to "Navigating".

### 2. Route Rendering & Progress
1. `ARRouteRenderer` places 3D arrows at calculated positions based on the alignment transform.
2. As the user moves, `ARNavigationViewModel` calculates the user's distance along the route.
3. `ARRouteRenderer.updateVisibility` implements a rolling window (lookahead) to show only relevant arrows.
4. Arrows passed by the user fade out and eventually disappear.

### 3. Navigation Guidance (Phase 11)
1. The system continuously checks the distance to the next "Action" arrow (Turn Left, Turn Right, Destination).
2. Guidance text is updated (e.g., "Turn left in 5m").
3. Haptic feedback is triggered when a turn is imminent (~2m) or upon arrival.

## Implementation Details
- **Coordinate System**: The building package uses a local Cartesian system. Alignment maps this to the AR world space using the detected marker as the origin (0,0,0 with local rotation).
- **Pathfinding**: Uses Dijkstra's algorithm on the navigation graph provided in the building package.
- **Turn Detection**: Calculated during arrow generation using the cross product of sequential path segments.

## Constraints & Rules
- **Reviewed Package Only**: Navigation will not start without a valid `manifest.json` and associated data files in the app bundle.
- **Strict Marker Match**: Only the specific marker identified in the QR code scan will be accepted for alignment.
- **Simulator Support**: Includes a "Simulate Alignment" feature to allow UI/logic testing without a physical AR environment.

## Related Files
- [ARNavigationView.swift](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/apps_iosApp_iosApp_ar_ARNavigationView.swift.md)
- [ARSessionManager.swift](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/apps_iosApp_iosApp_ar_ARSessionManager.swift.md)
- [ARMarkerDetector.swift](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/apps_iosApp_iosApp_ar_ARMarkerDetector.swift.md)
- [ARRouteRenderer.swift](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/apps_iosApp_iosApp_ar_ARRouteRenderer.swift.md)
- [BuildingPackageLoader.swift](file:///c:/Users/emirh/Desktop/bitirme/VecturAI/codebase-docs/files/apps_iosApp_iosApp_ar_BuildingPackageLoader.swift.md)
