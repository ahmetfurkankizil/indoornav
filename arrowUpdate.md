# Arrow Update Implementation Plan

## Goal

Replace the current single floating checkpoint arrow with a blue, floor-anchored route guidance system like the reference image:

- First update the arrow visual style to the blue floor-arrow look.
- Then render a short visible path made of repeated arrows on the floor.
- Drive arrow direction from the route path, not from a single checkpoint target.
- Keep vertical movement special-cased for stairs and elevators.
- Fix the technical causes of jitter, longer-than-expected path visuals, and weird 3D arrow placement.

Do not update `codebase-docs/` as part of this work.

## Source-Verified Refinements Added By Review

The plan direction is correct, but implementation should account for these source-verified details:

- SceneView is `4.0.1`; `ModelLoader.createInstancedModel("arrow.glb", count)` and `PathNode` are available. Use them instead of sharing one `ModelInstance` across many `ModelNode`s or creating/destroying models per frame.
- `ModelNode` is backed by a single model-instance root entity, so each visible floor arrow needs its own preallocated `ModelInstance`.
- `MapGraph` uses `EdgeKind.CORRIDOR`, `EdgeKind.STAIRS`, and `EdgeKind.ELEVATOR`; route sampling must filter by `EdgeKind`, not only by floor number.
- `ArSessionConfig.configureIndoorCloudSession(...)` does not explicitly configure plane finding today. If floor-plane refinement is implemented, make horizontal plane finding explicit while keeping the plane renderer hidden.
- `NavigationDiagnostics` reads `distanceToNextMeters`, `distanceToDestinationMeters`, and `statusMessage`; replacing the floating arrow should also update these user-facing strings and distances.
- Keep the implementation pure-testable by extracting route sampling into a non-Compose, non-SceneView helper before wiring it into `NavigationViewModel`.

## Current Implementation Facts To Preserve

Verified source files:

- `app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt`
- `app/src/main/java/com/example/vecturai/graph/MapGraph.kt`
- `app/src/main/java/com/example/vecturai/graph/Pathfinder.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`
- `app/src/main/java/com/example/vecturai/ui/mapping/MappingViewModel.kt`

Relevant current behavior:

- `NavigationViewModel` exposes one `arrowPose: ArrowPose?`.
- `ArrowRenderer.floatingArrowPose(cameraPose, target)` places one arrow in front of the camera and aims it toward the current waypoint.
- `NavigationScreen` renders one `PoseNode` for `"nav-arrow"`.
- Existing mapping saves graph nodes from camera/cloud-anchor poses, not from a detected floor point.
- `projectToPath(...)` already projects the user onto current-floor route segments and is the right foundation for route sampling.
- `Pathfinder.smoothPath(...)` already checks direct edges before dropping an intermediate node, which must remain true.
- `NavigationScreen` currently loads one `arrow.glb` instance and one fallback material for one arrow node.
- SceneView `PathNode` is available for optional route-line rendering.
- `arrow.glb` is a tiny binary asset today and should be replaced with a similarly simple, optimized single-material model, not a heavy imported scene.

Direct dependents to check before implementation:

- `ArrowPose` is used by `NavigationScreen` and `NavigationViewModel`.
- `ArrowRenderer.floatingArrowPose(...)` is called from `NavigationViewModel.updatedNavigationProgressState(...)`.
- `Pathfinder.shortestPath(...)` is used by destination selection, rerouting, and `PathfinderTest`.
- `NavigationDiagnostics` displays the navigation status and distances that will change wording from floating-arrow guidance to floor-path guidance.
- `ArSessionConfig.configureIndoorCloudSession(...)` is a direct dependency if floor-plane detection is enabled.

## Phase 1 - Update Arrow Look First

Objective: make the arrow look like the reference image before changing navigation behavior.

Implementation steps:

1. Replace or update `app/src/main/assets/arrow.glb` so it is a low-profile floor arrow:
   - Blue/cyan body, close to `#29B6F6` or `#03A9F4`.
   - Unlit or very low lighting dependence so it stays readable indoors.
   - Flat geometry with slight thickness, not a tall floating pointer.
   - Forward direction authored along local `+Z`, matching current `ModelNode` assumptions.
   - Origin centered on the arrow centerline so repeated instances align cleanly.
   - Single mesh and single material where possible; no animation, cameras, lights, or unused nodes.
   - Baked transforms and meter-like dimensions so runtime scale can stay simple.
   - Suggested real-world size before scale: about `0.6m` to `0.8m` long, `0.35m` to `0.5m` wide, `0.02m` to `0.05m` thick.

2. Update `NavigationScreen.kt` fallback arrow material:
   - Change fallback material from orange `Color(0xFFFF5722)` to the same blue.
   - Replace the fallback cube+sphere shape with a closer procedural floor-arrow fallback if the GLB fails:
     - Low-profile shaft cube.
     - Low-profile head made from a cone/triangular mesh if available, or two/three small cubes if not.
     - Same local `+Z` forward direction and same center offset as the GLB.
   - Keep fallback low-profile and floor-oriented.

3. Update arrow constants after the asset change:
   - Re-measure `ARROW_MODEL_SCALE`.
   - Re-measure `ARROW_HALF_LENGTH_LOCAL_M`.
   - Add a named render-pool constant such as `FLOOR_ARROW_RENDER_POOL_SIZE = FLOOR_ARROW_COUNT`.
   - Keep the asset centered so path samples do not visually drift.

4. Validate the asset:
   - `hasValidGlbAsset(...)` must still pass.
   - `assembleDebug` must still package the asset.
   - On-device visual check should show a blue arrow, not orange.

Acceptance criteria:

- The existing single arrow appears blue and low-profile.
- If the GLB fails to load, the fallback is also blue and low-profile.
- The asset is still loaded once and disposed through the existing model lifecycle.

## Phase 2 - Introduce Floor Route Visual Data

Objective: expose a route visual model from the ViewModel instead of only one floating `ArrowPose`.

Add new data types in the navigation layer:

- `FloorArrowPose`
  - `sampleIndex: Int`
  - `position: Vec3`
  - `yawDegrees: Float`
  - `alpha: Float`
  - `scale: Float`
  - `distanceAheadMeters: Float`
  - `segmentIndex: Int`

- `FloorPathSegment`
  - `from: Vec3`
  - `to: Vec3`
  - `alpha: Float`
  - `segmentIndex: Int`

- `RouteVisualState`
  - `arrows: List<FloorArrowPose>`
  - `segments: List<FloorPathSegment>`
  - `transitionCue: RouteTransitionCue?`

- `RouteTransitionCue`
  - `kind: EdgeKind`
  - `fromFloor: Int`
  - `toFloor: Int`
  - `position: Vec3?`

Suggested file split:

- `RouteVisualModels.kt` for the UI-state data classes.
- `RouteVisualSampler.kt` for pure route sampling math.
- Keep `NavigationViewModel.kt` responsible for converting current graph/session state into sampler input, not for owning all sampling details inline.

Update `NavigationUiState`:

- Add `routeVisualState: RouteVisualState = RouteVisualState.Empty`.
- Keep `arrowPose` temporarily during migration, then remove or stop using it once floor arrows are active.
- Clear `routeVisualState` anywhere `arrowPose` is currently cleared: building changes, session failure, tracking loss, destination picking, arrival, and no-route states.

Important rule:

- Route visuals should be computed in `NavigationViewModel`, but rendered in `NavigationScreen`.
- Rendering code should not recalculate routing or projection.
- Unit-test route visual math without ARCore `Pose`, Compose, or SceneView dependencies.

Acceptance criteria:

- UI state can carry multiple floor arrows and path segments.
- Existing navigation still works while the renderer is migrated.

## Phase 3 - Floor Height Strategy

Objective: arrows must sit on the floor, not at camera/cloud-anchor height.

Problem to solve:

- `MappingViewModel` creates nodes from `latestCameraPose` and hosted cloud anchor poses.
- Therefore route node `y` values are camera/anchor height, not guaranteed floor height.
- Simply rendering arrows at route node `y` would make them hover.

Implement a `FloorHeightEstimator` approach:

1. Baseline deterministic fallback:
   - For each current-floor route sample, compute route camera-height `y` from the transformed graph path.
   - Subtract `DEFAULT_CAMERA_TO_FLOOR_M`, initially around `1.35m`.
   - Add `FLOOR_ARROW_Y_OFFSET_M`, around `0.015m` to `0.03m`, to prevent z-fighting.

2. ARCore floor refinement:
   - Update `ArSessionConfig.configureIndoorCloudSession(...)` to explicitly enable horizontal plane finding if the current ARCore defaults are not sufficient:
     - `config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL`
     - Keep `planeRenderer = false` in `NavigationScreen`; detection does not require visual plane rendering.
   - Use available plane/depth information from `Frame` when possible.
   - Thread `Frame` or a computed floor estimate through the existing `onSessionUpdated(session, frame)` flow before `updatedNavigationProgressState(...)` runs.
   - Prefer tracked `Plane.Type.HORIZONTAL_UPWARD_FACING` planes below the camera and near the current route/camera position.
   - Smooth detected floor `y` over time.
   - Keep one runtime floor height estimate per logical floor.
   - Prefer a stable current-floor estimate over noisy per-arrow estimates.

3. Safety rules:
   - Never place arrows above waist height.
   - Clamp floor estimates to a plausible range below camera height, for example `0.6m..2.2m` below the camera before applying the arrow offset.
   - Clamp sudden floor-y changes unless the user is on a vertical transition edge.
   - If no floor estimate is available, hide floor path briefly or use the deterministic fallback.
   - Reset or re-key floor estimates when the building/graph changes.

Acceptance criteria:

- Arrows are visually on the floor.
- Arrows do not jump vertically every frame.
- Floor placement remains usable before ARCore plane detection is fully stable.

## Phase 4 - Route Projection And Lookahead Sampling

Objective: sample arrows along the path ahead of the user instead of targeting the next checkpoint directly.

Use `projectToPath(state, cameraPose)` as the starting point.

Add route-distance helpers:

- Convert the active path into session-space route segments.
- Filter to segments on the user's inferred floor.
- Exclude non-corridor edges from normal floor rendering by checking `MapGraph.edgeBetween(...).kind != EdgeKind.CORRIDOR`, even when both nodes have the same floor number.
- Compute cumulative distance along the route.
- Find the user's current projected route distance.
- Sample arrows at fixed distances ahead of the projection.
- Stop sampling at the first vertical/non-corridor transition and expose that transition through `RouteTransitionCue`.
- Reuse the same cumulative-distance model for route visuals, `distanceToNextMeters`, and `distanceToDestinationMeters` so diagnostics match what the user sees.

Suggested pure sampler input:

- `RouteSampleNode(id, position, floor)`
- `RouteSampleEdge(fromId, toId, kind)`
- `RouteProjection(segmentIndex, segmentT, cumulativeMeters, perpDist)`
- `FloorHeightEstimate(floor, yMeters, confidence)`

The ViewModel can build these from `state.path`, `graph.edgeBetween(...)`, `guidancePose(...)`, `inferUserFloor(...)`, and the floor estimator.

Suggested sampling:

- Start distance: `0.8m` ahead of the projected user point.
- Spacing: `0.9m` to `1.2m`.
- Count: `5` to `8` visible arrows.
- Max render distance: `8m` to `10m`.
- Stop at destination or before a stairs/elevator transition.

For each sample:

- Position is interpolated on the route polyline.
- `y` comes from the floor-height strategy, not from raw route node height.
- Yaw is the tangent of the route at that sample.
- Pitch is always `0`.
- Alpha fades with distance.
- Scale is larger/brighter near the user, smaller/subtler farther away.
- `sampleIndex` is stable from `0 until FLOOR_ARROW_COUNT`; do not derive identity from list order after filtering.

Acceptance criteria:

- The arrows form a continuous path like the reference image.
- Arrows follow turns by sampling the polyline, not by pointing through walls or across corners.
- The nearest arrow does not jump from checkpoint to checkpoint.

## Phase 5 - Renderer Migration

Objective: render multiple stable floor arrows and optional route strips in `NavigationScreen.kt`.

Implementation steps:

1. Extend `NavigationArScene(...)` parameters:
   - Replace or supplement `arrowPose` with `routeVisualState`.
   - Keep `nodePoses` unchanged.

2. Render floor arrows:
   - Use stable keys such as `"floor-arrow-${segmentIndex}-${sampleIndex}"`.
   - Use `PoseNode` per arrow with yaw-only rotation.
   - Preallocate model instances with SceneView 4.0.1:
     - `modelLoader.createInstancedModel("arrow.glb", FLOOR_ARROW_RENDER_POOL_SIZE)`
     - Bind one `ModelInstance` to one visible `ModelNode`.
     - Never share the same `ModelInstance` between two floor-arrow nodes.
     - Destroy the shared model asset once when the pool is disposed, not once per sample.
   - Use the blue `ModelNode` asset for each arrow sample.
   - Set `isSmoothTransformEnabled = false` for individual samples if ViewModel already smooths data.
   - Avoid creating/destroying materials per frame.
   - Keep fallback geometry pooled/stable as well; only visibility/pose/scale should change frame-to-frame.

3. Render a subtle path line if SceneView supports it cleanly:
   - SceneView 4.0.1 includes `io.github.sceneview.node.PathNode`; use it for the first implementation if a line is wanted.
   - Create one `PathNode` with one material and update its points instead of creating one node per segment.
   - Keep the path line disabled by default or very subtle if it makes the reference-like arrow path visually cluttered.
   - Use low-profile rectangular segment meshes/cubes only if `PathNode` line width is not readable enough on device.
   - Keep line thinner and lower alpha than arrows.

4. Control visual density:
   - Hide far arrows when tracking quality is poor.
   - Do not render path visuals when `phase != Navigating`.
   - Hide floor arrows during explicit vertical transitions and show the transition cue instead.

Acceptance criteria:

- The scene renders multiple blue floor arrows with stable identity.
- No per-frame GLB loads.
- No material leaks.
- No visible orange fallback remains.

## Phase 6 - Turn And Transition Behavior

Objective: make corners, stairs, and elevators understandable.

Turn behavior:

- Keep arrows along the route polyline through the corner.
- Make the arrow just before a turn slightly stronger.
- Determine left/right turns from consecutive corridor segment tangents with wrap-aware yaw delta.
- Optionally add a short status message such as "Turn left" or "Turn right" only when close to the turn.

Transition behavior:

- For `EdgeKind.STAIRS` and `EdgeKind.ELEVATOR`, stop normal floor arrows at the transition point.
- Show a separate transition cue:
  - "Take the stairs to floor X"
  - "Take the elevator to floor X"
- Resume floor arrows after the user's inferred floor matches the target floor.

Acceptance criteria:

- Arrows do not run up stairs or through elevator shafts as normal floor arrows.
- Floor changes feel intentional instead of visually broken.

## Phase 7 - Navigation Logic Improvements

Objective: make route guidance match the new floor-path UX.

Updates:

1. Replace checkpoint-target arrow logic:
   - Stop calling `ArrowRenderer.floatingArrowPose(...)` for normal navigation.
   - Compute route visuals from projected route distance and lookahead samples.
   - Replace status text such as `"Follow the floating arrow."` with floor-path wording.

2. Improve waypoint advancement:
   - Advance based on route projection passing a segment endpoint, with hysteresis.
   - Keep existing floor checks for vertical transitions.
   - Avoid advancing just because the user is horizontally close to a checkpoint on a different floor.
   - Preserve `advancedFromWaypointState(...)` arrival behavior, but clear `routeVisualState` as well as `arrowPose`.

3. Keep rerouting stable:
   - Continue using perpendicular distance from `projectToPath`.
   - Add hysteresis or cooldown if route visuals expose frequent near-threshold movement.
   - Reset route-visual smoothing when the route node-id signature changes after reroute.

4. Keep route correctness:
   - Ensure `Pathfinder` still prefers the truly shortest direct edge when graph weights are correct.
   - Keep the direct-edge guard in smoothing so a triangle route is not visually collapsed across missing edges.
   - Add tests for direct triangle edge preference and missing-direct-edge corner preservation.

Acceptance criteria:

- The user follows the rendered path, not a moving checkpoint target.
- Short direct edges are preferred when their weights are shorter.
- Smoothing never invents a visual shortcut that the graph does not allow.

## Phase 8 - Smoothing And Stability

Objective: remove jitter without making guidance laggy.

Smoothing rules:

- Smooth floor height separately from x/z route positions.
- Smooth route projection distance with a small deadband.
- Do not smooth across reroutes; reset route visual smoothing when the path id list changes.
- Also reset route visual smoothing when the inferred floor changes or a vertical transition cue becomes active/inactive.
- Keep yaw stable with wrap-aware smoothing, but derive yaw from segment tangent.
- Do not let arrow scale/alpha changes resize layout in a visually noisy way.
- Prefer smoothing the scalar route distance and floor height, then re-sampling deterministic arrow positions, over independently smoothing every arrow's x/z.

Suggested constants:

- `FLOOR_ARROW_SPACING_M = 1.0f`
- `FLOOR_ARROW_START_AHEAD_M = 0.8f`
- `FLOOR_ARROW_MAX_DISTANCE_M = 9.0f`
- `FLOOR_ARROW_COUNT = 7`
- `FLOOR_ARROW_Y_OFFSET_M = 0.02f`
- `DEFAULT_CAMERA_TO_FLOOR_M = 1.35f`
- `ROUTE_PROJECTION_DEADBAND_M = 0.08f`

Acceptance criteria:

- Arrows do not flicker or reorder while walking.
- Small localization corrections do not cause the whole path to jitter.
- Reroutes visibly update the path without lingering stale arrows.

## Phase 9 - Tests

Add unit tests around pure route math before relying on device testing.

Recommended tests:

1. `RouteVisualSamplerTest`
   - Straight hallway produces evenly spaced arrows.
   - Yaw points along the segment.
   - Samples stop at max distance.
   - Short final segment clamps at destination.
   - `sampleIndex` stays stable even when fewer than `FLOOR_ARROW_COUNT` samples are visible.

2. `RouteVisualSamplerTurnTest`
   - L-shaped route produces arrows before and after the corner.
   - Yaw changes only after samples pass the corner.
   - No direct diagonal shortcut is produced.

3. `RouteVisualSamplerFloorTest`
   - Only current-floor segments are sampled.
   - Vertical transition edge suppresses normal floor arrows.
   - Floor y uses floor estimate plus offset.
   - Same-floor `STAIRS` or `ELEVATOR` edge is treated as a transition, not a normal corridor arrow segment.

4. `PathfinderTest`
   - Direct triangle edge wins when shorter.
   - Missing direct edge keeps the intermediate corner.
   - Edge weights are refreshed or trusted consistently.

5. `FloorHeightEstimatorTest`
   - Deterministic fallback subtracts default camera-to-floor height.
   - Smoothed floor estimate rejects sudden implausible jumps.
   - Estimate resets when graph/building changes.

Testability rule:

- Keep sampler and floor-height math free of Android framework, Compose, SceneView, and ARCore `Pose` dependencies where practical. Convert to simple `Vec3`/data inputs at the ViewModel boundary.

Verification commands:

```powershell
$env:JAVA_HOME = 'C:\Users\emirh\AppData\Local\Programs\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Acceptance criteria:

- Unit tests cover route sampling and graph path correctness.
- Debug build succeeds.
- No context docs are modified.

## Phase 10 - Device QA Checklist

Test in a real hallway:

- Blue arrows appear on the floor.
- Arrows form a visible path, not a single floating pointer.
- Walking forward causes arrows to progress smoothly.
- Turning a corner keeps arrows on the route, not through the wall.
- Stairs/elevator edges show a transition cue instead of floor arrows going vertical.
- Tracking loss hides or freezes guidance gracefully.
- Rerouting updates the floor path within the expected cooldown.
- Destination arrival removes arrows and shows arrived state.

## File Change Plan

Expected implementation files:

- `app/src/main/assets/arrow.glb`
- `app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt` if horizontal plane finding is enabled explicitly.
- `app/src/main/java/com/example/vecturai/ui/navigation/RouteVisualModels.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/RouteVisualSampler.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/FloorHeightEstimator.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt`
- `app/src/main/java/com/example/vecturai/graph/Pathfinder.kt` only if tests show path-cost issues remain.
- `app/src/test/java/com/example/vecturai/graph/PathfinderTest.kt`
- New route visual sampler test file under `app/src/test/java/com/example/vecturai/ui/navigation/`
- New floor height estimator test file under `app/src/test/java/com/example/vecturai/ui/navigation/`

Files to avoid:

- Do not edit `codebase-docs/`.

## Recommended Implementation Order

1. Update the arrow asset and fallback to the blue floor-arrow look.
2. Add route visual data classes with no behavior change.
3. Extract route sampling into pure functions that can be unit tested.
4. Add pure sampler tests before wiring the sampler into AR state.
5. Add floor-height estimation and floor y offsets.
6. Populate `routeVisualState` from `NavigationViewModel`.
7. Render repeated floor arrows in `NavigationScreen` using an instanced model pool.
8. Add or refine optional `PathNode` path line rendering.
9. Add transition cues for stairs/elevators.
10. Remove or disable the old floating checkpoint arrow path.
11. Add remaining tests and run the full verification commands.

## Final Acceptance Criteria

- The app no longer relies on one floating arrow for normal navigation.
- The visual guidance looks like a blue floor-arrow path.
- Arrows sit on the floor and use yaw-only rotation.
- Route visuals follow graph edges and do not create shortcuts.
- Vertical transitions are treated separately.
- Jitter is reduced through projection, floor-height smoothing, and stable render keys.
- Rendering uses preallocated model/material resources and does not load GLB assets per frame.
- Diagnostic/status text describes floor-path guidance, not a floating arrow.
- Unit tests and `assembleDebug` pass.

## Final Coverage Audit

After implementation, the agent must perform a full coverage audit before calling the work complete:

- Re-read this plan and verify every phase, file change, test, and acceptance criterion has been implemented or explicitly justified as unnecessary.
- Trace all direct dependents of changed symbols again, especially `ArrowPose`, `RouteVisualState`, route sampling helpers, floor-height estimation, `NavigationScreen`, and `NavigationViewModel`.
- Confirm no stale floating-arrow logic remains in normal navigation, including status text, diagnostics, lifecycle clearing, and renderer paths.
- Confirm visual behavior against the reference intent: blue floor-anchored repeated arrows, yaw-only orientation, stable spacing, correct turns, and separate stairs/elevator cues.
- Run the listed verification commands and inspect failures rather than assuming build success is enough.
- Report any remaining gaps, device-only checks, or intentional deviations clearly in the final handoff.
