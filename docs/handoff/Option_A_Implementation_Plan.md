# Option A — 3D OpenGL Arrow Rendering — Implementation Plan

**Audience:** AI coding agent executing this plan end-to-end without follow-up questions.
**Scope:** Replace the current 2D Compose-based arrow overlay with true 3D arrow meshes rendered in OpenGL ES 2.0 inside the existing ARCore session, while preserving every other AR/UI/data behavior.
**Platform:** Native Android (Kotlin + Jetpack Compose + ARCore 1.46.0). This is **not** a Unity project.
**Status:** Ready for execution. All facts in this plan have been verified against current source.

---

## 1. Project context

VecturAI is an indoor AR navigation app. The Android client (`apps/androidApp`) uses:
- One `ComponentActivity` ([ArCameraActivity.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArCameraActivity.kt)) that owns the entire QR-to-AR flow.
- One `GLSurfaceView` set to `RENDERMODE_CONTINUOUSLY` with a single renderer ([UnifiedArRenderer.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt)).
- One long-lived `ARCore Session` ([UnifiedArSession.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArSession.kt)).
- One viewmodel for AR navigation state ([AndroidArNavigationViewModel.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt)).
- Compose UI overlays on top of the GLSurfaceView ([ArNavigationScreen.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt)).
- Reviewed-package data loaded via [AndroidReviewedPackageLoader.kt](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/data/AndroidReviewedPackageLoader.kt), which provides per-arrow `ArrowPlacementData` (position, forward vector, type, cumulative distance).

Read these before writing code:
- `AGENTS.md`
- `CLAUDE.md`
- `codebase-docs/codebase-map.md`
- `codebase-docs/features/ar_navigation_android.md`

---

## 2. Goal

Render navigation arrows as **lit, perspective-correct 3D meshes** anchored in AR world space, instead of the current screen-space Compose icons. Arrows must:
1. Sit on the floor (or at the configured `arrowHeightOffsetMeters`) at their building-local positions.
2. Point along the route's local forward direction.
3. Shade with a soft top-light gradient and a rim highlight for depth perception.
4. Cast a cheap fake "shadow blob" at floor height beneath each arrow.
5. Pulse the next imminent arrow subtly.
6. Respect the existing fade-behind, lookahead, alpha, and scale logic from [ArRouteRenderer.updateVisibility](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArRouteRenderer.kt).
7. Render at 60 fps on the existing supported devices without dropping frames.

All other AR behavior — alignment from entrance poster, route progress, tracking labels, next-action card, ETA HUD, arrival, error overlays, QR scanning, destination select, route preview, lifecycle, ARCore session resume/pause — must remain bit-identical.

---

## 3. Non-goals

- Do not change ARCore session lifecycle, alignment math, marker detection, lookahead/fade rules, or arrival logic.
- Do not introduce a 3D engine (Filament/Sceneform). Keep it OpenGL ES 2.0, hand-written, no new gradle dependencies.
- Do not load external `.obj`/`.glb` assets. The arrow mesh is hard-coded in code (a small chevron prism, ≤ 24 triangles).
- Do not add Android plane detection or anchor-on-plane. Arrows are positioned by transform from the existing `setAlignmentTransform`.
- Do not add localization, sound, or new haptics.
- Do not touch iOS or shared modules.
- Do not modify the Android manifest (the `HIGH_SAMPLING_RATE_SENSORS` fix is already applied — see [AndroidManifest.xml:7](../../apps/androidApp/src/main/AndroidManifest.xml)).

---

## 4. Assumptions (verified)

- App targets `compileSdk 35` / `targetSdk 35`, `minSdk 28` (per [libs.versions.toml:21-23](../../gradle/libs.versions.toml)).
- ARCore client lib is `com.google.ar:core:1.46.0` already on the classpath (per [libs.versions.toml:26](../../gradle/libs.versions.toml) and [build.gradle.kts](../../apps/androidApp/build.gradle.kts)).
- `GLSurfaceView` is created with `setEGLContextClientVersion(2)` and `preserveEGLContextOnPause = true` (per [ArCameraActivity.kt:163-167](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArCameraActivity.kt)). All GL code MUST be ES 2.0 compatible (no GLES30/uniform buffer objects/instancing/VAOs).
- `UnifiedArRenderer.onDrawFrame` runs on the GL thread. Its `onFrame(frame, width, height, rotationDegrees)` callback also runs on the GL thread. The viewmodel's `onFrame()` is therefore invoked on the GL thread.
- `ArrowPlacementData` carries `forwardDx/Dy/Dz` (per [AndroidReviewedPackageLoader.kt:352-363](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/data/AndroidReviewedPackageLoader.kt)).
- `routePackage.config.routeRendering.arrowHeightOffsetMeters` exists and is the floor-to-arrow elevation in meters. Default is small (~0.05 m).
- `frame.camera.getViewMatrix(out, 0)` and `getProjectionMatrix(out, 0, 0.1f, 100f)` are valid only inside the same `onDrawFrame` call. We must consume them on the GL thread the frame they were produced on.
- `ArRouteRenderer.updateVisibility(...)` is called from multiple threads (viewmodel main + GL thread via `onFrame`). Arrow snapshots passed to GL must be thread-safely published.

---

## 5. Constraints

- OpenGL ES 2.0 only. No `glDrawElementsInstanced`, no UBO, no VAO, no compute shaders.
- No new gradle dependencies. No third-party 3D libs.
- No additional permissions (`HIGH_SAMPLING_RATE_SENSORS` already declared).
- The new code must compile under Kotlin 2.1.10 + JVM target 21 already configured.
- Memory: total mesh + uniforms must be < 50 KB. The chevron prism is ~30 vertices.
- Performance budget: rendering ≤ 12 arrows per frame must add ≤ 2 ms of GL work on a Pixel 6-class device (target 60 fps stable).
- Behavior must be a strict superset of current arrow visuals — all alpha/scale/visibility transitions remain intact.

---

## 6. Current state (verified)

### 6.1 Arrow data path (do NOT change)
1. Reviewed package loader produces `List<ArrowPlacementData>` at [AndroidReviewedPackageLoader.kt:352](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/data/AndroidReviewedPackageLoader.kt).
2. Marker detection completes → [AndroidArNavigationViewModel.handleMarkerDetected](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt#L298) calls `routeRenderer.placeAllArrows(pkg.arrows)` and sets the alignment transform.
3. `routeRenderer.updateVisibility(userCumulativeDistance)` is called from `sampleCameraPose`, `advanceProgress`, `handleMarkerDetected`. It computes `List<VisibleArrow>` with world `xyz`, `alpha`, `scale`.

### 6.2 Current rendering path (TO BE REPLACED)
1. Per-frame, `AndroidArNavigationViewModel.onFrame` (GL thread) extracts `viewMatrix` and `projectionMatrix` from `frame.camera`, then calls `routeRenderer.projectVisibleArrows(...)` to produce 2D `ProjectedArrow(xPx, yPx, alpha, scale)` (see [ArRouteRenderer.kt:129-165](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArRouteRenderer.kt)).
2. The result lands in `uiState.projectedArrows` (StateFlow).
3. [ArNavigationScreen.ProjectedArrowLayer](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt#L131) renders each as a Compose `Box` (`CircleShape`) with a Material `Icon` inside.

### 6.3 GL pipeline today (MUST STAY)
- [UnifiedArRenderer.onDrawFrame](../../apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt#L64) clears, calls `session.update()`, calls `setDisplayGeometry`, draws the AR camera background to a fullscreen quad with `GL_TEXTURE_EXTERNAL_OES`, then dispatches `onFrame` callback.
- Depth is currently disabled and depth mask is reset to true after the camera background draw. Depth buffer exists.

---

## 7. Target behavior

After this plan is executed:

- The Compose `ProjectedArrowLayer` no longer draws arrows.
- `UnifiedArRenderer.onDrawFrame` draws, in order:
  1. Camera background quad (existing).
  2. **NEW**: 3D shadow blobs at floor height for each visible arrow (cheap ellipse fan).
  3. **NEW**: 3D arrow meshes at world position, oriented along `forwardDx/Dz`, with type-tinted Lambert+rim shading.
  4. ARCore frame callback for the viewmodel (existing).
- Arrows visually behave identically to today w.r.t. lookahead/fade/alpha/scale; the difference is they are now **anchored in 3D world space** with proper perspective.
- The `next imminent` arrow (smallest `cumulativeDistance >= userCumulativeDistance` with type ≠ FOLLOW or, if all FOLLOW, the nearest) pulses with `1.0 → 1.08 → 1.0` over 1.2 s.
- Arrival hides all arrows (existing logic via `routeRenderer.hideAllArrows()` already covers this — visibleArrows becomes empty, GL renders nothing).
- All other Compose overlays (top bar, action card, ETA HUD, alignment overlay, error overlays) render unchanged on top.

---

## 8. Architecture notes

### 8.1 Threading model

| Component | Thread | Notes |
|---|---|---|
| `UnifiedArRenderer.onDrawFrame` | GL | The only place we issue GL calls. |
| `viewmodel.onFrame(frame, w, h)` | GL (called from `onDrawFrame`) | Reads matrices, runs route logic, mutates StateFlow. |
| `routeRenderer.updateVisibility(...)` | Mixed (GL + main) | Called from `onFrame` (GL) and from `advanceProgress`/`endNavigation` (main). |
| `routeRenderer.placeAllArrows(...)` | GL (via `handleMarkerDetected`) or main (`endNavigation`) | |

Concurrency rule: the new 3D renderer reads `visibleArrows` strictly via an `AtomicReference<Snapshot>` published by `ArRouteRenderer`. Writers replace the reference; readers grab it once per frame.

### 8.2 Coordinate system

ARCore's world space is right-handed: +X right, +Y up, −Z forward at session start. `ArRouteRenderer.transformToAR` already converts building-local coords (also right-handed: +X right, +Y up, −Z forward) into AR-world coords using `alignmentOffset*` and `alignmentRotYDeg`. 3D rendering uses these AR-world coords directly with ARCore's view+projection matrices.

### 8.3 Mesh design

A chevron prism — front-pointing wedge with a flat base. Fits inside a 0.40 m × 0.04 m × 0.50 m oriented bounding box at unit scale. Object-space convention: forward = +Z, up = +Y, base centered at origin.

```
        +Z (forward)
         /\
        /  \
       /    \
      /------\
     |  body  |   width along X
     |  body  |
      \------/
        ^^^^
        base
```

Vertex set (positions only, 14 verts max):
- 4 base corners (low-Y rectangle: rear-left, rear-right, front-base-left, front-base-right)
- 1 tip (front-most apex at high-Z)
- 4 top corners (mirror of base, raised by `+0.04 m`)
- 1 apex top (raised tip)
- Repeated as needed to give per-face flat normals.

Triangulate into ~16 triangles (top, bottom, four side faces, two front-tip faces). Hard-code as a `FloatArray` of `(x, y, z, nx, ny, nz, vGradient)` where `vGradient` is 0 at the base and 1 at the top — used in the fragment shader to interpolate the body→edge color.

### 8.4 Shadow blob

A flat dark-tinted disk (16-segment fan) on the floor plane (`y = 0` in building space, transformed to AR by `ArRouteRenderer.transformToAR`). Drawn before the arrow with depth-write off and additive-friendly blend, so it composites without z-fighting against the AR camera background (which has no depth) but still fades correctly behind arrows.

### 8.5 Lighting model (fragment shader)

```
final = base * (ambient + diffuse * max(0, N·L)) + rim * pow(1 - max(0, N·V), 2)
final.a *= u_alpha
gl_FragColor = vec4(final.rgb, final.a)
```

- `L = normalize(vec3(0.3, 1.0, 0.4))` (top-light from camera-forward)
- `ambient = 0.35`
- `diffuse = 0.65`
- `rim = 0.25 * tint` where tint is the per-type color
- Multiply final RGB by per-type color; modulate value by `vGradient` so the top edge is brighter.

### 8.6 Per-arrow color (matches existing palette)

| Type | Hex (top) | Hex (bottom) |
|---|---|---|
| FOLLOW | `#3DB9FF` | `#1366C2` |
| TURN_LEFT, TURN_RIGHT | `#FFC355` | `#B57105` |
| U_TURN | `#FB923C` | `#9A3412` |
| DESTINATION | `#34D399` | `#047857` |

Top/bottom blended in the fragment shader via `vGradient`.

### 8.7 Render state per frame

```
glEnable(GL_BLEND)
glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
glEnable(GL_DEPTH_TEST)
glDepthFunc(GL_LEQUAL)
glDepthMask(true)
glDisable(GL_CULL_FACE)   // arrows look fine from any side; avoids backface invisibility issues
```

After arrow pass, restore the renderer's neutral state (`glDisable(GL_DEPTH_TEST)` for whatever follows; today the rest of the frame issues no GL calls so resetting is optional but recommended).

### 8.8 Compose layer

`ProjectedArrowLayer` is removed from the screen. The `uiState.projectedArrows` field is left in place but always empty (or removed — see step 11.4).

---

## 9. Files to create

### 9.1 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/Arrow3DGeometry.kt`
Pure data — vertex array, index array, color tables. No GL state.

### 9.2 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArArrow3DRenderer.kt`
Owns:
- Two GL programs (`arrowProgram`, `shadowProgram`) compiled lazily on first `draw`.
- Two static `FloatBuffer`s for vertex + normal + gradient data.
- One static `ShortBuffer` for indices.
- Per-frame `draw(visibleArrows, viewMatrix, projectionMatrix, frameTimeMs)` entry point.

### 9.3 (Optional) `docs/handoff/Option_A_Verification_Notes.md`
Short notes file for the executing agent to record device-specific findings during validation. Empty stub at first.

---

## 10. Files to modify

### 10.1 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArRouteRenderer.kt`

**Add:**
- A new `data class RenderableArrow3D(val type: ArrowPlacementType, val worldX: Float, val worldY: Float, val worldZ: Float, val headingRad: Float, val alpha: Float, val scale: Float, val isImminent: Boolean, val arrowId: String)`.
- A field `private val visibleSnapshot = AtomicReference<List<RenderableArrow3D>>(emptyList())`.
- A method `fun snapshot(): List<RenderableArrow3D> = visibleSnapshot.get()`.

**Modify `updateVisibility`:**
- After computing the existing `visible: MutableList<VisibleArrow>`, build a parallel `List<RenderableArrow3D>` by:
  - Computing heading in AR space: take `(forwardDx, _, forwardDz)` from `ArrowPlacementData`, transform via `transformDirectionToAR(...)`, then `headingRad = atan2(arZ, arX)` (note: ARCore uses `−Z forward`, so verify sign by smoke-testing).
  - Marking `isImminent = (arrow.cumulativeDistance == firstNonFollowAhead.cumulativeDistance)` where `firstNonFollowAhead` is the first arrow with `cumulativeDistance >= userCumulativeDistance` and `type != FOLLOW`.
  - Atomically publish via `visibleSnapshot.set(renderable)`.

**Keep (do not delete):**
- `projectVisibleArrows` — keep it for backward safety / debug HUD; it just goes unused. Deleting it later is a follow-up cleanup.
- Existing `VisibleArrow` and `ProjectedArrow` types.

### 10.2 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt`

**Add to constructor:**
```kotlin
private val routeRenderer: ArRouteRenderer,
```
Wired from `ArCameraActivity` (which already obtains `arViewModel` from Koin and could expose `arViewModel.routeRenderer` — see step 10.4).

**Add field:**
```kotlin
private val arrow3D = ArArrow3DRenderer()
```

**Modify `onSurfaceCreated`:**
- After the existing camera shader program is compiled, call `arrow3D.onSurfaceCreated()` to compile its programs.

**Modify `onDrawFrame`:**
- After `drawCameraBackground(frame)` and before `onFrame(...)`, do:
  ```kotlin
  val view = FloatArray(16)
  val proj = FloatArray(16)
  frame.camera.getViewMatrix(view, 0)
  frame.camera.getProjectionMatrix(proj, 0, 0.1f, 100f)
  arrow3D.draw(
      arrows = routeRenderer.snapshot(),
      viewMatrix = view,
      projectionMatrix = proj,
      frameTimeMs = System.nanoTime() / 1_000_000L,
  )
  ```
- Restore neutral state (`glDisable(GL_DEPTH_TEST)`, `glDepthMask(true)`).

### 10.3 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt`

**Add:**
- A `val routeRenderer: ArRouteRenderer get() = this.routeRenderer` (already a private constructor field; promote to internal-readable). Use `internal` visibility so `ArCameraActivity` can pass it to the renderer.

**Remove (or keep but stop populating):**
- The block in `onFrame(...)` that builds `projected = routeRenderer.projectVisibleArrows(...)` and updates `_uiState` with `projectedArrows`. Replace with:
  ```kotlin
  if (_uiState.value.isAligned && !_uiState.value.hasArrived) {
      val now = System.currentTimeMillis()
      if (!_uiState.value.isSimulated && now - lastPoseSampleMs >= 500L) {
          lastPoseSampleMs = now
          sampleCameraPose(frame)
      }
  }
  ```
  (Same logic, minus the projection update.)
- Drop `projectedArrows` from `ArNavigationUiState` initial / update sites if you choose to remove the field. Otherwise leave it as `emptyList()` always.

### 10.4 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArCameraActivity.kt`

**Modify the `renderer by lazy { ... }` block:**
```kotlin
private val renderer by lazy {
    UnifiedArRenderer(
        activity = this,
        unifiedSession = unifiedSession,
        routeRenderer = arViewModel.routeRenderer,        // NEW
        onTextureCreated = { textureId -> ... },
        onFrame = ::onArFrame,
        onFatalFailure = ::finishWithCameraError,
    )
}
```
No other change to this file.

### 10.5 `apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt`

**Remove:**
- The call site `ProjectedArrowLayer(uiState)` at line 94.
- The `private fun ProjectedArrowLayer(...)` definition at lines 131-168.
- Imports of `LocalDensity`, `IntOffset`, `roundToInt`, `colorForArrow`, `iconForArrow`, `ArrowPlacementType` if they become unused after removal. Do not delete `colorForArrow` if any other call site uses it (verify with `grep`).

**Keep everything else** — top bar, alignment overlay, action card, ETA HUD, alignment timed-out card, arrival sheet, action/tracking icon helpers.

---

## 11. Step-by-step implementation

Execute strictly in this order. After each step, run `./gradlew :apps:androidApp:assembleDebug`. The build must succeed before the next step.

### Step 1 — Add `RenderableArrow3D` and snapshot publishing

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArRouteRenderer.kt`

1. Add import: `import java.util.concurrent.atomic.AtomicReference`.
2. Add inside the class:
   ```kotlin
   data class RenderableArrow3D(
       val arrowId: String,
       val type: ArrowPlacementType,
       val worldX: Float,
       val worldY: Float,
       val worldZ: Float,
       val headingRad: Float,
       val alpha: Float,
       val scale: Float,
       val isImminent: Boolean,
   )

   private val snapshotRef = AtomicReference<List<RenderableArrow3D>>(emptyList())
   fun snapshot(): List<RenderableArrow3D> = snapshotRef.get()
   ```
3. At the end of `updateVisibility(userCumulativeDistance: Double)`, before `return visibleArrows`, compute and publish the renderable list:
   ```kotlin
   val firstNonFollowAhead = allArrows
       .firstOrNull { it.cumulativeDistance >= userCumulativeDistance && it.type != ArrowPlacementType.FOLLOW }
   val firstAheadDistance = firstNonFollowAhead?.cumulativeDistance
   val renderable = visible.map { v ->
       val arForward = transformDirectionToAR(v.arrow.forwardDx, 0.0, v.arrow.forwardDz)
       val headingRad = kotlin.math.atan2(-arForward[2], arForward[0])
       RenderableArrow3D(
           arrowId = v.arrow.id,
           type = v.arrow.type,
           worldX = v.worldX,
           worldY = v.worldY,
           worldZ = v.worldZ,
           headingRad = headingRad,
           alpha = v.alpha,
           scale = v.scale,
           isImminent = firstAheadDistance != null && v.arrow.cumulativeDistance == firstAheadDistance,
       )
   }
   snapshotRef.set(renderable)
   ```
4. In `clearArrows()`, `hideAllArrows()`, and `placeAllArrows(...)`, also call `snapshotRef.set(emptyList())` to clear the snapshot atomically.

Build check: `./gradlew :apps:androidApp:assembleDebug`.

### Step 2 — Promote `routeRenderer` visibility

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt`

1. Change the constructor parameter:
   ```kotlin
   class AndroidArNavigationViewModel(
       private val markerDetector: ArMarkerDetector,
       internal val routeRenderer: ArRouteRenderer,
       private val haptics: AndroidHapticManager,
   ) : ViewModel() {
   ```
   (Removes `private` and adds `internal`.)
2. No other change.

Build check.

### Step 3 — Create `Arrow3DGeometry`

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/Arrow3DGeometry.kt`

Define:

```kotlin
package com.VecturAI.android.ar

import com.VecturAI.android.data.ArrowPlacementType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal object Arrow3DGeometry {
    const val FLOATS_PER_VERTEX = 7  // x, y, z, nx, ny, nz, vGradient

    // Hand-authored chevron prism. Forward = +Z, up = +Y. Base extents ~ 0.20 m x 0.25 m, height 0.04 m.
    // Top face is brighter (vGradient = 1.0), bottom face is darker (vGradient = 0.0).
    // Triangulated as flat-shaded faces (each face has duplicated vertices so per-face normals are exact).
    val VERTICES: FloatArray = floatArrayOf(
        // (Authoring note: see docs/handoff/Option_A_Implementation_Plan.md §8.3 for shape rationale.)
        // Layout: top face (4 verts -> 2 tris) at +Y = 0.04, bottom face (4 verts -> 2 tris) at +Y = 0.0,
        // four side quads (rear, front-tip-left, front-tip-right, plus left and right side rectangles).
        // The full vertex array is generated below at runtime by buildVertices() to keep code compact.
    )
    val INDICES: ShortArray = shortArrayOf()  // populated by buildIndices()

    val vertexBuffer: FloatBuffer
    val indexBuffer: ShortBuffer
    val indexCount: Int

    init {
        val (verts, idx) = buildMesh()
        vertexBuffer = ByteBuffer.allocateDirect(verts.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .apply { put(verts); position(0) }
        indexBuffer = ByteBuffer.allocateDirect(idx.size * Short.SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
            .apply { put(idx); position(0) }
        indexCount = idx.size
    }

    private fun buildMesh(): Pair<FloatArray, ShortArray> {
        val w = 0.10f   // half-width
        val h = 0.04f   // height
        val tail = -0.10f
        val baseFront = 0.05f
        val tip = 0.18f

        // Each vertex: x, y, z, nx, ny, nz, vGradient
        fun v(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, g: Float): FloatArray =
            floatArrayOf(x, y, z, nx, ny, nz, g)

        val faces = mutableListOf<FloatArray>()
        val indices = mutableListOf<Short>()
        var nextIndex: Short = 0
        fun addFace(a: FloatArray, b: FloatArray, c: FloatArray) {
            faces += a; faces += b; faces += c
            indices += nextIndex; indices += (nextIndex + 1).toShort(); indices += (nextIndex + 2).toShort()
            nextIndex = (nextIndex + 3).toShort()
        }

        // TOP FACE — bright (vGradient = 1.0)
        // Quad: (-w, h, tail), (w, h, tail), (w, h, baseFront), (-w, h, baseFront)
        // Triangle fan from tail-left.
        addFace(
            v(-w, h, tail, 0f, 1f, 0f, 1f),
            v(w, h, tail, 0f, 1f, 0f, 1f),
            v(w, h, baseFront, 0f, 1f, 0f, 1f),
        )
        addFace(
            v(-w, h, tail, 0f, 1f, 0f, 1f),
            v(w, h, baseFront, 0f, 1f, 0f, 1f),
            v(-w, h, baseFront, 0f, 1f, 0f, 1f),
        )
        // TOP TIP TRIANGLE
        addFace(
            v(-w, h, baseFront, 0f, 1f, 0f, 1f),
            v(w, h, baseFront, 0f, 1f, 0f, 1f),
            v(0f, h, tip, 0f, 1f, 0f, 1f),
        )

        // BOTTOM FACE — dark (vGradient = 0.0). Reverse winding so normal points -Y.
        addFace(
            v(-w, 0f, tail, 0f, -1f, 0f, 0f),
            v(w, 0f, baseFront, 0f, -1f, 0f, 0f),
            v(w, 0f, tail, 0f, -1f, 0f, 0f),
        )
        addFace(
            v(-w, 0f, tail, 0f, -1f, 0f, 0f),
            v(-w, 0f, baseFront, 0f, -1f, 0f, 0f),
            v(w, 0f, baseFront, 0f, -1f, 0f, 0f),
        )
        addFace(
            v(-w, 0f, baseFront, 0f, -1f, 0f, 0f),
            v(0f, 0f, tip, 0f, -1f, 0f, 0f),
            v(w, 0f, baseFront, 0f, -1f, 0f, 0f),
        )

        // SIDES — each side face uses an interpolated vGradient (0 at bottom, 1 at top).
        // Rear (z = tail), normal = -Z
        addFace(
            v(-w, 0f, tail, 0f, 0f, -1f, 0f),
            v(w, 0f, tail, 0f, 0f, -1f, 0f),
            v(w, h, tail, 0f, 0f, -1f, 1f),
        )
        addFace(
            v(-w, 0f, tail, 0f, 0f, -1f, 0f),
            v(w, h, tail, 0f, 0f, -1f, 1f),
            v(-w, h, tail, 0f, 0f, -1f, 1f),
        )
        // Left side (x = -w), z from tail to baseFront, normal = -X
        addFace(
            v(-w, 0f, tail, -1f, 0f, 0f, 0f),
            v(-w, h, baseFront, -1f, 0f, 0f, 1f),
            v(-w, h, tail, -1f, 0f, 0f, 1f),
        )
        addFace(
            v(-w, 0f, tail, -1f, 0f, 0f, 0f),
            v(-w, 0f, baseFront, -1f, 0f, 0f, 0f),
            v(-w, h, baseFront, -1f, 0f, 0f, 1f),
        )
        // Right side (x = +w), z from tail to baseFront, normal = +X
        addFace(
            v(w, 0f, tail, 1f, 0f, 0f, 0f),
            v(w, h, tail, 1f, 0f, 0f, 1f),
            v(w, h, baseFront, 1f, 0f, 0f, 1f),
        )
        addFace(
            v(w, 0f, tail, 1f, 0f, 0f, 0f),
            v(w, h, baseFront, 1f, 0f, 0f, 1f),
            v(w, 0f, baseFront, 1f, 0f, 0f, 0f),
        )
        // Front-left tip face — from (-w, *, baseFront) to (0, *, tip), normal computed and constant
        // n = normalize(cross((0,0,tip)-(-w,0,baseFront), (-w,h,baseFront)-(-w,0,baseFront)))
        // = normalize(cross((w, 0, tip-baseFront), (0, h, 0))) = normalize((-h*(tip-baseFront), 0, w*h))
        run {
            val dx = -h * (tip - baseFront)
            val dz = w * h
            val len = kotlin.math.sqrt(dx * dx + dz * dz)
            val nx = dx / len; val nz = dz / len
            addFace(
                v(-w, 0f, baseFront, nx, 0f, nz, 0f),
                v(-w, h, baseFront, nx, 0f, nz, 1f),
                v(0f, h, tip, nx, 0f, nz, 1f),
            )
            addFace(
                v(-w, 0f, baseFront, nx, 0f, nz, 0f),
                v(0f, h, tip, nx, 0f, nz, 1f),
                v(0f, 0f, tip, nx, 0f, nz, 0f),
            )
        }
        // Front-right tip face — mirrored
        run {
            val dx = h * (tip - baseFront)
            val dz = w * h
            val len = kotlin.math.sqrt(dx * dx + dz * dz)
            val nx = dx / len; val nz = dz / len
            addFace(
                v(w, 0f, baseFront, nx, 0f, nz, 0f),
                v(0f, h, tip, nx, 0f, nz, 1f),
                v(w, h, baseFront, nx, 0f, nz, 1f),
            )
            addFace(
                v(w, 0f, baseFront, nx, 0f, nz, 0f),
                v(0f, 0f, tip, nx, 0f, nz, 0f),
                v(0f, h, tip, nx, 0f, nz, 1f),
            )
        }

        val flat = FloatArray(faces.sumOf { it.size })
        var i = 0
        for (face in faces) {
            for (f in face) flat[i++] = f
        }
        return flat to indices.toShortArray()
    }

    fun colorTopFor(type: ArrowPlacementType): FloatArray = when (type) {
        ArrowPlacementType.FOLLOW -> floatArrayOf(0.239f, 0.725f, 1.000f)         // #3DB9FF
        ArrowPlacementType.TURN_LEFT, ArrowPlacementType.TURN_RIGHT -> floatArrayOf(1.000f, 0.765f, 0.333f)  // #FFC355
        ArrowPlacementType.U_TURN -> floatArrayOf(0.984f, 0.572f, 0.235f)         // #FB923C
        ArrowPlacementType.DESTINATION -> floatArrayOf(0.204f, 0.827f, 0.600f)    // #34D399
    }

    fun colorBottomFor(type: ArrowPlacementType): FloatArray = when (type) {
        ArrowPlacementType.FOLLOW -> floatArrayOf(0.075f, 0.400f, 0.760f)         // #1366C2
        ArrowPlacementType.TURN_LEFT, ArrowPlacementType.TURN_RIGHT -> floatArrayOf(0.710f, 0.443f, 0.020f)  // #B57105
        ArrowPlacementType.U_TURN -> floatArrayOf(0.604f, 0.204f, 0.071f)         // #9A3412
        ArrowPlacementType.DESTINATION -> floatArrayOf(0.016f, 0.471f, 0.341f)    // #047857
    }
}
```

Build check.

### Step 4 — Create `ArArrow3DRenderer`

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArArrow3DRenderer.kt`

```kotlin
package com.VecturAI.android.ar

import android.opengl.GLES20
import android.opengl.Matrix

internal class ArArrow3DRenderer {
    private var arrowProgram = 0
    private var aPosition = 0
    private var aNormal = 0
    private var aGradient = 0
    private var uMVP = 0
    private var uModel = 0
    private var uColorTop = 0
    private var uColorBottom = 0
    private var uAlpha = 0
    private var uLightDir = 0

    private var shadowProgram = 0
    private var sAPosition = 0
    private var sUMVP = 0
    private var sUAlpha = 0

    private val mvp = FloatArray(16)
    private val model = FloatArray(16)
    private val tmp = FloatArray(16)

    private var compiled = false

    fun onSurfaceCreated() {
        arrowProgram = compile(ARROW_VS, ARROW_FS)
        if (arrowProgram != 0) {
            aPosition = GLES20.glGetAttribLocation(arrowProgram, "a_Position")
            aNormal = GLES20.glGetAttribLocation(arrowProgram, "a_Normal")
            aGradient = GLES20.glGetAttribLocation(arrowProgram, "a_Gradient")
            uMVP = GLES20.glGetUniformLocation(arrowProgram, "u_MVP")
            uModel = GLES20.glGetUniformLocation(arrowProgram, "u_Model")
            uColorTop = GLES20.glGetUniformLocation(arrowProgram, "u_ColorTop")
            uColorBottom = GLES20.glGetUniformLocation(arrowProgram, "u_ColorBottom")
            uAlpha = GLES20.glGetUniformLocation(arrowProgram, "u_Alpha")
            uLightDir = GLES20.glGetUniformLocation(arrowProgram, "u_LightDir")
        }
        shadowProgram = compile(SHADOW_VS, SHADOW_FS)
        if (shadowProgram != 0) {
            sAPosition = GLES20.glGetAttribLocation(shadowProgram, "a_Position")
            sUMVP = GLES20.glGetUniformLocation(shadowProgram, "u_MVP")
            sUAlpha = GLES20.glGetUniformLocation(shadowProgram, "u_Alpha")
        }
        compiled = arrowProgram != 0 && shadowProgram != 0
    }

    fun draw(
        arrows: List<ArRouteRenderer.RenderableArrow3D>,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        frameTimeMs: Long,
    ) {
        if (!compiled || arrows.isEmpty()) return

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // Sort back-to-front from camera for transparent blend correctness.
        val cameraPos = floatArrayOf(viewMatrix[12], viewMatrix[13], viewMatrix[14])
        val sorted = arrows.sortedByDescending { a ->
            val dx = a.worldX - cameraPos[0]
            val dy = a.worldY - cameraPos[1]
            val dz = a.worldZ - cameraPos[2]
            dx * dx + dy * dy + dz * dz
        }

        // Pulse for imminent arrow.
        val phase = ((frameTimeMs % 1200L) / 1200.0 * 2.0 * Math.PI).toFloat()
        val pulseScale = 1.0f + 0.04f * kotlin.math.sin(phase)

        // SHADOW PASS
        GLES20.glUseProgram(shadowProgram)
        for (arrow in sorted) {
            buildModelMatrix(arrow, isShadow = true, pulseScale = if (arrow.isImminent) pulseScale else 1f)
            Matrix.multiplyMM(tmp, 0, viewMatrix, 0, model, 0)
            Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, tmp, 0)
            GLES20.glUniformMatrix4fv(sUMVP, 1, false, mvp, 0)
            GLES20.glUniform1f(sUAlpha, 0.35f * arrow.alpha)
            drawShadowDisk()
        }

        // ARROW PASS
        GLES20.glUseProgram(arrowProgram)
        GLES20.glUniform3f(uLightDir, 0.3f, 1.0f, 0.4f)
        Arrow3DGeometry.vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 7 * 4, Arrow3DGeometry.vertexBuffer)

        Arrow3DGeometry.vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 7 * 4, Arrow3DGeometry.vertexBuffer)

        Arrow3DGeometry.vertexBuffer.position(6)
        GLES20.glEnableVertexAttribArray(aGradient)
        GLES20.glVertexAttribPointer(aGradient, 1, GLES20.GL_FLOAT, false, 7 * 4, Arrow3DGeometry.vertexBuffer)

        for (arrow in sorted) {
            buildModelMatrix(arrow, isShadow = false, pulseScale = if (arrow.isImminent) pulseScale else 1f)
            Matrix.multiplyMM(tmp, 0, viewMatrix, 0, model, 0)
            Matrix.multiplyMM(mvp, 0, projectionMatrix, 0, tmp, 0)
            GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
            val top = Arrow3DGeometry.colorTopFor(arrow.type)
            val bot = Arrow3DGeometry.colorBottomFor(arrow.type)
            GLES20.glUniform3f(uColorTop, top[0], top[1], top[2])
            GLES20.glUniform3f(uColorBottom, bot[0], bot[1], bot[2])
            GLES20.glUniform1f(uAlpha, arrow.alpha)
            Arrow3DGeometry.indexBuffer.position(0)
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                Arrow3DGeometry.indexCount,
                GLES20.GL_UNSIGNED_SHORT,
                Arrow3DGeometry.indexBuffer,
            )
        }

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
        GLES20.glDisableVertexAttribArray(aGradient)

        // Restore neutral state for downstream callers.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
    }

    private fun buildModelMatrix(
        arrow: ArRouteRenderer.RenderableArrow3D,
        isShadow: Boolean,
        pulseScale: Float,
    ) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, arrow.worldX, arrow.worldY + if (isShadow) -0.001f else 0f, arrow.worldZ)
        Matrix.rotateM(model, 0, Math.toDegrees(arrow.headingRad.toDouble()).toFloat(), 0f, 1f, 0f)
        val s = arrow.scale * pulseScale
        if (isShadow) {
            Matrix.scaleM(model, 0, s * 1.4f, 1f, s * 1.6f)
        } else {
            Matrix.scaleM(model, 0, s, s, s)
        }
    }

    private fun drawShadowDisk() {
        // 16-segment fan around y=0 in object space (radius 0.20). Built statically once.
        if (!shadowReady) {
            buildShadowDisk()
        }
        shadowVerts.position(0)
        GLES20.glEnableVertexAttribArray(sAPosition)
        GLES20.glVertexAttribPointer(sAPosition, 3, GLES20.GL_FLOAT, false, 0, shadowVerts)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, shadowVertCount)
        GLES20.glDisableVertexAttribArray(sAPosition)
    }

    private var shadowReady = false
    private lateinit var shadowVerts: java.nio.FloatBuffer
    private var shadowVertCount = 0

    private fun buildShadowDisk() {
        val segs = 16
        val r = 0.18f
        val verts = FloatArray((segs + 2) * 3)
        verts[0] = 0f; verts[1] = 0f; verts[2] = 0f
        for (i in 0..segs) {
            val a = (i.toDouble() / segs) * 2.0 * Math.PI
            verts[3 + i * 3] = (r * kotlin.math.cos(a)).toFloat()
            verts[3 + i * 3 + 1] = 0f
            verts[3 + i * 3 + 2] = (r * kotlin.math.sin(a)).toFloat()
        }
        shadowVerts = java.nio.ByteBuffer
            .allocateDirect(verts.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(verts); position(0) }
        shadowVertCount = segs + 2
        shadowReady = true
    }

    private fun compile(vs: String, fs: String): Int {
        val v = loadShader(GLES20.GL_VERTEX_SHADER, vs); if (v == 0) return 0
        val f = loadShader(GLES20.GL_FRAGMENT_SHADER, fs); if (f == 0) return 0
        val p = GLES20.glCreateProgram(); if (p == 0) return 0
        GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f); GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e("Arrow3D", "Program link failed: ${GLES20.glGetProgramInfoLog(p)}")
            GLES20.glDeleteProgram(p); return 0
        }
        return p
    }

    private fun loadShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        val status = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e("Arrow3D", "Shader compile failed: ${GLES20.glGetShaderInfoLog(s)}")
            GLES20.glDeleteShader(s); return 0
        }
        return s
    }

    companion object {
        private const val ARROW_VS = """
            uniform mat4 u_MVP;
            uniform mat4 u_Model;
            attribute vec4 a_Position;
            attribute vec3 a_Normal;
            attribute float a_Gradient;
            varying vec3 v_WorldNormal;
            varying float v_Gradient;
            void main() {
                gl_Position = u_MVP * a_Position;
                v_WorldNormal = mat3(u_Model) * a_Normal;
                v_Gradient = a_Gradient;
            }
        """

        private const val ARROW_FS = """
            precision mediump float;
            uniform vec3 u_ColorTop;
            uniform vec3 u_ColorBottom;
            uniform float u_Alpha;
            uniform vec3 u_LightDir;
            varying vec3 v_WorldNormal;
            varying float v_Gradient;
            void main() {
                vec3 base = mix(u_ColorBottom, u_ColorTop, v_Gradient);
                vec3 N = normalize(v_WorldNormal);
                vec3 L = normalize(u_LightDir);
                float diff = max(0.0, dot(N, L));
                vec3 lit = base * (0.35 + 0.65 * diff);
                // cheap rim
                float rim = pow(1.0 - max(0.0, N.y), 2.0);
                lit += rim * 0.18 * u_ColorTop;
                gl_FragColor = vec4(lit, u_Alpha);
            }
        """

        private const val SHADOW_VS = """
            uniform mat4 u_MVP;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MVP * a_Position;
            }
        """

        private const val SHADOW_FS = """
            precision mediump float;
            uniform float u_Alpha;
            void main() {
                gl_FragColor = vec4(0.0, 0.0, 0.0, u_Alpha);
            }
        """
    }
}
```

Build check.

### Step 5 — Wire the renderer into the GL pipeline

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt`

1. Add constructor parameter:
   ```kotlin
   class UnifiedArRenderer(
       private val activity: android.app.Activity,
       private val unifiedSession: UnifiedArSession,
       private val routeRenderer: ArRouteRenderer,                  // NEW
       private val onTextureCreated: (Int) -> Unit,
       private val onFrame: (Frame, Int, Int, Int) -> Unit,
       private val onFatalFailure: (Throwable) -> Unit,
   ) : GLSurfaceView.Renderer {
   ```
2. Add field: `private val arrow3D = ArArrow3DRenderer()`.
3. In `onSurfaceCreated`, after the existing `onTextureCreated(cameraTextureId)` call, add:
   ```kotlin
   arrow3D.onSurfaceCreated()
   ```
4. In `onDrawFrame`, replace the body of the `if (frame.timestamp != 0L) { ... }` block with:
   ```kotlin
   if (frame.timestamp != 0L) {
       drawCameraBackground(frame)

       val view = FloatArray(16)
       val proj = FloatArray(16)
       frame.camera.getViewMatrix(view, 0)
       frame.camera.getProjectionMatrix(proj, 0, 0.1f, 100f)
       arrow3D.draw(
           arrows = routeRenderer.snapshot(),
           viewMatrix = view,
           projectionMatrix = proj,
           frameTimeMs = System.nanoTime() / 1_000_000L,
       )

       onFrame(frame, width, height, displayRotationDegrees())
   }
   ```

Build check.

### Step 6 — Pass `routeRenderer` from the activity

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArCameraActivity.kt`

In `private val renderer by lazy { ... }`, add the new constructor argument:
```kotlin
UnifiedArRenderer(
    activity = this,
    unifiedSession = unifiedSession,
    routeRenderer = arViewModel.routeRenderer,                    // NEW
    onTextureCreated = { ... },
    onFrame = ::onArFrame,
    onFatalFailure = ::finishWithCameraError,
)
```

Build check.

### Step 7 — Stop computing the 2D projection in the viewmodel

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt`

Replace the body of `onFrame` (currently at line 193-218) with:
```kotlin
fun onFrame(frame: Frame, width: Int, height: Int) {
    markerDetector.processFrame(frame)
    updateTrackingStatus(frame)

    if (_uiState.value.isAligned && !_uiState.value.hasArrived) {
        val now = System.currentTimeMillis()
        if (!_uiState.value.isSimulated && now - lastPoseSampleMs >= 500L) {
            lastPoseSampleMs = now
            sampleCameraPose(frame)
        }
    }
}
```

(Remove the `lastProjectedUpdateMs` block and the `_uiState.update { it.copy(projectedArrows = projected) }` call. Leave `lastProjectedUpdateMs` field declared but unused — it's not worth a separate cleanup commit.)

Optionally remove `projectedArrows` field from `ArNavigationUiState` and from the two existing `it.copy(...)` sites that reference it (in `endNavigation` and `checkArrival`). If you remove the field, also remove its use in `ArNavigationScreen` (already removed in step 8).

Build check.

### Step 8 — Remove the Compose 2D arrow layer

File: `apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt`

1. Delete the call `ProjectedArrowLayer(uiState)` at line 94.
2. Delete the entire `private fun ProjectedArrowLayer(uiState: ArNavigationUiState) { ... }` definition at lines 131-168.
3. Run `grep -n "ProjectedArrowLayer\|colorForArrow\|iconForArrow\|projectedArrows" apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt`.
   - If `colorForArrow` / `iconForArrow` / `projectedArrows` no longer have callers, delete those helpers and their imports.
4. Remove now-unused imports (let the IDE / `./gradlew` decide; the build will succeed either way but warnings are noise).

Build check.

### Step 9 — Smoke test compile

```
./gradlew :apps:androidApp:assembleDebug
```

Must succeed with zero errors. Warnings about unused symbols are tolerated.

---

## 12. Validation steps

### 12.1 Manifest sanity (already in place; confirm only)

```
grep HIGH_SAMPLING_RATE_SENSORS apps/androidApp/src/main/AndroidManifest.xml
```
Must print one line. Do not modify.

### 12.2 Unit/instrumented tests
There are no Android unit tests for AR rendering ([ar_navigation_android.md](../../codebase-docs/features/ar_navigation_android.md): "Related Tests: None"). Skip.

### 12.3 Manual smoke test on device

Required device: Android 12+ ARCore-supported phone where Pokémon GO works. (User has previously confirmed this device.)

1. Build and install:
   ```
   adb uninstall com.VecturAI.android
   ./gradlew :apps:androidApp:installDebug
   ```
2. Watch logs:
   ```
   adb logcat -s native:E ARCore:* Arrow3D:* "[ARDiag]:*" *:S
   ```
3. Launch the app → Home → Start.
4. Grant camera permission, scan the printed entrance poster (or use simulator/emulator path if you're on an emulator — `simulateAlignment()` runs automatically there).
5. Alignment locks, navigation starts.
6. Verify on screen:
   - 3D wedge-shaped arrows appear on the floor along the route.
   - Each arrow points along the route's local forward direction (not always at the camera).
   - Arrows farther away appear smaller (true perspective; not just `scale` modulation).
   - Walking toward an arrow makes it grow naturally; walking past an arrow makes it slide off behind your shoulder (depth-correct).
   - Arrows behind you fade out (existing fade-behind logic still works).
   - The next imminent (TURN_*/U_TURN/DESTINATION) arrow gently pulses.
   - Each arrow casts a soft elliptical shadow on the floor.
7. Verify untouched UI:
   - Top bar shows tracking label and status badge as before.
   - Action card and ETA HUD update with progress as before.
   - Alignment timeout overlay still appears if you hide the poster for 30 s.
   - Arrival screen ("You've reached …") still triggers when you reach the destination.
   - Pressing End ends navigation cleanly.

### 12.4 Performance sanity
With the AR session running, run:
```
adb shell dumpsys gfxinfo com.VecturAI.android | grep -E "Janky|frames rendered|95th percentile"
```
Compare to a pre-change baseline. Janky frame % must not increase by more than 2 percentage points. 95th percentile frame time must stay under 16.7 ms on the Pokémon-GO-capable test device.

### 12.5 Regression sanity
Manually verify the rest of the visitor flow is unaffected:
- Home screen identical.
- QR scan view identical.
- Destination select / route preview identical.
- Error overlays identical.

---

## 13. Expected outputs

After install and run:
- `logcat`: lines `[ARDiag] Session created with '...' (...)` once on session resume. No `Failed to register sensor to queue 0`. No `Arrow3D` shader/program error logs.
- On screen during AR navigation: 3D shaded chevron arrows on the floor, each pointing forward along the route, with shadow blobs and a pulsing imminent-action arrow. No more circle+icon overlays.
- All other UI (top bar, action card, ETA HUD, alignment overlay, arrival, error states) visually identical to before this change.

---

## 14. Edge cases

| Case | Required behavior |
|---|---|
| `routeRenderer.snapshot()` returns empty (no arrows visible) | `ArArrow3DRenderer.draw` early-returns; no GL calls beyond depth state are issued. |
| Compile of arrow shader fails on a device | `ArArrow3DRenderer.onSurfaceCreated` logs and leaves `compiled = false`; `draw` becomes a no-op. The fallback is "no arrows on screen" — non-fatal, AR session still runs. (No crash.) |
| ARCore frame timestamp = 0 | Existing guard in `UnifiedArRenderer.onDrawFrame` already skips both camera draw and arrow draw. |
| Activity goes to background mid-frame | `glSurfaceView.onPause()` halts the GL thread; `unifiedSession.onActivityPause()` pauses ARCore. No GL calls happen on the paused thread. |
| Marker detection fires twice (re-alignment) | `routeRenderer.placeAllArrows(...)` is called again in `handleMarkerDetected`; `snapshotRef` is cleared and re-published. Renderer reads the new snapshot the next frame. |
| User reaches destination | `checkArrival` calls `routeRenderer.hideAllArrows()` which sets the snapshot to empty. Renderer draws nothing. |
| Heading singularity (forward vector ~ 0) | If `transformDirectionToAR` returns a near-zero vector, `atan2` returns 0; arrow points along world +X. Acceptable for that one frame. To bulletproof, in step 1's heading computation, fall back to the previous frame's heading when the magnitude is below `1e-3`. (Tracked as a follow-up; not blocking.) |
| Emulator path (`isEmulator = true`) | `GLSurfaceView` is not created at all (per `ArCameraContent`). `UnifiedArRenderer` and `ArArrow3DRenderer` never get their `onSurfaceCreated` called. `simulateAlignment()` still drives the viewmodel state. The 2D Compose layer being gone means: emulator users will not see any arrow visualization — only the action card / ETA / alignment overlay. This is acceptable for a demo build but flag in `Option_A_Verification_Notes.md`. |
| Device privacy mic toggle off | Already handled by the prior sensor permission fix; not in scope. |

### Emulator regression mitigation (optional)

If "no arrows in emulator" is unacceptable, add a debug-only `Surface` overlay in `ArNavigationScreen.kt` that shows a small "Demo arrows: GL renderer not active in emulator" pill when `isEmulator && uiState.isAligned`. This is opt-in; do not implement unless explicitly requested.

---

## 15. Risks

| Risk | Mitigation |
|---|---|
| Depth buffer not requested by `GLSurfaceView` → arrows render with no depth ordering. | `GLSurfaceView` requests an EGL depth buffer by default (via `setEGLConfigChooser` defaults). If a test device doesn't provide one, force it: `glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)` in `ArCameraActivity.factory`. Add only if validation step 12.3 shows arrows rendering through each other. |
| Heading sign error (atan2 conventions differ) | Verify on first device run. If arrows point backward, negate `headingRad` (one-line fix). |
| Camera background draws after arrows due to ordering bug | Existing code guarantees `drawCameraBackground` precedes `arrow3D.draw`. Verified by step 11 step 5 placement. |
| Slow shader compile on first session resume causes a visible hitch | `onSurfaceCreated` runs once at GL surface init, well before the user reaches AR navigation. No mitigation needed. |
| `glDrawElements` with `GL_UNSIGNED_SHORT` capacity exceeded if mesh grows | Mesh has < 100 vertices. Plenty of headroom. |
| AR alignment math change (`transformDirectionToAR` semantics) breaks heading | Validate by visual inspection — arrows should reliably point along the corridor in both Mutfak and Salon test routes. |
| Non-power-of-two depth buffer issues on legacy GPUs | Not applicable for OpenGL ES 2.0 depth attachments. |
| `lastProjectedUpdateMs` + `projectedArrows` left dangling becomes confusing for future readers | Either delete cleanly in step 7 or note in `Option_A_Verification_Notes.md` for follow-up cleanup. |

---

## 16. Rollback plan

If the new visuals regress on real hardware, revert by reverting these files in this order (each step compiles standalone):

1. `apps/androidApp/src/main/kotlin/com/VecturAI/android/ui/ArNavigationScreen.kt` — restore the `ProjectedArrowLayer` call site and definition.
2. `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/AndroidArNavigationViewModel.kt` — restore `onFrame` body that updates `projectedArrows`.
3. `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArCameraActivity.kt` — drop the `routeRenderer = arViewModel.routeRenderer` argument.
4. `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/UnifiedArRenderer.kt` — drop the constructor param, the `arrow3D` field, the `onSurfaceCreated` call, and the new draw block in `onDrawFrame`.
5. `apps/androidApp/src/main/kotlin/com/VecturAI/android/ar/ArRouteRenderer.kt` — remove `RenderableArrow3D`, `snapshotRef`, `snapshot()`, and the publication code in `updateVisibility/clearArrows/hideAllArrows/placeAllArrows`. Restore `routeRenderer` to `private val` in the viewmodel.
6. Delete `Arrow3DGeometry.kt` and `ArArrow3DRenderer.kt`.

Use `git revert` on the implementation commits in reverse order. The fix is fully isolated to these files; no shared/iOS/admin/preprocessor changes.

---

## 17. Acceptance criteria

The implementation is accepted if and only if **every** statement below holds:

1. `./gradlew :apps:androidApp:assembleDebug` succeeds with no errors.
2. The app installs and launches on the previously-validated ARCore-capable device.
3. Camera permission flow, QR scan, entrance confirmation, destination select, and route preview screens are visually unchanged.
4. After alignment, navigation arrows render as 3D shaded wedge meshes anchored on the floor, each pointing along the route forward direction.
5. Each arrow casts a soft elliptical shadow on the floor.
6. Walking through the route shows arrows growing/shrinking with true perspective, fading out behind the user, and the imminent action arrow pulsing.
7. The next-action card, ETA HUD, tracking-status badge, alignment-timed-out card, and arrival sheet all behave exactly as before.
8. Logcat shows no new error tags, no shader/program compile errors, no `Failed to register sensor to queue` (regression check).
9. Janky-frame percentage from `dumpsys gfxinfo` does not regress by more than 2 percentage points compared to the pre-change baseline.
10. End Route returns the user to Home cleanly with the AR session torn down.

---

## 18. Handoff notes for the executing agent

- Do not run `gh`, `git push`, `git commit`, or any GitHub CLI command. The user must approve and stage commits themselves.
- Do not modify the `iosApp`, `shared`, `tools`, `gradle/libs.versions.toml`, or `AndroidManifest.xml`.
- After each numbered step in §11, run the build and report only the diff and a one-line status.
- If a build fails, stop, report the exact compiler error and the file/line, and ask the user before guessing.
- Do not create new ADRs as part of this work — once the change ships and the user requests it, a follow-up ADR ("ADR-034: Native 3D arrow rendering pipeline") can be drafted separately.
- Do not delete `ArRouteRenderer.projectVisibleArrows` or `ProjectedArrow` in this change — that is a follow-up cleanup once the new path is proven.
- Keep all code comments minimal: only add a comment when a magic constant has a non-obvious origin (e.g., the chevron dimensions in `Arrow3DGeometry`).

---

End of plan.
