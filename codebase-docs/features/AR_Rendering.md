# Feature: AR Rendering

## Purpose
Visualizes navigation guidance and mapping markers in the user's physical environment using 3D graphics overlaid on the camera feed.

## Implemented In
- `app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`
- `app/src/main/java/com/example/vecturai/ui/ArAssetUtils.kt`

## Used By
- Mapping UI
- Navigation UI

## Main Flow
1. **Mapping:** Renders procedural spheres at hosted anchor locations to provide visual feedback of graph construction.
2. **Navigation:**
   - Calculates a "floating" pose in front of the camera using `ArrowRenderer`.
   - Rotates the arrow (yaw) to point toward the next waypoint.
   - Renders a 3D arrow model (`arrow.glb`) at that pose.
   - **Confidence Visualization:** Waypoints are colored based on resolution confidence (Amber for estimated, Cyan for resolved).
   - **Smoothing:** Virtual poses are lerped (`lerpPose`) to prevent jitter when the graph-to-session alignment updates.
   - Updates distances and visibility based on camera proximity.

## Key Symbols
- `ArrowRenderer.floatingArrowPose()`
- `NavigationViewModel.lerpPose()`
- `confidenceColor()`
- `Vec3`

## Config / Env / Flags
- `ARROW_FORWARD_OFFSET_M`: Distance in front of the user (1.5m).
- `DISPLAY_POSE_CATCHUP_HZ`: Speed of pose smoothing.

## Data Structures / Protocols
- `ArrowPose`: Position, rotation, and distance metadata for the UI.
- `SessionNodePose`: Combines a graph node with its current AR session pose and confidence.

## Related Tests
N/A

## Related File Dossiers
- [ArrowRenderer.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ar_ArrowRenderer.kt.md)
- [PoseUtils.kt](file:///c:/Users/emirh/Desktop/vecturDENEME/codebase-docs/files/app_src_main_java_com_example_vecturai_ar_PoseUtils.kt.md)

## Risks / Notes
- Visual alignment depends on accurate ARCore tracking.
- The arrow is horizontal-only; steep vertical changes might not be fully represented by the model's pitch.
