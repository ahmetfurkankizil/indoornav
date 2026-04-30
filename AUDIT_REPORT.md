# Audit Verdict

PARTIAL: Core pieces exist and the main code findings have been fixed, but end-to-end demo readiness still requires a user-run build and real ARCore device validation.

# Executive Summary

The implementation now supports the planned MVP in source: mapping mode can open AR, host manually/automatically dropped Cloud Anchors, tag rooms, create sequential graph edges, save graph JSON, and later navigation can load that graph, resolve anchors, localize, pick a labeled destination, compute a bidirectional shortest path, show a floating AR arrow, advance waypoints, and arrive.

During follow-up implementation, the audit findings were addressed where source changes could reasonably fix them without a build: Cloud Anchor API-key diagnostics are visible, AR session failures are surfaced in UI, manual Drop Pin now requires AR tracking, host operations have a timeout, resolve failures remain visible, next/final distances are shown, and `arrow.glb` is now a valid binary GLB asset.

This remains PARTIAL only because the user explicitly asked not to build, and Cloud Anchor behavior cannot be proven without a real ARCore device, API auth, camera/network access, and target-building conditions.

# Phase-by-Phase Compliance

| Phase | Required Behavior | Status: PASS/PARTIAL/FAIL | Evidence | Notes |
|---|---|---:|---|---|
| Project setup | Kotlin Android app, Compose activity, min SDK 24+, ARCore, SceneView, serialization, coroutines, app builds | PARTIAL | `app/build.gradle.kts` uses Compose, serialization, minSdk 24, ARCore, SceneView, coroutines, JSON. `MainActivity.kt` is Compose-based. | Source setup is present. Build was not run by audit per user instruction. |
| Manifest | CAMERA, INTERNET, ARCore metadata, AR hardware feature, main activity | PASS | `app/src/main/AndroidManifest.xml` has CAMERA, INTERNET, `android.hardware.camera.ar`, `com.google.ar.core`, API key metadata, and exported `.MainActivity`. | ARCore min APK metadata is supplied by the ARCore dependency in the merged manifest. |
| AR session | Permission handled, AR camera scene renders, ARCore configured, CloudAnchorMode enabled, tracking state handled, lifecycle/background/resume not obviously broken | PARTIAL | `CameraPermissionGate`; `MappingScreen.kt` and `NavigationScreen.kt` use `ARSceneView`; `ArSessionConfig.kt` sets `CloudAnchorMode.ENABLED`; ViewModels consume frame tracking state; `onSessionFailed` now updates UI state. | Static implementation is correct enough for MVP. Actual camera rendering/lifecycle recovery was not device-verified. |
| Cloud Anchors | Modern async host/resolve, IDs stored, errors surfaced, disclosure, same-day demo support | PARTIAL | `CloudAnchorHelper.kt` uses `hostCloudAnchorAsync` and `resolveCloudAnchorAsync`, defensively ensures `Config.CloudAnchorMode.ENABLED`, and converts thrown ARCore exceptions into `Result.failure`; `MapNode.cloudAnchorId`; privacy dialog in `ModePickerScreen.kt`; TTL defaults to 1 day; API-key status is visible in the mode picker; Gradle reads `ARCORE_API_KEY` from Gradle property, environment variable, or repo-root `.env`. | Same-day demo path is implemented. Long TTL/keyless auth remains intentionally outside this MVP implementation. Runtime success still requires the key to be valid and enabled for ARCore API. |
| Mapping mode | Reachable mapping screen, camera remains open, manual drop, auto-drop, tracking check, nodes/edges/labels/markers, save JSON, runtime real graph creation | PASS | `MainActivity.kt` routes to `MappingRoute`; `MappingScreen.kt` overlays controls on `ARSceneView`; `MappingViewModel.kt` hosts anchors, requires tracking for manual drop, auto-drops at 4m, tags latest anchor, adds edges, publishes markers, saves graph. | This satisfies runtime real-building graph creation. |
| Persistence | Serializable graph model, stores building/nodes/edges/labels/anchor IDs/positions, JSON save/load/list, survives restart | PASS | `MapGraph.kt` serializable models; `GraphRepository.kt` saves/loads/lists JSON under internal `filesDir/graphs`. | Actual app restart was not run, but the persistence path is implemented. |
| Navigation mode | Reachable, saved graph loads, anchors resolve in parallel, localizes after one resolve, associated poses, destination picker, reasonable start node | PASS | `NavigationViewModel.kt` lists/loads graphs, resolves anchors in batches of 40, publishes resolved/estimated node poses, transitions to destination picking, selects closest current node, and recomputes poses from resolved anchors. | Cloud Anchor runtime success is unverified. |
| Pathfinding | Dijkstra or A*, bidirectional edges, ordered route, unreachable handled, current node selection reasonable | PASS | `Pathfinder.kt` runs Dijkstra and duplicates each edge in reverse; `PathfinderTest.kt` covers shortest route and disconnected graph. | Tests were inspected but not run. |
| AR arrow navigation | Floating camera-relative arrow, points to next waypoint, horizontal math, distance, waypoint advance, arrived state, tracking loss safe | PASS | `ArrowRenderer.kt` computes camera-forward arrow pose and yaw; `NavigationScreen.kt` renders `arrow.glb` or procedural fallback; `NavigationViewModel.kt` updates next/final distances, advances under 1.2m, handles tracking loss and arrived state. | Visual orientation still needs real-device verification. |
| Debug/demo usability | Visible status for tracking/localizing/resolved anchors, error messages, no silent critical failures, developer notes | PARTIAL | Mapping/navigation overlays show tracking/status/counts/errors; resolve attempts and last resolve error are visible; AR session failures are user-visible; `IMPLEMENTATION_NOTES.md` has run/test instructions. | Full diagnostic overlay from the plan is still not complete, and no live run was performed. |

# Critical Blockers

1. Conditional setup blocker: Cloud Anchor auth must be valid in Google Cloud. The project now has repo-root `.env` support and a local key file, but hosting/resolving still requires that key to be enabled for ARCore API and allowed for this Android app/signing certificate.

2. Certification blocker: the complete mapping -> save graph -> restart -> load graph -> resolve Cloud Anchors -> navigate -> arrive flow has not been verified on a real ARCore device. This cannot be proven from code alone.

No source-code architecture blocker remains that obviously prevents runtime graph creation or graph-based AR navigation.

# High-Priority Fixes

1. User must build with the `.env` ARCore API key and run on an ARCore-supported Android device.

2. User must perform a Cloud Anchor smoke test in the target building: map 2-3 anchors, force-stop, reopen, resolve, and confirm markers land correctly.

3. If the demo map must last longer than same-day testing, add keyless Cloud Anchor authorization and raise TTL deliberately.

4. Tune auto-drop distance, waypoint threshold, and relocalization interval after a real hallway walk-test.

# Medium/Low Issues

1. Re-resolving every graph anchor every 15 seconds can waste quota, battery, and network. A skip/backoff strategy for recently resolved anchors would be safer after MVP validation.

2. `GraphRepository.listBuildings()` displays sanitized filenames rather than the original `MapGraph.buildingName`, so names with spaces or symbols may appear changed in the picker.

3. Diagnostics are useful but not as complete as the plan's debug overlay. Missing items include feature point count, FPS, resolve latency, transform residual, and graph/session transform diagnostics.

4. The standalone Phase 2 host/resolve round-trip utility described in the plan is not present. The integrated mapping/navigation flow covers the same API path but is less direct for early Cloud Anchor risk validation.

# Missing vs Plan

1. No verified build result from this audit. The plan requires "app builds"; user explicitly prohibited builds.

2. No real-device verification of AR camera rendering, tracking, Cloud Anchor host/resolve, or arrow orientation.

3. No standalone Cloud Anchor round-trip test UI for host ID -> force quit -> paste/resolve.

4. Long-lived Cloud Anchors/keyless authorization are not implemented; current default is same-day demo TTL.

5. Full diagnostics overlay from the plan is incomplete.

# Risky/Incorrect Technical Choices

1. `ARCORE_API_KEY` still falls back to empty if no Gradle property, environment variable, or `.env` value exists. The app warns about this, but Cloud Anchor success still depends on a valid Google Cloud key restriction setup.

2. Continuous resolve loops reattempt all anchors in fixed batches. This is simple and useful for MVP drift correction, but it can hammer the API and should be tuned after real-device testing.

3. Waypoint advancement uses full 3D distance while arrow direction is horizontal. For a single-floor demo this is acceptable; noisy Y estimates could delay waypoint advancement.

4. SceneView yaw assumes the arrow model/procedural arrow forward axis is +Z and that `Rotation(y = yawDegrees)` matches the intended world yaw. The math is plausible, but only a device run can confirm it visually.

# Build/Run Verification

- Build command used: none. The user explicitly instructed not to build.
- Suggested build command for the user with `.env`: `.\gradlew.bat :app:assembleDebug`
- Optional override build command: `.\gradlew.bat :app:assembleDebug -PARCORE_API_KEY=your_same_day_demo_key`
- Suggested unit test command for the user: `.\gradlew.bat :app:testDebugUnitTest`
- Result: not run by this audit.
- Errors: none captured because no build/test command was run.
- Existing artifact note: `app/build/outputs/apk/debug/app-debug.apk` existed before this follow-up, but this audit did not create or validate it.
- Real-device run possible by audit: no.
- Could not manually verify: AR camera feed, runtime permissions on device, ARCore Services install/update flow, Cloud Anchor host success, Cloud Anchor resolve success after restart, saved graph survival after actual process death, physical arrow orientation, waypoint advancement in a real hallway, arrival behavior in a real building.

# End-to-End Demo Checklist

| Item | Status | Notes |
|---|---:|---|
| launch app | PARTIAL | Main activity and Compose nav exist; not run. |
| enter mapping mode | PASS | Mode picker routes to `MappingRoute`. |
| camera opens | PARTIAL | `ARSceneView` and camera permission gate exist; no device verification. |
| drop anchors | PARTIAL | Manual host path exists and now requires tracking; actual Cloud Anchor host not verified. |
| auto-drop anchors | PASS | Distance-based auto-drop at 4m checks `TrackingState.TRACKING` and `!isHosting`. |
| tag rooms | PASS | Tag dialog updates latest anchor label. |
| save graph | PASS | Repository writes serialized JSON to internal storage. |
| restart app | PARTIAL | Persistence code supports it; not run. |
| load graph | PASS | Navigation lists and loads saved graph names. |
| resolve anchor | PARTIAL | Modern async resolve path exists with diagnostics; API key/device/environment not verified. |
| select destination | PASS | Picker lists labeled nodes. |
| compute path | PASS | Dijkstra pathfinder returns ordered node list and handles unreachable routes. |
| follow AR arrow | PARTIAL | Floating arrow and valid GLB asset exist; physical orientation/rendering not verified. |
| advance waypoints | PASS | Advances under 1.2m threshold. |
| arrive | PASS | Final waypoint transitions to `Arrived`. |

# Recommended Fix Order

1. Build with an explicit ARCore API key and run `:app:testDebugUnitTest`.
2. Install on an ARCore-supported Android device and confirm the camera scene opens.
3. Run a small Cloud Anchor round trip in the target building.
4. Map a 5-room graph, tag rooms, save, force-stop, reopen, load, resolve, and navigate.
5. Tune auto-drop distance, waypoint threshold, and relocalization interval from walk-test results.
6. Add resolve backoff/skip logic if quota, battery, or latency becomes painful.
7. Add the fuller diagnostics overlay only if field testing shows you need deeper telemetry.

# Changes Made After Initial Audit

1. Added `CloudAnchorAuthStatus` and a mode-picker warning/success banner for ARCore API-key configuration.
2. Routed `ARSceneView.onSessionFailed` into mapping/navigation UI state instead of printing stack traces only.
3. Required `TrackingState.TRACKING` before manual Drop Pin hosting.
4. Added a Cloud Anchor host timeout for mapping.
5. Added navigation resolve attempt count and last resolve error display.
6. Added final-destination distance alongside next-waypoint distance.
7. Replaced the text placeholder `arrow.glb` with a valid binary GLB arrow asset.
8. Added `.env` support for `ARCORE_API_KEY`.
9. Added `HIGH_SAMPLING_RATE_SENSORS` permission.
10. Added a defensive Cloud Anchor config guard and delayed navigation resolves until the first AR camera frame to avoid `CloudAnchorsNotConfiguredException` during SceneView startup.

No build, unit test, or device run was performed.
