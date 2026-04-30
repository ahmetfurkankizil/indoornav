# 3D Arrow Rendering Pipeline — Audit & Fix Plan

Scope: bugs, logic errors, visual defects and improvements in the AR navigation arrow pipeline.

Files in scope:
- `app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`
- `app/src/main/java/com/example/vecturai/ui/ArAssetUtils.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt` (for R1)

Conventions used below:
- ARCore: right-handed, Y-up, camera looks toward −Z.
- SceneView/Filament: same handedness, Y-up.
- "Session space" = AR world coordinates as exposed by ARCore.

---

## Critical (visible "sloppy" demo behavior)

### C1 — Smooth-transform fights a per-frame world-space target
**Where:** `NavigationScreen.kt:165-194` (the `arrowPose?.let { Node(...) }` block, with `isSmoothTransformEnabled = true`, `smoothTransformSpeed = 10f`).
**Symptom:** Arrow lags behind the user as they walk — never converges to the intended 1.5 m offset. The smoother is interpolating toward a session-space target that itself moves every frame.
**Fix (preferred):** Make the arrow camera-relative. Either:
1. Parent the arrow `Node` to a camera-anchored node and use a constant **local** offset `(0, 0, -ARROW_FORWARD_OFFSET_M)`; only the yaw needs to update.
2. Or: keep world-space placement but **disable position smoothing**. Smooth only yaw (e.g. exponential lerp on `yawDegrees` in the ViewModel before publishing). Suggested code shape:
```kotlin
Node(
    position = pose.position.toScenePosition(),
    rotation = Rotation(y = pose.yawDegrees),
    apply = {
        isSmoothTransformEnabled = false   // position is recomputed each frame
    }
)
```
Apply yaw smoothing in `NavigationViewModel` (track previous yaw, lerp toward new).

### C2 — Yaw is computed from the camera, but the arrow is drawn 1.5 m ahead
**Where:** `ArrowRenderer.kt:16` — `yawDegreesToward(cameraPose.translationVec(), target)`.
**Symptom:** Arrow visibly mis-aims at close waypoints (worst exactly when approaching, since user stares at it most then).
**Fix:** Compute yaw from the **arrow position**, not the camera:
```kotlin
fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose {
    val position = positionInFrontOfCamera(cameraPose, ARROW_FORWARD_OFFSET_M)
    return ArrowPose(
        position = position,
        yawDegrees = yawDegreesToward(position, target),
        distanceToTargetMeters = horizontalDistanceMeters(cameraPose.translationVec(), target)
    )
}
```

### C3 — Vertical pitch contaminates horizontal placement
**Where:** `PoseUtils.kt:81-82` — `positionInFrontOfCamera` uses the full 3D `forwardVec()`.
**Symptom:** Arrow rises/dips when user looks up/down; "floating arrow" promise broken.
**Fix:** Project forward to horizontal before scaling and lock Y to camera height:
```kotlin
fun positionInFrontOfCamera(cameraPose: Pose, meters: Float): Vec3 {
    val cam = cameraPose.translationVec()
    val fwd = cameraPose.forwardVec()
    val flat = Vec3(fwd.x, 0f, fwd.z).normalized()
    return Vec3(cam.x + flat.x * meters, cam.y, cam.z + flat.z * meters)
}
```
Optional: after computing `position`, gently bias Y toward the waypoint Y:
`val y = lerp(cam.y, targetY, 0.25f)`.

### C4 — Model-forward convention is silently assumed +Z
**Where:** `PoseUtils.kt:74-77` (`yawDegreesToward`) + the .glb. Fallback procedural geometry (`NavigationScreen.kt:182-191`) is +Z forward, masking the issue if `arrow.glb` is authored −Z (glTF default).
**Symptom:** With a stock −Z-forward arrow.glb, arrow points 180° away from target.
**Fix:** Introduce an explicit model-forward offset in `ArrowRenderer`:
```kotlin
const val ARROW_MODEL_YAW_OFFSET_DEG = 0f   // set 180f if arrow.glb forward is -Z
```
Apply at render time: `Rotation(y = pose.yawDegrees + ARROW_MODEL_YAW_OFFSET_DEG)`.
Verify visually with the actual `arrow.glb` once and bake the right constant.

### C5 — Arrow target teleports every 15 s relocalize cycle
**Where:** `NavigationViewModel.kt:313` — `graphToSessionPose = sessionFromGraphPose(node, anchor.pose)` is overwritten by whichever node resolved most recently.
**Symptom:** Estimated node poses (including the active waypoint, if unresolved) jump between resolves; with C1 smoothing this manifests as a sideways swing every cycle.
**Fix options:**
1. **Freeze during `Navigating`:** stop overwriting `graphToSessionPose` once `phase == Navigating` unless drift exceeds a threshold (e.g. 0.5 m delta from current transform).
2. **Average across resolved anchors:** for each resolved node compute its candidate transform `anchor.pose.compose(node.graphPose().inverse())`, then use a weighted/median translation + slerp rotation as the active transform.
3. Combine: average all resolved anchors, only update if delta > threshold.

### C6 — Resolved-vs-estimate switch causes mid-route waypoint jumps
**Where:** `NavigationViewModel.kt:331-342` (`publishNodeEstimates`). When a waypoint becomes newly resolved, `pose` flips from estimate → anchor pose in one frame.
**Fix:** Interpolate. Keep a per-node `displayedPose` that exponentially follows the latest authoritative pose (constant rate, e.g. 4 Hz of catch-up). When `Navigating`, only the **active waypoint**'s displayed pose drives the arrow target. New resolves still update truth but the displayed pose eases in.

---

## Logic / correctness

### L1 — Distance metric inconsistency
**Where:**
- Advance test 3D: `NavigationViewModel.kt:380` (`distanceMeters(cameraPosition, waypointPosition)`).
- Arrow distance horizontal: `ArrowRenderer.kt:17`.
- Destination distance 3D: `NavigationViewModel.kt:419`.
**Fix:** Pick **horizontal** everywhere for indoor. Replace the two 3D calls with `horizontalDistanceMeters(...)`.

### L2 — `Vec3.normalized()` zero-length fallback returns `(0,0,-1)` → silent garbage yaw
**Where:** `PoseUtils.kt:23-26`. When camera and waypoint share horizontal position, yaw resolves to `atan2(0,-1)=180°` and the arrow snaps.
**Fix:** Make `yawDegreesToward` resilient instead of relying on `normalized()`'s magic fallback:
```kotlin
fun yawDegreesToward(from: Vec3, to: Vec3): Float? {
    val d = (to - from).horizontal()
    if (d.length() < 1e-3f) return null
    return Math.toDegrees(atan2(d.x.toDouble(), d.z.toDouble())).toFloat()
}
```
`ArrowRenderer.floatingArrowPose` returns `null` (or holds last valid yaw) when there's no meaningful direction; UI hides the arrow in that frame.

### L3 — `distanceToTargetMeters` reported from camera, not arrow
**Where:** `ArrowRenderer.kt:17`. On-screen distance disagrees with perceived gap when close.
**Fix:** Either (a) keep camera-relative distance but rename the field for clarity, or (b) compute from `position` (the arrow). Recommend (a) since UX wants distance from the user, not the arrow.

### L4 — `selectDestination` runs `updateNavigationProgress` with potentially stale camera pose
**Where:** `NavigationViewModel.kt:239`.
**Symptom:** One-frame placement jolt before the next `onSessionUpdated`.
**Fix:** Skip the synchronous call; let the next frame populate `arrowPose`. Or stamp `latestCameraPose` with a frame timestamp and ignore if older than e.g. 100 ms.

### L5 — Tracking-loss handling clears `arrowPose` but doesn't reset Node smoothing
**Where:** `NavigationViewModel.kt:198-207`.
**Symptom:** On tracking re-acquire the SceneView Node still holds the previous interpolated transform and pops.
**Fix:** Once C1 is applied (smoothing off, or camera-parented), this disappears. If keeping smoothing, force-reset by toggling Node identity on tracking transitions (see R2).

### L6 — `currentNodeId` is computed from estimates that may be unreliable
**Where:** `NavigationViewModel.kt:365-371` (`updateCurrentNode`).
**Symptom:** With only estimated poses, "closest node" is meaningless and `selectDestination` can pick a wrong start.
**Fix:** Restrict to resolved nodes when any are resolved:
```kotlin
val candidates = poses.filter { it.isResolved }.ifEmpty { poses }
val closest = candidates.minByOrNull { horizontalDistanceMeters(cameraPosition, it.pose.translationVec()) }
```

---

## Render / GL / SceneView

### R1 — Arrow renders without depth occlusion (visible through walls)
**Where:** `NavigationScreen.kt:141-151` (`ARSceneView(... planeRenderer = false ...)`) and `ArSessionConfig.kt`.
**Fix:** Enable ARCore Depth API in `ArSessionConfig.configureIndoorCloudSession`:
```kotlin
if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
    config.depthMode = Config.DepthMode.AUTOMATIC
}
```
Then use a depth-aware material (or SceneView's depth occlusion option) for the arrow so walls/objects occlude it. Verify on-device — older devices fall back gracefully because of the support check.

### R2 — Arrow Node lifecycle has no stable identity
**Where:** `NavigationScreen.kt:165-194`.
**Symptom:** Toggling `arrowPose` between non-null/null (Arrived, tracking loss) breaks SmoothTransform state and risks reallocating the Node each recomposition.
**Fix:** Wrap in a `key(...)`:
```kotlin
key("nav-arrow") {
    arrowPose?.let { pose -> Node(...) { ... } }
}
```
Ensure the `Node` is the same instance across recompositions; the `apply` block runs only once.

### R3 — `arrowMaterial` allocated even when GLB is loaded
**Where:** `NavigationScreen.kt:131-133`.
**Fix:** Only create when needed:
```kotlin
val arrowMaterial = remember(materialLoader, hasArrowAsset) {
    if (hasArrowAsset) null
    else materialLoader.createColorInstance(Color(0xFFFF5722), roughness = 0.45f)
}
```
Guard usages with `?:` / null-checks.

### R4 — `scaleToUnits = 0.45f` couples size to .glb bounding box
**Where:** `NavigationScreen.kt:179`.
**Fix:** Replace with explicit per-axis scale (`scale = Scale(0.45f)`) after measuring the model, or normalize the model export. Document the assumption in a comment near the constant.

### R5 — Fallback procedural arrow self-intersects (Z-fighting on seam)
**Where:** `NavigationScreen.kt:182-191`. Cube spans z = −0.05…+0.41, sphere at z = +0.45 with radius 0.12 (spans +0.33…+0.57). ~0.08 m overlap.
**Fix:** Move sphere to `Position(z = 0.50f)` (or shrink to `radius = 0.09f`, center `z = 0.50f`) so its base sits at cube tip.

### R6 — `Rotation(y = ...)` Euler order is implicit
**Where:** `NavigationScreen.kt:168`.
**Fix (defensive):** Build a quaternion explicitly:
```kotlin
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.Quaternion
val rotQ = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), pose.yawDegrees)
Node(position = ..., quaternion = rotQ, apply = { ... })
```
Safe today (X=Z=0), but pins behavior if someone later adds pitch/roll.

### R7 — `hasValidGlbAsset` only checks 4-byte magic
**Where:** `ArAssetUtils.kt:5-14`.
**Fix:** Validate full 12-byte GLB header (`magic 'glTF'`, `version=2`, `length` ≤ asset size):
```kotlin
fun hasValidGlbAsset(context: Context, assetPath: String): Boolean = runCatching {
    context.assets.openFd(assetPath).use { afd ->
        afd.createInputStream().use { input ->
            val header = ByteArray(12)
            if (input.read(header) != 12) return@runCatching false
            val magicOk = header[0]==0x67.toByte() && header[1]==0x6C.toByte() &&
                          header[2]==0x54.toByte() && header[3]==0x46.toByte()
            val version = (header[4].toInt() and 0xff) or
                          ((header[5].toInt() and 0xff) shl 8) or
                          ((header[6].toInt() and 0xff) shl 16) or
                          ((header[7].toInt() and 0xff) shl 24)
            magicOk && version == 2
        }
    }
}.getOrDefault(false)
```
And: surface a UI fallback when async model loading itself fails (catch in a SceneView listener if available).

### R8 — `rememberModelInstance` not reactive to asset state changes
**Where:** `NavigationScreen.kt:134-139`.
**Fix:** Key on `hasArrowAsset` so the model loader rebuilds if validity flips:
```kotlin
val arrowModelInstance = remember(modelLoader, hasArrowAsset) {
    if (hasArrowAsset) /* load */ else null
}
```
Use SceneView's loader API directly inside `remember` to avoid Compose-internal `rememberModelInstance` keying assumptions.

---

## Incidental / cleanup

### D1 — `relativePose` only used by mapping flow
**Where:** `PoseUtils.kt:42`. Not a render bug; leave or move to a mapping-only util file.

### D2 — `MAX_PARALLEL_RESOLVES = 40` can starve the resolve loop
**Where:** `NavigationViewModel.kt:439`. Indirectly delays first arrow render. Consider lowering to 8–16 and verifying with telemetry; orthogonal to rendering correctness.

---

## Suggested fix order (high impact first)

1. **C3** (horizontal-only forward in `positionInFrontOfCamera`) — `PoseUtils.kt:81-82`.
2. **C2** (yaw from arrow position) — `ArrowRenderer.kt:12-19`.
3. **C1** (drop position smoothing or parent to camera node) — `NavigationScreen.kt:165-194`. Pair with optional yaw smoothing in `NavigationViewModel`.
4. **C4** (verify model forward, add `ARROW_MODEL_YAW_OFFSET_DEG`) — `ArrowRenderer.kt`, `NavigationScreen.kt:168`.
5. **C5 + C6** (stabilize `graphToSessionPose`, ease-in waypoint pose changes) — `NavigationViewModel.kt:313, 331-342`.
6. **L1** (unify on horizontal distance) — `NavigationViewModel.kt:380, 419`.
7. **L2** (nullable yaw, drop magic fallback) — `PoseUtils.kt:74-77` + downstream.
8. **R1** (depth occlusion) — `ArSessionConfig.kt`, material setup.
9. **R2** (`key("nav-arrow")`) — `NavigationScreen.kt:165-194`.
10. **R5** (fallback geometry seam) — `NavigationScreen.kt:182-191`.
11. **R6, R7, R8, R3, R4, L4, L5, L6** — cleanup pass.

---

## Verification checklist (manual, on-device)

- Walking forward 5 m: arrow stays glued ~1.5 m ahead, no lag, no swing. (C1, C3)
- Looking up/down: arrow Y stays roughly at camera height. (C3)
- Approaching a waypoint within 1.5 m: arrow remains aimed at waypoint, no over-rotation. (C2)
- Standing directly under a waypoint: arrow either hides or holds last yaw — no snap to a default direction. (L2)
- After a re-resolve cycle (every 15 s): no waypoint jump. (C5, C6)
- Across two floors: distances and advance threshold consistent. (L1)
- Walls between user and arrow: arrow occluded by walls. (R1)
- Arrived → pick another → tracking loss → re-acquire: no Node pop. (L5, R2)
- arrow.glb removed from assets: fallback procedural arrow renders without seam. (R5)
