# 3D Arrow Rendering Pipeline — Polish Plan

This document is a follow-on to `fix.md` and `AUDIT_REPORT.md`. It targets the residual bugs, GPU/resource leaks, and polish items found in the **current** state of the arrow pipeline. It is written for another agent: each item lists the exact file, line range, the symptom, and the concrete code change to apply.

> **Revision note:** This document was updated after a cross-review pass. Items C4, C6, and L7 were broadened with refinements; new items C10-C13, L12-L13, R8-R10, and an Improvements section (I1) were appended. Cross-review claims that duplicated existing items (overshoot/flip, material churn, etc.) are noted in the appendix.

Files in scope:
- `app/src/main/java/com/example/vecturai/ar/ArrowRenderer.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt`
- `app/src/main/java/com/example/vecturai/ar/Relocalizer.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt`
- `app/src/main/java/com/example/vecturai/ui/ArAssetUtils.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt`
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`

Conventions:
- ARCore + Filament/SceneView are right-handed, Y-up; camera looks toward −Z; arrow.glb mesh extends along **+Z** (verified: `POSITION min=[-0.14,-0.07,0.0], max=[0.14,0.07,0.92]`).
- `dev.romainguy.kotlin.math.Quaternion.fromAxisAngle` takes the angle in **degrees**.
- "Session space" = AR world coordinates as exposed by ARCore.

---

## Critical (visible "sloppiness")

### C1 — Arrow flips backward in the 1.2 m – 1.5 m dead zone
**Where:** `ArrowRenderer.kt:13`, `ArrowRenderer.kt:22-24`.
**Symptom:** While `1.2 m < distance(camera, waypoint) < 1.5 m`, the waypoint is closer than the arrow position (`ARROW_FORWARD_OFFSET_M = 1.5`), so `yawDegreesToward(arrowPos, waypoint)` returns ~180° and the arrow rotates to point at the user just before the advance threshold fires.
**Fix (preferred):** clamp the arrow's forward offset so it can never sit past the waypoint horizontally. Replace `floatingArrowPose`:
```kotlin
fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose? {
    val cameraVec = cameraPose.translationVec()
    val toTarget = horizontalDistanceMeters(cameraVec, target)
    val offset = minOf(ARROW_FORWARD_OFFSET_M, toTarget * 0.9f)
    val position = positionInFrontOfCamera(cameraPose, offset)
    val yawDegrees = yawDegreesToward(position, target) ?: return null
    return ArrowPose(
        position = position,
        yawDegrees = yawDegrees,
        userDistanceToTargetMeters = toTarget
    )
}
```
**Alternative:** simply set `WAYPOINT_ADVANCE_DISTANCE_M = ARROW_FORWARD_OFFSET_M` (1.5 m). Cheaper but advances earlier than the user expects.
**Verify:** walk to ~1.3 m from a waypoint → arrow keeps pointing forward, never spins.

---

### C2 — Per-node `MaterialInstance` and sphere mesh re-allocate every frame
**Where:** `NavigationScreen.kt:163-178` (sphere node block in `nodePoses.forEach`).
**Symptom:** `confidence` is a continuous float (`exp(-d/8)·exp(-age/60)` from `NavigationViewModel.kt:561-564`); `remember(materialLoader, confidence)` re-keys per frame, calling `materialLoader.createColorInstance(...)` and dropping the old `MaterialInstance` without `engine.destroyMaterialInstance(it)`. The `radius = 0.07 + 0.03*confidence` parameter also forces sphere geometry rebuilds. With N nodes at 60 fps that's `N×60` MaterialInstances/sec leaked.
**Fix:** create one `MaterialInstance` per node id (stable across frames), update its parameters each frame, and remove `confidence` from the radius. Replace the `nodePoses.forEach { … }` block with:
```kotlin
nodePoses.forEach { nodePose ->
    key(nodePose.nodeId) {
        val confidence = nodePose.confidence.coerceIn(0f, 1f)
        val material = remember(materialLoader, nodePose.nodeId) {
            materialLoader.createColorInstance(confidenceColor(confidence), roughness = 0.6f)
        }
        DisposableEffect(material) {
            onDispose { material.destroy() }
        }
        // Update color/roughness each frame without reallocating.
        SideEffect {
            material.setParameter("baseColorFactor", confidenceColor(confidence))
            material.setParameter("roughnessFactor", 0.75f - 0.25f * confidence)
        }
        PoseNode(pose = nodePose.pose) {
            SphereNode(
                radius = 0.085f, // fixed; do not modulate per frame
                center = Position(y = 0.08f),
                materialInstance = material
            )
        }
    }
}
```
Notes:
- `MaterialInstance.setParameter("baseColorFactor", color)` requires the underlying material to expose `baseColorFactor`. SceneView's `createColorInstance` builds a PBR material with this parameter; verify the parameter names against the SceneView 4.0.1 source if `setParameter` no-ops. If the API differs, fall back to **quantizing** confidence to 4 buckets and keying `remember` on the bucket so allocations occur at most 4 times.
- `confidenceColor` returns `androidx.compose.ui.graphics.Color`; `setParameter` typically wants `Float, Float, Float, Float`. Use `material.setParameter("baseColorFactor", c.red, c.green, c.blue, c.alpha)` if the overload exists.

**Verify:** stand still 30 s with several unresolved nodes → no measurable native heap or Filament resource growth (Android Studio Memory Profiler / `adb shell dumpsys meminfo`).

---

### C3 — Resolved anchors are smoothed at 4 Hz, lagging ARCore's authoritative pose
**Where:** `NavigationViewModel.kt:510-526` (`refreshDisplayedNodePoses`).
**Symptom:** For resolved anchors, `targetPose = livePose` (anchor.pose, fresh per frame), but the lerp catch-up rate (`DISPLAY_POSE_CATCHUP_HZ = 4`) is the same as for global-fit changes. Visible spheres and the waypoint pose used for arrow yaw trail truth by ~250 ms.
**Fix:** snap to live pose; only lerp when the pose is estimated. Replace the inner block:
```kotlin
val estimates = graph.nodes.map { node ->
    val livePose = resolvedAnchors[node.id]
        ?.takeIf { it.trackingState == TrackingState.TRACKING }
        ?.pose
    val targetPose = livePose ?: estimateSessionPose(transform, node)
    val displayedPose = if (livePose != null) {
        targetPose
    } else {
        displayedNodePoses[node.id]?.let { previousPose ->
            if (alpha >= 1f) targetPose else lerpPose(previousPose, targetPose, alpha)
        } ?: targetPose
    }
    displayedNodePoses[node.id] = displayedPose

    SessionNodePose(
        nodeId = node.id,
        label = node.label,
        pose = displayedPose,
        isResolved = livePose != null,
        confidence = computeConfidence(node, livePose)
    )
}
```
**Verify:** look at a resolved sphere while moving slowly — it should track ARCore's anchor pose without visible drag.

---

### C4 — Smoothed display poses are used as truth for ALL navigation logic, not just arrow yaw
**Where:** `NavigationViewModel.kt:691-692` (arrow yaw); `NavigationViewModel.kt:665, 681-684` (advance threshold and projection); `NavigationViewModel.kt:706` (`maybeReroute` projection); `NavigationViewModel.kt:837-842` (`distanceToDestination`). All consume `state.nodePoses[*].pose`, which is the 4 Hz–smoothed `displayedPose`.
**Symptom:** The 4 Hz display lerp + 8 Hz yaw lerp cascades into one slow filter for arrow yaw — but the same lagged poses also drive (a) waypoint advancement (you walk past a waypoint and don't advance for ~250 ms because the smoothed pose hasn't caught up), (b) `projectToPath` perpendicular distance (so reroute decisions trigger off lagged geometry), and (c) the destination-distance readout. After a relocalization step, all of these stagger. This is the root cause of "laggy or wrong arrow behavior after relocalization changes."
**Fix:** keep the smoothed `displayedPose` for **rendering only** (sphere markers and visible arrow position); use the **target/raw guidance pose** for distance, advancement, projection, reroute, and arrow yaw computation. Add a helper and use it everywhere navigation logic consumes a node pose.

In `NavigationViewModel`:
```kotlin
private fun guidancePose(node: MapNode, transform: Pose): Pose {
    val live = resolvedAnchors[node.id]
        ?.takeIf { it.trackingState == TrackingState.TRACKING }
        ?.pose
    return live ?: estimateSessionPose(transform, node)
}
```

In `updateNavigationProgress`, replace lines 681-691 with:
```kotlin
val transform = graphToSessionPose ?: return
val waypoint = state.currentWaypoint ?: return
val targetWaypointPose = guidancePose(waypoint, transform)
val cameraPosition = cameraPose.translationVec()
val waypointPosition = targetWaypointPose.translationVec()
val distance = horizontalDistanceMeters(cameraPosition, waypointPosition)

if (distance <= ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M) {
    advanceFromWaypoint(state)
    return
}

val arrowPose = ArrowRenderer.floatingArrowPose(cameraPose, waypointPosition)
    ?.let { it.copy(yawDegrees = smoothYawDegrees(it.yawDegrees)) }
```

Change `projectToPath` to take a transform and compute guidance positions itself:
```kotlin
private fun projectToPath(state: NavigationUiState, camHorizontal: Vec3): PathProjection? {
    val transform = graphToSessionPose ?: return null
    if (state.path.size < 2) return null
    val poses = state.path.map { node ->
        guidancePose(node, transform).translationVec().horizontal()
    }
    // …rest unchanged…
}
```

Update `distanceToDestination` similarly:
```kotlin
private fun distanceToDestination(cameraPose: Pose, state: NavigationUiState): Float? {
    val transform = graphToSessionPose ?: return null
    val destination = state.path.lastOrNull() ?: return null
    val target = guidancePose(destination, transform).translationVec()
    return horizontalDistanceMeters(cameraPose.translationVec(), target)
}
```

The display-pose smoothing then governs only the sphere markers; the arrow, distance text, projection, advance, and reroute all see the unlagged truth.

**Verify:** trigger a relocalization (e.g., resolve a new anchor by walking past it). The arrow should re-aim within one frame and the next/destination distances should update without a 250 ms lag.

---

### C5 — `recomputeGraphToSession` runs on every camera frame
**Where:** `NavigationViewModel.kt:222-225`, body at `NavigationViewModel.kt:469-499`.
**Symptom:** ~60 fits/sec (with `repeat(2)` outlier rejection inside, ~180 inner passes). Anchor poses don't materially change every 16 ms; the constant refit injects noise into displayed estimates.
**Fix:** gate the refit. Add a min interval and a "live anchor moved" check.

Add companion constants:
```kotlin
private const val GRAPH_FIT_MIN_INTERVAL_MS = 250L
private const val GRAPH_FIT_LIVE_ANCHOR_MOVE_M = 0.02f
```
Add fields:
```kotlin
private var lastFitAtMs = 0L
private val lastFitAnchorPositions = mutableMapOf<String, Vec3>()
```
Replace the call site at `NavigationViewModel.kt:222-225` with:
```kotlin
if (trackingState == TrackingState.TRACKING) {
    val now = System.currentTimeMillis()
    val anchorsMoved = resolvedAnchors.any { (id, anchor) ->
        if (anchor.trackingState != TrackingState.TRACKING) return@any false
        val previous = lastFitAnchorPositions[id] ?: return@any true
        distanceMeters(anchor.pose.translationVec(), previous) > GRAPH_FIT_LIVE_ANCHOR_MOVE_M
    }
    val due = now - lastFitAtMs >= GRAPH_FIT_MIN_INTERVAL_MS
    val changed = if (due || anchorsMoved) {
        recomputeGraphToSession().also { lastFitAtMs = now }
    } else false
    if (changed || due || anchorsMoved) refreshDisplayedNodePoses()
}
```
At the end of `recomputeGraphToSession` (after `graphToSessionPose = fit`):
```kotlin
lastFitAnchorPositions.clear()
resolvedAnchors.forEach { (id, anchor) ->
    if (anchor.trackingState == TrackingState.TRACKING) {
        lastFitAnchorPositions[id] = anchor.pose.translationVec()
    }
}
```
Reset both fields in `resetLocalizationState()`:
```kotlin
lastFitAtMs = 0L
lastFitAnchorPositions.clear()
```
**Verify:** Profile `recomputeGraphToSession` calls — should drop from ~60/s to ~4/s when anchors are stable.

---

### C6 — `Relocalizer.averageDirectRotation` is not Procrustes — sub-optimal alignment with > 1 anchor
**Where:** `Relocalizer.kt:13-50` and `Relocalizer.kt:63-90`.
**Symptom:** The current rotation estimate averages per-anchor solo transforms via hemisphere-aligned linear quaternion sum, then computes translation from weighted centroids. This does not minimize `Σ wᵢ‖R·gᵢ + t − sᵢ‖²`. With anchors offset along a hallway, estimated nodes are systematically biased.
**Fix:** replace `fitGraphToSession` with weighted Kabsch/Umeyama (rotation-only). Keep `Correspondence`, `rejectOutliers`, and the current API surface.

Drop-in replacement:
```kotlin
package com.example.vecturai.ar

import com.google.ar.core.Pose
import kotlin.math.sqrt

data class Correspondence(
    val graphPose: Pose,
    val sessionPose: Pose,
    val weight: Float = 1f
)

object Relocalizer {
    fun fitGraphToSession(correspondences: List<Correspondence>): Pose? {
        if (correspondences.isEmpty()) return null
        if (correspondences.size == 1) {
            val c = correspondences.first()
            return c.sessionPose.compose(c.graphPose.inverse())
        }

        // Sanitize weights.
        val weights = FloatArray(correspondences.size) { i ->
            correspondences[i].weight.coerceAtLeast(0f)
        }
        val totalWeight = weights.sum().coerceAtLeast(EPSILON)

        // Centroids.
        var gx = 0f; var gy = 0f; var gz = 0f
        var sx = 0f; var sy = 0f; var sz = 0f
        correspondences.forEachIndexed { i, c ->
            val w = weights[i] / totalWeight
            gx += c.graphPose.tx() * w; gy += c.graphPose.ty() * w; gz += c.graphPose.tz() * w
            sx += c.sessionPose.tx() * w; sy += c.sessionPose.ty() * w; sz += c.sessionPose.tz() * w
        }

        // Cross-covariance H = Σ wᵢ (gᵢ − ḡ)(sᵢ − s̄)ᵀ.
        val h = DoubleArray(9)
        correspondences.forEachIndexed { i, c ->
            val w = weights[i] / totalWeight
            val px = c.graphPose.tx() - gx
            val py = c.graphPose.ty() - gy
            val pz = c.graphPose.tz() - gz
            val qx = c.sessionPose.tx() - sx
            val qy = c.sessionPose.ty() - sy
            val qz = c.sessionPose.tz() - sz
            h[0] += (px * qx).toDouble() * w; h[1] += (px * qy).toDouble() * w; h[2] += (px * qz).toDouble() * w
            h[3] += (py * qx).toDouble() * w; h[4] += (py * qy).toDouble() * w; h[5] += (py * qz).toDouble() * w
            h[6] += (pz * qx).toDouble() * w; h[7] += (pz * qy).toDouble() * w; h[8] += (pz * qz).toDouble() * w
        }

        val r = kabschRotationFromH(h) ?: return null
        // R is 3x3 row-major; convert to quaternion.
        val rotation = rotationMatrixToQuaternion(r)

        // t = s̄ − R · ḡ
        val rgx = (r[0] * gx + r[1] * gy + r[2] * gz)
        val rgy = (r[3] * gx + r[4] * gy + r[5] * gz)
        val rgz = (r[6] * gx + r[7] * gy + r[8] * gz)
        val translation = floatArrayOf(sx - rgx, sy - rgy, sz - rgz)
        return Pose(translation, rotation)
    }

    fun rejectOutliers(
        correspondences: List<Correspondence>,
        fit: Pose,
        maxResidualMeters: Float = 1.0f
    ): List<Correspondence> = correspondences.filter { c ->
        val predicted = fit.compose(c.graphPose)
        distanceMeters(predicted, c.sessionPose) <= maxResidualMeters
    }

    /**
     * 3x3 SVD via Jacobi on Hᵀ·H is overkill for our N≤8 case. Use the closed-form
     * via the polar decomposition of H using one-sided Jacobi if you need full robustness;
     * for indoor anchor counts a 32-iteration symmetric Jacobi on Hᵀ·H is plenty.
     *
     * h is row-major 3x3.
     * Returns rotation R minimizing Σ wᵢ‖R·gᵢ − sᵢ‖² as row-major 3x3, or null on degeneracy.
     */
    private fun kabschRotationFromH(h: DoubleArray): FloatArray? {
        // SVD: H = U Σ Vᵀ → R = V · diag(1, 1, det(V·Uᵀ)) · Uᵀ.
        val u = DoubleArray(9); val v = DoubleArray(9); val s = DoubleArray(3)
        if (!svd3x3(h, u, s, v)) return null
        val det = det3(matMul3(v, transpose3(u)))
        val d = if (det < 0) -1.0 else 1.0
        // R = V · diag(1,1,d) · Uᵀ
        val mid = doubleArrayOf(1.0,0.0,0.0, 0.0,1.0,0.0, 0.0,0.0,d)
        val r = matMul3(matMul3(v, mid), transpose3(u))
        return FloatArray(9) { r[it].toFloat() }
    }

    private fun rotationMatrixToQuaternion(r: FloatArray): FloatArray {
        // Shepperd's method, returns [qx, qy, qz, qw].
        val m00 = r[0]; val m01 = r[1]; val m02 = r[2]
        val m10 = r[3]; val m11 = r[4]; val m12 = r[5]
        val m20 = r[6]; val m21 = r[7]; val m22 = r[8]
        val trace = m00 + m11 + m22
        return if (trace > 0f) {
            val s = 0.5f / sqrt(trace + 1f)
            floatArrayOf((m21 - m12) * s, (m02 - m20) * s, (m10 - m01) * s, 0.25f / s)
        } else if (m00 > m11 && m00 > m22) {
            val s = 2f * sqrt(1f + m00 - m11 - m22)
            floatArrayOf(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
        } else if (m11 > m22) {
            val s = 2f * sqrt(1f + m11 - m00 - m22)
            floatArrayOf((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
        } else {
            val s = 2f * sqrt(1f + m22 - m00 - m11)
            floatArrayOf((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
        }
    }

    private fun matMul3(a: DoubleArray, b: DoubleArray): DoubleArray = DoubleArray(9).also { c ->
        for (i in 0..2) for (j in 0..2) {
            var sum = 0.0
            for (k in 0..2) sum += a[i*3+k] * b[k*3+j]
            c[i*3+j] = sum
        }
    }
    private fun transpose3(a: DoubleArray): DoubleArray = doubleArrayOf(
        a[0], a[3], a[6],
        a[1], a[4], a[7],
        a[2], a[5], a[8]
    )
    private fun det3(a: DoubleArray): Double =
        a[0]*(a[4]*a[8]-a[5]*a[7]) - a[1]*(a[3]*a[8]-a[5]*a[6]) + a[2]*(a[3]*a[7]-a[4]*a[6])

    /**
     * Symmetric Jacobi on AᵀA → eigenvectors give V; U = AV·diag(1/σ).
     * Returns true on success, false if A is rank-deficient enough to be unreliable.
     */
    private fun svd3x3(a: DoubleArray, u: DoubleArray, sigma: DoubleArray, v: DoubleArray): Boolean {
        // Build AᵀA (symmetric).
        val ata = matMul3(transpose3(a), a)
        // Eigen-decomposition via Jacobi rotations on 3x3 symmetric.
        val w = doubleArrayOf(ata[0], ata[4], ata[8])      // diagonal
        val off = doubleArrayOf(ata[1], ata[2], ata[5])    // (0,1), (0,2), (1,2)
        val vMat = doubleArrayOf(1.0,0.0,0.0, 0.0,1.0,0.0, 0.0,0.0,1.0)
        repeat(32) {
            val (p, q) = pickLargestOff(off)
            if (kotlin.math.abs(off[indexOf(p,q)]) < 1e-12) return@repeat
            jacobiRotate(w, off, vMat, p, q)
        }
        // sigma = sqrt(eigenvalues), descending.
        val ev = intArrayOf(0,1,2).sortedByDescending { w[it] }
        for (i in 0..2) sigma[i] = sqrt(kotlin.math.max(0.0, w[ev[i]]))
        // V columns reordered by ev.
        for (i in 0..2) for (j in 0..2) v[i*3+j] = vMat[i*3+ev[j]]
        // U = A V Σ⁻¹, column by column.
        for (j in 0..2) {
            val s = if (sigma[j] > 1e-9) 1.0 / sigma[j] else 0.0
            for (i in 0..2) {
                u[i*3+j] = (a[i*3+0]*v[0*3+j] + a[i*3+1]*v[1*3+j] + a[i*3+2]*v[2*3+j]) * s
            }
        }
        return sigma[0] > 1e-6
    }
    private fun indexOf(p: Int, q: Int): Int = when {
        p == 0 && q == 1 -> 0
        p == 0 && q == 2 -> 1
        else -> 2
    }
    private fun pickLargestOff(off: DoubleArray): Pair<Int,Int> {
        val a0 = kotlin.math.abs(off[0]); val a1 = kotlin.math.abs(off[1]); val a2 = kotlin.math.abs(off[2])
        return when {
            a0 >= a1 && a0 >= a2 -> 0 to 1
            a1 >= a2 -> 0 to 2
            else -> 1 to 2
        }
    }
    private fun jacobiRotate(w: DoubleArray, off: DoubleArray, v: DoubleArray, p: Int, q: Int) {
        val idx = indexOf(p, q)
        val app = w[p]; val aqq = w[q]; val apq = off[idx]
        if (kotlin.math.abs(apq) < 1e-15) return
        val theta = (aqq - app) / (2.0 * apq)
        val t = if (theta >= 0) 1.0 / (theta + sqrt(1.0 + theta * theta))
                 else 1.0 / (theta - sqrt(1.0 + theta * theta))
        val c = 1.0 / sqrt(1.0 + t * t); val s = t * c
        w[p] = app - t * apq; w[q] = aqq + t * apq
        off[idx] = 0.0
        // Update remaining off-diagonals.
        val r = 3 - p - q
        val ipr = indexOf(kotlin.math.min(p, r), kotlin.math.max(p, r))
        val iqr = indexOf(kotlin.math.min(q, r), kotlin.math.max(q, r))
        val apr = off[ipr]; val aqr = off[iqr]
        off[ipr] = c * apr - s * aqr
        off[iqr] = s * apr + c * aqr
        // Update V columns p and q.
        for (i in 0..2) {
            val vip = v[i*3+p]; val viq = v[i*3+q]
            v[i*3+p] = c * vip - s * viq
            v[i*3+q] = s * vip + c * viq
        }
    }

    private const val EPSILON = 1e-6f
}
```
**Verify:** With 2 resolved anchors and a third estimated between them, the estimated sphere should sit within ~5 cm of the ground-truth midline rather than drift along the hallway.
**Note:** keep `RelocalizerTest.kt` green (or add tests) — Procrustes is well-defined math, so cases of 2-anchor identity, 2-anchor 90°-rotated, and noisy 4-anchor inputs make good asserts.

**Simpler alternative for indoor-only deployments:** restrict the rotation search to the gravity plane (yaw only) and treat Y as a separate average. Buildings are gravity-aligned and ARCore's anchor pitch/roll is mostly noise relative to graph poses. A planar fit is cheaper and noticeably more robust against bad anchor orientation:

```kotlin
fun fitGraphToSession(correspondences: List<Correspondence>): Pose? {
    if (correspondences.isEmpty()) return null
    if (correspondences.size == 1) {
        val c = correspondences.first()
        return c.sessionPose.compose(c.graphPose.inverse())
    }

    val totalW = correspondences.sumOf { it.weight.coerceAtLeast(0f).toDouble() }
        .toFloat().coerceAtLeast(EPSILON)

    var gx = 0f; var gz = 0f; var gy = 0f
    var sx = 0f; var sz = 0f; var sy = 0f
    correspondences.forEach { c ->
        val w = c.weight.coerceAtLeast(0f) / totalW
        gx += c.graphPose.tx() * w; gz += c.graphPose.tz() * w; gy += c.graphPose.ty() * w
        sx += c.sessionPose.tx() * w; sz += c.sessionPose.tz() * w; sy += c.sessionPose.ty() * w
    }

    // Planar Kabsch in (x, z): minimize Σ wᵢ‖R_y · (gᵢ - ḡ) - (sᵢ - s̄)‖² over yaw θ.
    var sxx = 0f; var sxz = 0f; var szx = 0f; var szz = 0f
    correspondences.forEach { c ->
        val w = c.weight.coerceAtLeast(0f) / totalW
        val px = c.graphPose.tx() - gx; val pz = c.graphPose.tz() - gz
        val qx = c.sessionPose.tx() - sx; val qz = c.sessionPose.tz() - sz
        sxx += px * qx * w; sxz += px * qz * w
        szx += pz * qx * w; szz += pz * qz * w
    }
    // Closed-form yaw: θ = atan2(sxz - szx, sxx + szz). (Standard 2D Procrustes.)
    val theta = kotlin.math.atan2((sxz - szx).toDouble(), (sxx + szz).toDouble()).toFloat()
    val c = kotlin.math.cos(theta); val s = kotlin.math.sin(theta)

    // Translation in plane: t = s̄ - R · ḡ.
    val tx = sx - (c * gx + s * gz)
    val tz = sz - (-s * gx + c * gz)
    val ty = sy - gy   // simple Y average difference

    val rotation = floatArrayOf(0f, kotlin.math.sin(theta * 0.5f), 0f, kotlin.math.cos(theta * 0.5f))
    return Pose(floatArrayOf(tx, ty, tz), rotation)
}
```

Pick the planar variant if you want fewer dependencies (no SVD), and the full Kabsch variant if anchors might be intentionally tilted (e.g., wall-mounted at known non-vertical orientations). For the current demo path, **planar is preferred**.

---

### C7 — `publishNodeEstimates` runs `updateNavigationProgress` with stale camera pose
**Where:** `NavigationViewModel.kt:460-467`.
**Symptom:** Resolves complete asynchronously; the call to `updateNavigationProgress(latestCameraPose)` runs between frames with a `latestCameraPose` that's 16-100 ms old, then `smoothYawDegrees` sees an irregular `now − previousNanos` and produces a one-frame jolt at the next real frame.
**Fix:** drop `updateNavigationProgress` from this path. Replace the body with:
```kotlin
private fun publishNodeEstimates(statusMessage: String) {
    recomputeGraphToSession()
    if (!refreshDisplayedNodePoses(statusMessage)) return
    latestCameraPose?.let { updateCurrentNode(it) }
    // Arrow + waypoint advancement run on the next frame in onSessionUpdated.
}
```

---

### C8 — Procedural fallback arrow has the wrong size and origin convention vs. arrow.glb
**Where:** `NavigationScreen.kt:196-205` (procedural fallback) vs. `arrow.glb` mesh bbox `z∈[0, 0.92]` and `ARROW_MODEL_SCALE = 0.45f` → 0.41 m.
**Symptom:** Procedural is 0.64 m long with a tail at z=−0.05, GLB is 0.41 m long with a tail at z=0. Same yaw produces visibly different arrows, and procedural sticks 5 cm behind the arrow's reference point.
**Fix:** redo procedural to match GLB footprint (length 0.42 m, anchored at z=0). Replace the procedural block with:
```kotlin
} else if (arrowMaterial != null) {
    CubeNode(
        size = Size(x = 0.08f, y = 0.05f, z = 0.30f),
        center = Position(z = 0.15f),         // shaft spans z = 0.00 .. 0.30
        materialInstance = arrowMaterial
    )
    SphereNode(
        radius = 0.06f,
        center = Position(z = 0.36f),         // tip spans z = 0.30 .. 0.42
        materialInstance = arrowMaterial
    )
}
```
This also resolves C9 (no Z-fight: cube tip at 0.30, sphere base at 0.30 with identical material — coplanar + same color is visually fine; if a bevel is desired, overlap by 1 mm by setting `sphere center z = 0.35`).

---

### C10 — Synchronous model + asset load on the Compose thread causes a launch hitch
**Where:** `NavigationScreen.kt:134-148`. Both `hasValidGlbAsset(context, "arrow.glb")` and `modelLoader.createModelInstance("arrow.glb")` run inside `remember { … }`, which executes on the Compose composition thread (effectively the main/UI thread). Filament resource creation also touches the GL context.
**Symptom:** Frame stutter at the moment the navigation screen first composes — exactly when the AR camera is initializing. For the current 1.3 KB `arrow.glb` the hitch is small but measurable; the pattern is wrong and will get visibly worse if the asset grows or the device GL context is busy.
**Fix:** load asynchronously into a `MutableState<ModelInstance?>`. Replace lines 134-148 with:
```kotlin
val arrowModelInstance = remember { mutableStateOf<ModelInstance?>(null) }
val arrowMaterial = remember { mutableStateOf<MaterialInstance?>(null) }

LaunchedEffect(modelLoader, materialLoader, context) {
    val valid = withContext(Dispatchers.IO) { hasValidGlbAsset(context, "arrow.glb") }
    val model = if (valid) {
        // createModelInstance must run on the GL/main thread for some SceneView versions.
        // If the API is documented as main-thread-safe, you can keep this on Default.
        runCatching { modelLoader.createModelInstance("arrow.glb") }.getOrNull()
    } else null
    arrowModelInstance.value = model
    arrowMaterial.value = if (model == null) {
        materialLoader.createColorInstance(Color(0xFFFF5722), roughness = 0.45f)
    } else null
}

DisposableEffect(arrowModelInstance.value) {
    onDispose {
        arrowModelInstance.value?.let { runCatching { modelLoader.destroyModelInstance(it) } }
    }
}
DisposableEffect(arrowMaterial.value) {
    onDispose { arrowMaterial.value?.let { runCatching { it.destroy() } } }
}
```
And in the render block, read `arrowModelInstance.value` / `arrowMaterial.value` instead of the locals. Until the model resolves, the arrow simply doesn't render — acceptable for the first ~50 ms.
**Verify:** scroll-test entering the Navigation screen 10× in a row; no jank/dropped frame on entry.
**Note:** confirm `modelLoader.createModelInstance` is safe to call off the main thread in SceneView 4.0.1. If it isn't, do the asset validation on `Dispatchers.IO` and only the `createModelInstance` call back on `Dispatchers.Main` — still avoids the Compose-time block.

---

### C11 — `Pathfinder.smoothPath` drops corner nodes based purely on geometric deviation
**Where:** `Pathfinder.kt:78` (returns `smoothPath(...)`); `Pathfinder.kt:128-134` (`canDropIntermediate`). The drop test checks only `distancePointToSegment(b, a, c) ≤ 0.75 m`, label preservation, and same-floor — it does **not** verify that there is an actual `a→c` edge in the graph or that the chord is collision-free.
**Symptom:** A real graph corner node `b` between `a` and `c` (where the corridor literally bends through `b`) can be dropped if `b`'s perpendicular distance to the `a-c` chord is under 0.75 m. The arrow then aims from `a` straight to `c`, cutting through the wall the corner is in. This produces "the arrow points through walls" exactly when the user needs the corner cue most.
**Fix:** treat the graph as the authority on connectivity. Only drop `b` if there's a direct edge from the previous accepted node to `c`. Add the adjacency check inside `canDropIntermediate`:
```kotlin
private fun canDropIntermediate(a: MapNode, b: MapNode, c: MapNode): Boolean {
    if (!b.label.isNullOrBlank()) return false
    if (a.floor != b.floor || b.floor != c.floor) return false
    val ac = c.point() - a.point()
    if (ac.length() < 1e-3f) return false
    if (!hasDirectEdge(a.id, c.id)) return false  // <— new
    return distancePointToSegment(b.point(), a.point(), c.point()) <= SMOOTHING_MAX_DEVIATION_M
}

private fun hasDirectEdge(fromId: String, toId: String): Boolean {
    val edges = adjacentEdges[fromId].orEmpty()
    return edges.any { it.toNodeId == toId }
}
```
This means smoothing only short-cuts when the graph itself already says "you can walk a→c directly." For a hallway with an explicit `a-c` corridor edge that happens to pass close to a labeled-but-not-named corner, smoothing is preserved; for a true 90° corner with only `a-b` and `b-c` edges, the corner is kept.
**Verify:** add a test in `PathfinderTest.kt` that a U-shaped graph (a-b-c-d with edges a-b, b-c, c-d only) returns `[a,b,c,d]` unchanged regardless of geometry. Existing straight-line smoothing tests should still pass.

---

### C12 — Waypoint advancement is purely horizontal, so floor-change waypoints are skipped instantly
**Where:** `NavigationViewModel.kt:684-689` (`horizontalDistanceMeters` against advance threshold), `NavigationViewModel.kt:734-768` (`projectToPath` over `.horizontal()` projections).
**Symptom:** A stairs/elevator waypoint at `(x, y₂, z)` directly above the user at `(x, y₁, z)` reports `horizontalDistanceMeters = 0`, immediately triggering `advanceFromWaypoint` even though the user has not actually used the stairs. The arrow then jumps to whatever's after the floor transition. Same problem affects `projectToPath`: a vertical leg has zero horizontal length and the projection block at `Pathfinder`/`NavigationViewModel.kt:744-749` (`if (len2 < 1e-4f) continue`) silently skips the segment.
**Fix:** when the path edge to/from the current waypoint is `EdgeKind.STAIRS` or `EdgeKind.ELEVATOR`, require both horizontal proximity **and** floor match. Add a helper:
```kotlin
private fun isVerticalEdgeBetween(graph: MapGraph, a: MapNode?, b: MapNode?): Boolean {
    if (a == null || b == null) return false
    val edge = graph.edges.firstOrNull {
        (it.fromNodeId == a.id && it.toNodeId == b.id) ||
            (it.bidirectional && it.fromNodeId == b.id && it.toNodeId == a.id)
    } ?: return a.floor != b.floor
    return edge.kind == EdgeKind.STAIRS || edge.kind == EdgeKind.ELEVATOR
}
```
In `updateNavigationProgress`, before advancing:
```kotlin
val previousNode = state.path.getOrNull(state.currentWaypointIndex - 1)
val isVertical = isVerticalEdgeBetween(state.graph!!, previousNode, waypoint) ||
                 (previousNode != null && previousNode.floor != waypoint.floor)
val advance = if (isVertical) {
    distance <= ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M &&
        waypoint.floor == inferUserFloor(state, cameraPose)
} else {
    distance <= ArrowRenderer.WAYPOINT_ADVANCE_DISTANCE_M
}
if (advance) {
    advanceFromWaypoint(state)
    return
}
```
`inferUserFloor` can use the closest resolved/estimated node's floor:
```kotlin
private fun inferUserFloor(state: NavigationUiState, cameraPose: Pose): Int {
    val transform = graphToSessionPose ?: return state.currentWaypoint?.floor ?: 0
    val camY = cameraPose.translationVec().y
    return state.graph?.nodes
        ?.minByOrNull { kotlin.math.abs(estimateSessionPose(transform, it).ty() - camY) }
        ?.floor ?: 0
}
```
Also gate `projectToPath` to a single floor's worth of segments — the user can only project onto the path leg they're actually on. Easiest: split the path into per-floor sub-paths and only project against the sub-path whose floor matches `inferUserFloor`.

**Verify:** Build a 2-floor graph with a stairs edge, stand directly under the upper-floor waypoint on floor 1, observe that the route does not auto-advance until you reach the upper floor.

---

### C13 — `updateCurrentNode` over-prefers resolved anchors, breaking route start once `graphToSessionPose` exists
**Where:** `NavigationViewModel.kt:651` — `val candidates = poses.filter { it.isResolved }.ifEmpty { poses }`.
**Symptom:** With one resolved anchor at node A and the user actually closer to estimated node B (computed from the same graph→session fit derived from A), `currentNodeId` is forced to A. `selectDestination` then plans from A→…→D and the user has to walk back to A or follow a redundant prefix. Visible on demos where you resolve at the entrance and pick a destination after walking a few meters in.
**Fix:** once `graphToSessionPose` is established, all estimated poses are derived from it and are usable; treat them on equal footing, optionally weighted by confidence.
```kotlin
private fun updateCurrentNode(cameraPose: Pose) {
    val state = _uiState.value
    val poses = state.nodePoses
    if (poses.isEmpty()) return

    val cameraPosition = cameraPose.translationVec()
    if (state.phase == NavigationPhase.Navigating && state.path.size > 1) {
        projectToPath(state, cameraPosition.horizontal())?.let { projection ->
            val pathIndex = if (projection.segmentT > 0.5f) projection.segmentIndex + 1
                            else projection.segmentIndex
            _uiState.update { it.copy(currentNodeId = state.path[pathIndex.coerceIn(0, state.path.lastIndex)].id) }
            return
        }
    }

    // Once we have a fit, estimated poses are reliable. Pick by confidence-weighted nearest.
    val haveFit = graphToSessionPose != null
    val pool = if (haveFit) poses else poses.filter { it.isResolved }.ifEmpty { poses }
    val closest = pool.minByOrNull { p ->
        val d = horizontalDistanceMeters(cameraPosition, p.pose.translationVec())
        // Penalize low confidence so a 2 m-away resolved anchor still wins over a 1.9 m
        // estimate with confidence 0.1.
        d / (0.5f + 0.5f * p.confidence)
    }
    _uiState.update { it.copy(currentNodeId = closest?.nodeId) }
}
```
**Verify:** resolve one anchor at the entrance, walk 5 m past the next node (B), select a destination. The route should start at B (or the next reasonable node), not at the entrance anchor.

---

## Logic / correctness

### L1 — `displayedPoseBlendAlpha` keeps ratcheting `lastDisplayedPoseUpdateNanos` outside Navigating
**Where:** `NavigationViewModel.kt:579-593`.
**Symptom:** On entering Navigating, `previous` is set to "now" from the prior non-navigating frame, so `elapsedSeconds` is just one frame and `alpha ≈ 5%` for the first navigating frame. Combined with C3, the displayed pose lerps slowly off whatever it was during PickingDestination.
**Fix:** null out the timestamp when not Navigating; the existing first-call branch then snaps cleanly.
```kotlin
private fun displayedPoseBlendAlpha(phase: NavigationPhase): Float {
    if (phase != NavigationPhase.Navigating) {
        lastDisplayedPoseUpdateNanos = null
        return 1f
    }
    val now = System.nanoTime()
    val previous = lastDisplayedPoseUpdateNanos
    lastDisplayedPoseUpdateNanos = now
    val elapsedSeconds = previous?.let {
        ((now - it) / NANOS_PER_SECOND).coerceIn(0f, MAX_BLEND_DELTA_SECONDS)
    } ?: return 1f
    return (1f - exp((-DISPLAY_POSE_CATCHUP_HZ * elapsedSeconds).toDouble()).toFloat())
        .coerceIn(0f, 1f)
}
```

---

### L2 — `resetNavigationSmoothing` does not reset the displayed-pose smoother
**Where:** `NavigationViewModel.kt:964-967`.
**Symptom:** After re-route or destination change, `displayedNodePoses` and `lastDisplayedPoseUpdateNanos` retain stale values; the new waypoint visibly drifts in.
**Fix:**
```kotlin
private fun resetNavigationSmoothing() {
    lastYawUpdateNanos = null
    lastSmoothedYawDegrees = null
    lastDisplayedPoseUpdateNanos = null
    // Don't clear displayedNodePoses — refreshDisplayedNodePoses will overwrite them
    // with target on the next frame because alpha will be 1 (no previous timestamp).
}
```

---

### L3 — `recomputeGraphToSession` returns a "did it change" flag that nobody reads
**Where:** `NavigationViewModel.kt:498`.
**Fix:** consume it in C5's gating logic (already done in the patch above). If C5 is not adopted, change the return type to `Unit`.

---

### L4 — `repeat(2)` outlier rejection iterates even when nothing was pruned
**Where:** `NavigationViewModel.kt:484-490`.
**Fix:** break early when no outliers were rejected.
```kotlin
repeat(2) {
    val pruned = Relocalizer.rejectOutliers(correspondences, fit)
    if (pruned.isEmpty() || pruned.size == correspondences.size) return@repeat
    correspondences = pruned
    fit = Relocalizer.fitGraphToSession(pruned) ?: fit
}
```

---

### L5 — `closestNodeIdToCamera` fallback `?:` is unreachable
**Where:** `NavigationViewModel.kt:770-781`.
**Fix:** simplify and make the fallback meaningful.
```kotlin
private fun closestNodeIdToCamera(state: NavigationUiState, cameraPose: Pose): String? {
    if (state.nodePoses.isEmpty()) return null
    val cameraPosition = cameraPose.translationVec()
    val resolved = state.nodePoses.filter { it.isResolved }
    val candidates = if (resolved.isNotEmpty()) resolved else state.nodePoses
    return candidates.minByOrNull {
        horizontalDistanceMeters(cameraPosition, it.pose.translationVec())
    }?.nodeId
}
```

---

### L6 — `onSessionUpdated` fires 3-5 separate `_uiState.update` per frame
**Where:** `NavigationViewModel.kt:208-240` plus the cascading updates in `refreshDisplayedNodePoses`, `updateCurrentNode`, `updateNavigationProgress`.
**Fix:** batch into a single update at the end of frame. Easiest path with low risk: introduce a per-frame mutable accumulator and apply once.

Add a private data class and helper:
```kotlin
private data class FrameDelta(
    val trackingState: String? = null,
    val arrowPose: ArrowPose? = null,
    val clearArrowPose: Boolean = false,
    val nodePoses: List<SessionNodePose>? = null,
    val resolvedAnchorCount: Int? = null,
    val phase: NavigationPhase? = null,
    val currentNodeId: String? = null,
    val currentWaypointIndex: Int? = null,
    val distanceToNextMeters: Float? = null,
    val distanceToDestinationMeters: Float? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val clearError: Boolean = false,
    val lastResolveError: String? = null,
    val clearLastResolveError: Boolean = false
)

private fun applyFrameDelta(delta: FrameDelta) {
    _uiState.update { current ->
        current.copy(
            trackingState = delta.trackingState ?: current.trackingState,
            arrowPose = if (delta.clearArrowPose) null else delta.arrowPose ?: current.arrowPose,
            nodePoses = delta.nodePoses ?: current.nodePoses,
            resolvedAnchorCount = delta.resolvedAnchorCount ?: current.resolvedAnchorCount,
            phase = delta.phase ?: current.phase,
            currentNodeId = delta.currentNodeId ?: current.currentNodeId,
            currentWaypointIndex = delta.currentWaypointIndex ?: current.currentWaypointIndex,
            distanceToNextMeters = delta.distanceToNextMeters ?: current.distanceToNextMeters,
            distanceToDestinationMeters = delta.distanceToDestinationMeters ?: current.distanceToDestinationMeters,
            statusMessage = delta.statusMessage ?: current.statusMessage,
            errorMessage = if (delta.clearError) null else delta.errorMessage ?: current.errorMessage,
            lastResolveError = if (delta.clearLastResolveError) null else delta.lastResolveError ?: current.lastResolveError
        )
    }
}
```
Then in `onSessionUpdated`, accumulate locally instead of calling `_uiState.update` repeatedly. This is mechanical but invasive — if it's too risky for a polish pass, defer it. The minimal version is to consolidate `onSessionUpdated`'s top three updates (lines 219, 222-225, 230) into one trailing update at the end of the function.

---

### L7 — `Vec3.normalized()`'s `(0, 0, -1)` magic fallback + degenerate horizontal forward when looking up/down
**Where:** `PoseUtils.kt:23-26` (the magic fallback), `PoseUtils.kt:82-91` (`positionInFrontOfCamera`'s `Vec3(forward.x, 0f, forward.z).normalized()`), used by `forwardVec()` at `PoseUtils.kt:31-34`.
**Symptom (two related bugs):**
1. **Session bring-up:** if ARCore delivers a degenerate Z axis during init, `forwardVec` returns `(0, 0, -1)` and the arrow snaps to world −Z.
2. **User looks straight down/up:** when the camera is pitched near vertical, `forward.x` and `forward.z` are both near zero. Normalizing a near-zero horizontal vector amplifies float noise — the arrow rapidly jitters horizontally, or snaps to the magic `(0, 0, -1)` fallback. Most visible when the user looks at the floor while walking.
**Fix:** make the math nullable, add a stable fallback using the camera's local Y axis (the top-of-the-phone direction, which still has a well-defined horizontal projection when the user pitches the camera).

```kotlin
fun Pose.forwardVec(): Vec3? {
    val zAxis = getZAxis()
    val raw = Vec3(-zAxis[0], -zAxis[1], -zAxis[2])
    val len = raw.length()
    return if (len < 1e-4f) null else raw * (1f / len)
}

/** Top-of-phone direction (camera local +Y) projected to world. Stable when forward is nearly vertical. */
private fun Pose.topVec(): Vec3 {
    val y = getYAxis()
    return Vec3(y[0], y[1], y[2])
}

fun positionInFrontOfCamera(cameraPose: Pose, meters: Float): Vec3? {
    val camera = cameraPose.translationVec()
    val forward = cameraPose.forwardVec() ?: return null
    var flatX = forward.x
    var flatZ = forward.z
    var flatLen = kotlin.math.sqrt(flatX * flatX + flatZ * flatZ)
    if (flatLen < 0.1f) {
        // Camera pitched near-vertical (looking up/down). Use the phone's top direction
        // projected onto the XZ plane — that's still the horizontal direction the user is "facing."
        val top = cameraPose.topVec()
        flatX = top.x
        flatZ = top.z
        flatLen = kotlin.math.sqrt(flatX * flatX + flatZ * flatZ)
        if (flatLen < 1e-4f) return null  // phone is on its side and looking straight up — give up for this frame
    }
    val k = meters / flatLen
    return Vec3(camera.x + flatX * k, camera.y, camera.z + flatZ * k)
}
```

And in `ArrowRenderer.floatingArrowPose`:
```kotlin
fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose? {
    val cameraVec = cameraPose.translationVec()
    val toTarget = horizontalDistanceMeters(cameraVec, target)
    val offset = minOf(ARROW_FORWARD_OFFSET_M, toTarget * 0.9f)
    val position = positionInFrontOfCamera(cameraPose, offset) ?: return null
    val yawDegrees = yawDegreesToward(position, target) ?: return null
    return ArrowPose(position = position, yawDegrees = yawDegrees, userDistanceToTargetMeters = toTarget)
}
```
The renderer already handles `arrowPose == null` gracefully (it just doesn't render).

Then drop the magic fallback in `Vec3.normalized()` — replace with:
```kotlin
fun normalized(): Vec3 {
    val len = length()
    return if (len < 1e-6f) Vec3(0f, 0f, 0f) else Vec3(x / len, y / len, z / len)
}
```
Audit other callers of `normalized()` after this change and ensure none of them rely on the old `(0, 0, -1)` behavior. Grep confirms `forwardVec` is the only relevant one in the rendering pipeline.

**Verify:** look at the floor while walking → arrow does not jitter horizontally; when the camera is pitched < ~85°, behavior is identical to before.

---

### L8 — `Vec3.rotateY` is dead code
**Where:** `PoseUtils.kt:93-98`.
**Fix:** delete it. Grep for callers in `app/` first; expect zero hits.

---

### L9 — `lerpPose` near-antipodal fallback uses `to`'s pre-flip quaternion, undoing the hemisphere alignment
**Where:** `NavigationViewModel.kt:622-626`.
**Fix:** use the flipped values:
```kotlin
val rotation = if (length > 0f) {
    floatArrayOf(qx / length, qy / length, qz / length, qw / length)
} else {
    floatArrayOf(toQx, toQy, toQz, toQw)
}
```

---

### L10 — `selectDestination` leaves the UI in a "Follow the floating arrow" state with no arrow until next frame
**Where:** `NavigationViewModel.kt:259-271`.
**Fix:** if a recent camera pose exists, run one progress update synchronously after publishing the path.
```kotlin
resetNavigationSmoothing()
_uiState.update { /* …same as today… */ }
latestCameraPose?.let {
    if (path.size > 1) updateNavigationProgress(it)
}
```
Make sure `updateNavigationProgress` no-ops cleanly when there's no graphToSessionPose yet; today it bails on `state.phase != Navigating` first, then reads `state.nodePoses` which would be empty → `firstOrNull` returns null → return. OK.

---

### L11 — `floorTransitionMessage` reads `currentWaypointIndex - 1` which can be -1 in defensive paths
**Where:** `NavigationViewModel.kt:812-829`.
**Symptom:** `state.path.getOrNull(state.currentWaypointIndex - 1)` returns null when index is 0; the function then falls back to `currentNodeId`. Safe today (initial index is 1 for size>1), but a future re-route at the user's exact node could set index 0.
**Fix:** short-circuit:
```kotlin
private fun floorTransitionMessage(state: NavigationUiState, waypoint: MapNode): String? {
    if (state.currentWaypointIndex <= 0) return null
    // …rest unchanged…
}
```

---

### L12 — Arrow placed at strict camera height blocks the user's forward view
**Where:** `PoseUtils.kt:87-89` — `y = camera.y` in `positionInFrontOfCamera`.
**Symptom:** With `ARROW_FORWARD_OFFSET_M = 1.5 m` and the arrow at exactly eye level, the arrow sits squarely in the user's central vision, occluding the real-world hallway, doorways, signs, etc. It feels like an obstacle, not a guide.
**Fix:** drop the arrow to chest/waist height. Add a constant offset and apply it in `positionInFrontOfCamera`:
```kotlin
const val ARROW_VERTICAL_OFFSET_M = -0.4f  // ~chest level for typical phone hold

fun positionInFrontOfCamera(cameraPose: Pose, meters: Float): Vec3? {
    // …horizontal computation as in L7…
    return Vec3(camera.x + flatX * k, camera.y + ARROW_VERTICAL_OFFSET_M, camera.z + flatZ * k)
}
```
Co-tunable with `ARROW_FORWARD_OFFSET_M`. If you want the arrow to bias toward the waypoint's height (useful for stair landings), blend toward target Y rather than a fixed −0.4:
```kotlin
val targetY = target.y  // pass into positionInFrontOfCamera or compute downstream
val y = camera.y + ARROW_VERTICAL_OFFSET_M + 0.25f * (targetY - camera.y)
```
That requires threading `target` into `positionInFrontOfCamera`. Cleaner to keep the constant offset for now and revisit if multi-floor demos need a tilt cue (covered separately by I1).
**Verify:** the arrow should sit just below the user's forward sightline; doorways and signage in front of the user remain unobstructed.

---

### L13 — `distanceToDestination` shows straight-line horizontal, not remaining route distance
**Where:** `NavigationViewModel.kt:837-842`.
**Symptom:** A destination 30 m away through three turns reports "Destination: 8.0 m" (the through-walls Euclidean number) — confusing and useless for ETA/UX.
**Fix:** compute the remaining route distance — the chord from the user's projection on the current segment to its end, plus the sum of remaining edge lengths.
```kotlin
private fun distanceToDestination(cameraPose: Pose, state: NavigationUiState): Float? {
    val transform = graphToSessionPose ?: return null
    if (state.path.isEmpty()) return null

    val cameraHorizontal = cameraPose.translationVec().horizontal()
    val poses = state.path.map { guidancePose(it, transform).translationVec().horizontal() }
    val projection = projectToPath(state, cameraHorizontal) ?: run {
        // No projection (single-node path or bail-out): fall back to direct distance.
        return horizontalDistanceMeters(cameraPose.translationVec(), poses.last())
    }
    val segIdx = projection.segmentIndex
    val a = poses[segIdx]; val b = poses[segIdx + 1]
    val ab = b - a
    val abLen = ab.length()
    val onSegmentRemaining = abLen * (1f - projection.segmentT.coerceIn(0f, 1f))
    var sum = onSegmentRemaining
    for (i in segIdx + 1 until poses.lastIndex) {
        sum += (poses[i + 1] - poses[i]).length()
    }
    return sum
}
```
Note: this uses `guidancePose` from the C4 fix, so apply C4 first. For multi-floor paths add the vertical leg lengths from C12's per-floor split.
**Verify:** plan a route through 3 turns; the destination distance should monotonically decrease as the user walks the corridor and should match the corridor walking distance to within a few meters.

---

## Render / GL / SceneView

### R1 — `arrowModelInstance` and `arrowMaterial` are never disposed
**Where:** `NavigationScreen.kt:135-148`.
**Fix:** add `DisposableEffect`s.
```kotlin
DisposableEffect(arrowModelInstance) {
    onDispose {
        arrowModelInstance?.let { runCatching { modelLoader.destroyModelInstance(it) } }
    }
}
DisposableEffect(arrowMaterial) {
    onDispose {
        arrowMaterial?.let { runCatching { it.destroy() } }
    }
}
```
If `MaterialInstance.destroy()` is not the SceneView 4.0.1 API, use `materialLoader.engine.destroyMaterialInstance(it)` (or whatever the engine accessor is in this version). Grep `materialLoader` source if uncertain.

---

### R2 — `SideEffect { cameraStream.isDepthOcclusionEnabled = true }` runs every recomposition
**Where:** `NavigationScreen.kt:130-132`.
**Fix:** move into `LaunchedEffect`:
```kotlin
LaunchedEffect(cameraStream) {
    cameraStream.isDepthOcclusionEnabled = true
}
```

---

### R3 — `ArAssetUtils.hasValidGlbAsset` reads the entire asset to check 12 bytes
**Where:** `ArAssetUtils.kt:6-7`.
**Fix:** read only the header.
```kotlin
fun hasValidGlbAsset(context: Context, assetPath: String): Boolean = runCatching {
    context.assets.open(assetPath).use { input ->
        val header = ByteArray(GLB_HEADER_LENGTH_BYTES)
        if (input.read(header) != GLB_HEADER_LENGTH_BYTES) return@runCatching false
        val magicOk = header[0] == 0x67.toByte() &&
            header[1] == 0x6C.toByte() &&
            header[2] == 0x54.toByte() &&
            header[3] == 0x46.toByte()
        val version = readLittleEndianUInt(header, 4)
        val declaredLength = readLittleEndianUInt(header, 8)
        val assetSize = context.assets.openFd(assetPath).use { it.length }
        magicOk && version == GLB_VERSION_2 &&
            declaredLength >= GLB_HEADER_LENGTH_BYTES.toLong() &&
            declaredLength <= assetSize
    }
}.getOrDefault(false)
```
If the asset is in a packed APK (no `openFd` available), drop the size check and just trust the header.

---

### R4 — `confidenceColor` lerps in sRGB linear → muddy mid-tones
**Where:** `NavigationScreen.kt:216-225`.
**Fix:** lerp in linear-light. Keep the sRGB→linear→sRGB roundtrip cheap.
```kotlin
private fun confidenceColor(confidence: Float): Color {
    fun toLinear(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    fun toSrgb(v: Float) = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.toDouble().pow(1.0/2.4).toFloat() - 0.055f
    val low = Color(0xFFFFC107)
    val high = Color(0xFF00BCD4)
    fun lerp(a: Float, b: Float) = toSrgb(toLinear(a) + (toLinear(b) - toLinear(a)) * confidence)
    return Color(red = lerp(low.red, high.red), green = lerp(low.green, high.green), blue = lerp(low.blue, high.blue), alpha = 1f)
}
```
(Cosmetic — defer if you don't want a ramp at all.)

---

### R5 — `autoAnimate = true` on `ModelNode` does nothing (arrow.glb has no animations) but ticks an animator each frame
**Where:** `NavigationScreen.kt:191-194`.
**Fix:** drop the parameter:
```kotlin
ModelNode(
    modelInstance = arrowModelInstance,
    scale = Scale(ARROW_MODEL_SCALE)
)
```

---

### R6 — Arrow `PoseNode` mounts/unmounts whenever `arrowPose` toggles null
**Where:** `NavigationScreen.kt:181-209`.
**Symptom:** Tracking loss / Arrived / re-route nullifies `arrowPose`, which removes the entire `PoseNode` subtree (and the `ModelNode` child); recovery re-creates it. Cheap pop on screen.
**Fix:** keep the node mounted; toggle visibility. SceneView nodes expose `isVisible`. Replace the arrow block:
```kotlin
key("nav-arrow") {
    val displayedPose = remember { mutableStateOf<Pose?>(null) }
    val visible = arrowPose != null
    arrowPose?.let { displayedPose.value = it.toArPose() }
    val pose = displayedPose.value
    if (pose != null) {
        PoseNode(
            pose = pose,
            apply = {
                isSmoothTransformEnabled = false
            }
        ) {
            // visibility toggling — adjust to match SceneView 4.0.1's API
            this.isVisible = visible
            if (arrowModelInstance != null) {
                ModelNode(
                    modelInstance = arrowModelInstance,
                    scale = Scale(ARROW_MODEL_SCALE)
                )
            } else if (arrowMaterial != null) {
                CubeNode(
                    size = Size(x = 0.08f, y = 0.05f, z = 0.30f),
                    center = Position(z = 0.15f),
                    materialInstance = arrowMaterial
                )
                SphereNode(
                    radius = 0.06f,
                    center = Position(z = 0.36f),
                    materialInstance = arrowMaterial
                )
            }
        }
    }
}
```
Note: SceneView 4.0.1's exact API for visibility may be `isVisible` on the node or on the parent — verify against the lib source. The intent is: keep the same Filament node alive across visibility toggles.

If SceneView's Compose surface doesn't expose visibility cleanly, fall back to: hold the last valid `pose` in a `remember` so the node stays mounted at the last seen position when `arrowPose == null`, and place the arrow far below the floor (y -= 1000) when invisible. That's hacky; prefer the visibility API.

---

### R7 — Verify wall-occlusion of the arrow on-device
**Where:** `ArSessionConfig.kt:12-14` (depth mode), `NavigationScreen.kt:131` (camera-stream depth occlusion).
**Symptom:** Some SceneView versions require a depth-occlusion-aware material variant for virtual content; `materialLoader.createColorInstance` may build a non-depth-aware material so the procedural arrow renders through walls even though the GLB does not.
**Fix:** smoke-test on a device. If the procedural arrow shows through walls while the GLB doesn't, switch the procedural path to a depth-aware material constructor (consult SceneView 4.0.1's `MaterialLoader` API). No code change if both paths already occlude correctly.

---

### R8 — Arrow asset pivot is at the **tail**, not the centroid
**Where:** `NavigationScreen.kt:189-194` (ModelNode at PoseNode origin); verified glb mesh bbox `z ∈ [0, 0.92]` from earlier audit. Same applies to the procedural fallback after C8 (extends z = 0..0.42 from origin).
**Symptom:** With pivot at the tail, the arrow extends `length × scale` *forward* of the placed pose. With C1 clamping `offset = min(1.5, dist*0.9)` and a 1.2 m advance, at distance 1.5 m the arrow tail sits at 1.35 m and the tip at ~1.76 m — past the waypoint. Visually it looks like the arrow is poking through whatever is at the waypoint.
**Fix (preferred, no asset change):** offset the rendered model backward by half its length so the pivot acts like the centroid. In the render block:
```kotlin
const val ARROW_HALF_LENGTH_LOCAL_M = 0.21f  // (0.92 * 0.45) / 2

if (arrowModelInstance.value != null) {
    ModelNode(
        modelInstance = arrowModelInstance.value!!,
        scale = Scale(ARROW_MODEL_SCALE)
    ).apply {
        position = Position(z = -ARROW_HALF_LENGTH_LOCAL_M)
    }
}
```
For the procedural fallback (after C8), shift both children by the same `-0.21`:
```kotlin
CubeNode(size = Size(0.08f, 0.05f, 0.30f), center = Position(z = 0.15f - 0.21f), …)
SphereNode(radius = 0.06f, center = Position(z = 0.36f - 0.21f), …)
```
Now both rendered arrows are centered on the PoseNode position (extend −0.21..+0.21).

**Alternative (asset change):** edit `arrow.glb` in Blender so the mesh origin sits at the centroid. Cleaner long-term, requires re-exporting the asset.
**Verify:** standing 1.5 m from a waypoint, the arrow's tip should sit visually at the waypoint, not past it.

---

### R9 — `arrow.glb` ships only POSITION + indices; PBR material has no normals
**Where:** `app/src/main/assets/arrow.glb` (verified: only `POSITION` and `indices` accessors, no `NORMAL`); material is PBR-metallic-roughness with `metallicFactor=0, roughnessFactor=0.45`.
**Symptom:** Filament's loader either generates flat per-face normals or uses a default direction. With PBR + ENVIRONMENTAL_HDR light estimation (`ArSessionConfig.kt:10`), the arrow's shading is faceted/flat and changes oddly as light estimation updates. Reads as a "low-fidelity" demo cue.
**Fix (preferred, simpler):** make the arrow material **unlit** so normals don't matter. Re-author `arrow.glb` with `KHR_materials_unlit`:
```json
"materials": [{
    "name": "ArrowOrange",
    "doubleSided": true,
    "pbrMetallicRoughness": { "baseColorFactor": [1.0, 0.35, 0.12, 1.0], "metallicFactor": 0.0, "roughnessFactor": 1.0 },
    "extensions": { "KHR_materials_unlit": {} }
}],
"extensionsUsed": ["KHR_materials_unlit"]
```
The procedural fallback should match — switch from `materialLoader.createColorInstance(...)` (PBR) to whatever SceneView 4.0.1 exposes for an unlit material loader (e.g., `createUnlitColorInstance` if available; otherwise build from a `mat-unlit` template).

**Alternative (asset change with normals):** re-export with smooth or per-face normals. If you keep PBR, also generate tangents if you ever add a normal map.

For a navigation demo, **unlit is the right call** — the arrow becomes the same readable orange under any lighting and stops shimmering as ENVIRONMENTAL_HDR estimation updates.
**Verify:** walk the same hallway with bright/dim/mixed lighting; arrow color should stay constant.

---

### R10 — Depth occlusion can flicker the arrow; spheres should be occluded but the arrow probably should not
**Where:** `NavigationScreen.kt:131` — `cameraStream.isDepthOcclusionEnabled = true` is global to the scene.
**Symptom:** ARCore's depth map is noisy, especially at edges and beyond ~3 m. A floating guidance arrow at 1.5 m forward gets clipped/flickered when the depth pixel under it disagrees with its actual distance, producing an unstable demo. Conversely, sphere markers that *are* anchored to the world genuinely should be occluded by walls.
**Fix:** scope occlusion to the markers, not the arrow. Two options:
1. **If SceneView exposes per-node depth-occlusion:** keep `cameraStream.isDepthOcclusionEnabled = true`, but on the arrow's `PoseNode`/`ModelNode` set the per-node depth-occlusion flag to false (e.g., `isDepthOcclusionEnabled = false` if available). Verify the API in SceneView 4.0.1.
2. **If only the global flag exists:** disable global depth occlusion (`cameraStream.isDepthOcclusionEnabled = false`) and accept that wall occlusion is lost for the spheres too. This is the simpler demo-safe choice — flicker is more obvious than "arrow visible through wall," especially since the user is following the arrow forward and rarely sees the spheres through a wall anyway.

This concern conflicts with R7 (which assumes occlusion is on); resolve them together with one device test:
- arrow occluded by wall → looks fine? keep on (R7 path).
- arrow flickers in mid-air → turn off (R10 path).

**Verify:** stand at the entrance of a 5 m corridor, place the arrow toward the far end; observe stability over 30 s.

---

## Improvements (optional polish, beyond bug fixes)

### I1 — Add pitch to the arrow for multi-floor / vertical guidance
**Where:** `ArrowRenderer.kt:5-9` (`ArrowPose`), `ArrowRenderer.kt:12-20` (`floatingArrowPose`), `NavigationScreen.kt:227-234` (`ArrowPose.toArPose`).
**Motivation:** the codebase docs already note "the arrow is horizontal-only; steep vertical changes might not be fully represented." For stairs/elevator/ramp segments, a 2D yaw-only compass loses the spatial cue.
**Fix:** add a `pitchDegrees` field, compute it from the vertical delta to the target, and compose it into the rendered quaternion (yaw first, pitch second so pitch tilts the already-yaw-aligned forward axis).

`ArrowRenderer.kt`:
```kotlin
data class ArrowPose(
    val position: Vec3,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val userDistanceToTargetMeters: Float
)

fun floatingArrowPose(cameraPose: Pose, target: Vec3): ArrowPose? {
    val cameraVec = cameraPose.translationVec()
    val toTarget = horizontalDistanceMeters(cameraVec, target)
    val offset = minOf(ARROW_FORWARD_OFFSET_M, toTarget * 0.9f)
    val position = positionInFrontOfCamera(cameraPose, offset) ?: return null
    val yaw = yawDegreesToward(position, target) ?: return null
    val dy = target.y - position.y
    val horizontal = horizontalDistanceMeters(position, target).coerceAtLeast(1e-3f)
    val pitch = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), horizontal.toDouble())).toFloat()
        .coerceIn(-MAX_ARROW_PITCH_DEG, MAX_ARROW_PITCH_DEG)
    return ArrowPose(position, yaw, pitch, toTarget)
}

const val MAX_ARROW_PITCH_DEG = 60f
```

`NavigationScreen.kt`:
```kotlin
private fun ArrowPose.toArPose(): Pose {
    val yawDeg = yawDegrees + ArrowRenderer.ARROW_MODEL_YAW_OFFSET_DEG
    val yawQ = Quaternion.fromAxisAngle(Float3(0f, 1f, 0f), yawDeg)
    // Pitch around local X after yaw — sign chosen so positive pitch tilts the +Z forward toward +Y.
    val pitchQ = Quaternion.fromAxisAngle(Float3(1f, 0f, 0f), -pitchDegrees)
    val q = yawQ * pitchQ
    return Pose(
        floatArrayOf(position.x, position.y, position.z),
        floatArrayOf(q.x, q.y, q.z, q.w)
    )
}
```
Smooth `pitchDegrees` the same way `smoothYawDegrees` smooths yaw (separate state, same exponential filter, same wrap handling not needed because pitch is bounded). Reset both on `resetNavigationSmoothing()`.
**Verify:** a stairs waypoint above the user should visibly tilt the arrow upward by ~30-45° while still pointing toward the stairwell horizontally.

---

## Open considerations (not converted into fixes)

- **Arrow position smoothing.** `NavigationScreen.kt:186` deliberately disables `isSmoothTransformEnabled`, and the position is recomputed each frame from the raw camera pose. Camera rotational noise of ~0.5° produces ~13 mm of horizontal arrow position oscillation at 1.5 m offset, contributing to "swimming." Re-enabling SceneView smoothing would re-introduce the per-frame target chase that fix.md C1 fought (the smoother converging on a moving target produces lag). The cleanest answer is to parent the arrow to a camera-anchored node (constant local offset, world position is implicit), and only update yaw/pitch via local rotation — but that requires rewiring the arrow rendering to attach under SceneView's camera node, which is a non-trivial change in SceneView 4.0.1's Compose API. **Defer until on-device testing shows the swimming is dominant**; if so, prefer the camera-parenting approach over re-enabling per-frame smoothing.

---

## Suggested order of application

Apply in this order to minimize regressions; each item is independently testable.

1. **C2, R1** — stop GPU/material leaks (highest leverage, cheapest to verify).
2. **C10** — async model load (kills the launch hitch and pairs cleanly with R1's disposal).
3. **C1** — kill the backward-flip dead zone.
4. **C3** — snap live anchor poses; smooth only estimates.
5. **C4** — feed the target/guidance pose into all navigation logic (arrow, advance, projection, reroute, distances).
6. **C7** — drop `updateNavigationProgress` from `publishNodeEstimates`.
7. **C5, L3, L4** — gate refit by interval / live anchor movement; consume return value; break-early on no-prune.
8. **C6** — planar Procrustes (preferred) or full Kabsch in `Relocalizer`. Add tests.
9. **C11** — Pathfinder smoothing requires real `a→c` adjacency before dropping `b`.
10. **C12** — floor-aware advancement and projection.
11. **C13** — relax `currentNode` selection once `graphToSessionPose` exists.
12. **C8** — rebuild procedural fallback geometry; **R8** — center pivot for both rendered arrows.
13. **R6** — keep arrow PoseNode mounted; use visibility.
14. **L1, L2, L5, L7, L9, L10, L11, L12, L13** — correctness/UX cleanup.
15. **R2, R3, R4, R5, R7, R10** — render/asset cleanup and on-device verification (R7 + R10 share one device test).
16. **R9** — re-author `arrow.glb` as unlit (or add normals).
17. **L6, L8** — batch state updates and remove dead `Vec3.rotateY`. Defer L6 if invasive.
18. **I1** — pitch support, only after the multi-floor advancement (C12) is solid.

## Manual on-device verification checklist

- Entering Navigation screen 10× in a row → no jank/dropped frame on entry (C10).
- Walk to ~1.3 m from a waypoint → arrow does not flip toward the user (C1).
- Stand still 30 s with several unresolved nodes → no measurable native heap or Filament resource growth (C2, R1, C5).
- Look straight down at the floor while walking → arrow does not jitter horizontally (L7).
- Arrow sits below the user's central sightline (chest/waist) (L12).
- Turn camera fast while waypoint is ~5 m away → arrow yaw catches up cleanly without overshoot (C4).
- Trigger a relocalization (resolve a new anchor mid-route) → arrow re-aims within one frame; next-distance updates without lag (C3, C4).
- Two resolved anchors + one estimated between → estimated sphere within ~5 cm of midpoint (C6).
- U-shaped graph with no `a→c` edge → route preserves the corner node (C11; covered by added Pathfinder test).
- Two-floor graph; stand under an upper-floor stairs waypoint → route does **not** auto-advance (C12).
- Resolve at entrance, walk past node B, pick a destination → route starts at B, not at the entrance (C13).
- 30 m destination through 3 turns → distance text decreases monotonically and matches walking distance (L13).
- Standing 1.5 m from a waypoint → arrow tip sits at the waypoint, not past it (R8).
- Same hallway under bright/dim/mixed light → arrow color stays constant (R9).
- Stand at corridor entrance, point arrow down it for 30 s → no flicker; spheres still occluded by walls (R7 + R10 device decision).
- Tracking loss → recover → no PoseNode pop, arrow re-appears at correct yaw (R6).
- Re-route → arrow snaps to new waypoint within one frame (L2).
- Remove `arrow.glb` from assets → procedural arrow has GLB-equivalent footprint and pivot (C8, R8).
- Stairs waypoint above user → arrow tilts up by ~30-45° (I1, after C12).

## Already covered (no separate action needed)

These cross-review findings duplicate items already in this document; no new fix is required, but listing them prevents future audits from re-reporting:
- "Compute yaw from camera, not the floating position" — superseded by C1's clamp; computing yaw from the camera was previously a separate bug (`fix.md` C2) and reverting to it would re-introduce mis-aim near close waypoints.
- "Arrow flips when between 1.2 m and 1.5 m" — C1.
- "Arrow disappears or flips near waypoints due to null yaw" — C1 (clamp eliminates the near-zero direction case).
- "Marker `MaterialInstance` churn / quantize confidence" — C2.
- "Material leaks on screen exit" — R1.

## Out of scope (do not change here)

- Cloud Anchor resolve cadence/backoff tuning (covered in `AUDIT_REPORT.md`).
- `MAX_PARALLEL_RESOLVES`, `RELOCALIZE_INTERVAL_MS`, etc. — these are pipeline-adjacent but not arrow-rendering bugs.
- Diagnostic overlay enrichment (FPS, residuals, etc.).
- `MapGraph` correctness; `Pathfinder` correctness *except* the smoothing connectivity bug folded in as C11.
