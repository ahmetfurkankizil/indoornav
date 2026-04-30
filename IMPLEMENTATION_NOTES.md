# Indoor AR Navigation MVP Implementation Notes

## Completed Phases

1. Project/dependency setup
   - Added Jetpack Navigation Compose, lifecycle ViewModel Compose, coroutines, kotlinx.serialization JSON, ARCore, and SceneView AR dependencies.
   - Added Kotlin serialization plugin.
   - Added camera/internet permissions, ARCore required feature, and ARCore manifest metadata.

2. Camera permission + AR scene rendering
   - Added a Compose camera permission gate.
   - Mapping and navigation screens host SceneView `ARScene` with ARCore session callbacks.
   - Anchor markers are rendered as AR procedural spheres.

3. ARCore session config with Cloud Anchors enabled
   - Added `ArSessionConfig.configureIndoorCloudSession`.
   - Enables Cloud Anchors, autofocus, HDR light estimation, latest camera image update mode, and depth when supported.

4. Cloud Anchor host/resolve round trip
   - Added coroutine wrappers in `CloudAnchorHelper` for `hostCloudAnchorAsync` and `resolveCloudAnchorAsync`.
   - Mapping hosts anchors.
   - Navigation resolves saved anchors and uses the first successful resolve for localization.

5. Mapping mode
   - Manual drop pin hosts a Cloud Anchor at the current camera pose.
   - Auto-drop runs every frame after the first anchor and triggers at about 4 meters while tracking is healthy.
   - Successful anchors become graph nodes.
   - Sequential anchors create bidirectional-traversable graph edges through the pathfinder.
   - Latest anchor can be tagged with a room label.
   - Markers render green for waypoints and red for labeled room anchors.
   - Save writes local JSON and exits to the mode picker.

6. Persistence
   - Added serializable graph data models.
   - Added `GraphRepository` for save, load, and list from internal storage under `filesDir/graphs`.

7. Navigation mode
   - Lists saved building graphs.
   - Loads selected graph.
   - Resolves anchors in parallel batches of up to 40.
   - Starts navigation when at least one anchor resolves.
   - Keeps resolving anchors on a loop for simple drift correction.
   - Provides a destination picker from labeled nodes.

8. Pathfinding
   - Added Dijkstra shortest path implementation.
   - Added unit tests for shortest route and disconnected graph behavior.

9. Arrow navigation
   - Added single floating camera-relative arrow pose math.
   - Navigation advances waypoints when the camera is within 1.2 meters.
   - Shows arrived state at the final waypoint.
   - `arrow.glb` placeholder is present. The app checks for a valid GLB magic header and falls back to a procedural arrow until a real binary asset replaces it.

10. Debug/tuning
   - Mapping overlay shows tracking, node count, edge count, hosting state, and mapping guidance.
   - Navigation overlay shows phase, tracking, resolved anchor count, current node, current waypoint, distance to next waypoint, status, and error messages.

11. User build feedback fixes
   - Added the manifest merger override for SceneView's ARCore metadata.
   - Updated mapping and navigation AR rendering to SceneView 4.0's Compose scene content DSL instead of the removed `rememberNodes`/`childNodes` API.
   - Removed the stale local ARCore minimum APK metadata so ARCore 1.53.0 supplies its current value.
   - Switched from deprecated `ARScene` alias to `ARSceneView` and fixed the nullable priority-queue warning in pathfinding.

## Remaining Gaps

- I did not run Gradle builds or tests because you asked me not to do builds.
- Cloud Anchor reliability cannot be validated without a real ARCore device, camera access, network access, and a configured ARCore Cloud Anchor API key/project.
- The checked-in `app/src/main/assets/arrow.glb` is a text placeholder. Replace it with a real binary GLB arrow model when available.
- The app uses API-key/same-day demo Cloud Anchor TTL (`ttlDays = 1`). For long-lived anchors, switch to the appropriate keyless/auth setup and raise TTL intentionally.

## Known Limitations

- Single device, single building, local JSON only.
- Each floor should be mapped as a separate building graph.
- Auto-drop starts after the first manual anchor so the mapper can choose a stable graph origin.
- Drift correction is intentionally simple: each successful resolve recomputes the graph-to-session transform from that anchor.
- The procedural fallback arrow is functional rather than polished.

## Run/Test Instructions

1. Open the project in Android Studio.
2. Make sure `JAVA_HOME` is configured for Android Studio/Gradle.
3. Sync Gradle.
4. Provide Cloud Anchor API-key authorization by creating a repo-root `.env` file:
   - `ARCORE_API_KEY=your_same_day_demo_key`
   - The file is ignored by git. You can also override it with `-PARCORE_API_KEY=...` or a real environment variable named `ARCORE_API_KEY`.
5. Build:
   - `.\gradlew.bat :app:assembleDebug`
6. Unit tests:
   - `.\gradlew.bat :app:testDebugUnitTest`
7. Device test:
   - Install on an ARCore-supported Android device.
   - Grant camera permission.
   - Accept the Cloud Anchor privacy disclosure.
   - Enter mapping mode, drop the first pin manually, walk slowly, let auto-drop add anchors, tag rooms, then save.
   - Force-stop/reopen the app, enter navigation mode, select the saved graph, look around until at least one anchor resolves, choose a tagged room, and follow the floating arrow.
