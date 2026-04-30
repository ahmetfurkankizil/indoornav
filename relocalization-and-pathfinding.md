# Relocalization & Pathfinding — Enhancement Plan

Companion to `fix.md`. Where `fix.md` covers the 3D arrow render pipeline, this doc covers the two upstream subsystems whose noise the arrow ultimately exposes: cloud-anchor relocalization and route computation.

Files in scope:
- `app/src/main/java/com/example/vecturai/ar/CloudAnchorHelper.kt`
- `app/src/main/java/com/example/vecturai/ar/ArSessionConfig.kt`
- `app/src/main/java/com/example/vecturai/ar/PoseUtils.kt` (math helpers)
- `app/src/main/java/com/example/vecturai/ui/navigation/NavigationViewModel.kt`
- `app/src/main/java/com/example/vecturai/graph/MapGraph.kt`
- `app/src/main/java/com/example/vecturai/graph/Pathfinder.kt`
- `app/src/main/java/com/example/vecturai/persistence/GraphRepository.kt` (for S6)

Conventions:
- "graph space" = the rigid local frame defined when the building was mapped (origin = first hosted anchor, see `MappingViewModel.relativePose` usage).
- "session space" = current ARCore session frame.
- A "transform" `T_sg` maps graph→session: `T_sg = anchorSessionPose ∘ node.graphPose⁻¹`.

---

## Part 1 — Relocalization Accuracy

### A1 — Multi-anchor consensus fit (Procrustes / Kabsch)
**Where:** `NavigationViewModel.kt:313` — `graphToSessionPose = sessionFromGraphPose(node, anchor.pose)` (last-resolve-wins).
**Problem:** Each resolved anchor has small independent error. Using whichever resolved most recently makes the global transform jitter every cycle, dragging all unresolved node poses with it.
**Fix:** When ≥2 anchors are resolved, compute one rigid transform that minimizes RMS error across all of them.

Add a new file `app/src/main/java/com/example/vecturai/ar/Relocalizer.kt`:
```kotlin
package com.example.vecturai.ar

import com.example.vecturai.graph.MapNode
import com.google.ar.core.Anchor
import com.google.ar.core.Pose

data class Correspondence(val graphPose: Pose, val sessionPose: Pose, val weight: Float = 1f)

object Relocalizer {
    /** Returns null if input is empty. Single-correspondence falls back to direct map. */
    fun fitGraphToSession(correspondences: List<Correspondence>): Pose? {
        if (correspondences.isEmpty()) return null
        if (correspondences.size == 1) {
            val c = correspondences.first()
            return c.sessionPose.compose(c.graphPose.inverse())
        }
        // Weighted centroids
        val totalW = correspondences.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1e-6f)
        var gx = 0f; var gy = 0f; var gz = 0f
        var sx = 0f; var sy = 0f; var sz = 0f
        correspondences.forEach { c ->
            val w = c.weight / totalW
            gx += c.graphPose.tx() * w; gy += c.graphPose.ty() * w; gz += c.graphPose.tz() * w
            sx += c.sessionPose.tx() * w; sy += c.sessionPose.ty() * w; sz += c.sessionPose.tz() * w
        }
        // 3x3 covariance H = sum(w * (g - g_bar) * (s - s_bar)^T)
        val H = FloatArray(9)
        correspondences.forEach { c ->
            val w = c.weight / totalW
            val gxr = c.graphPose.tx() - gx; val gyr = c.graphPose.ty() - gy; val gzr = c.graphPose.tz() - gz
            val sxr = c.sessionPose.tx() - sx; val syr = c.sessionPose.ty() - sy; val szr = c.sessionPose.tz() - sz
            H[0] += w * gxr * sxr; H[1] += w * gxr * syr; H[2] += w * gxr * szr
            H[3] += w * gyr * sxr; H[4] += w * gyr * syr; H[5] += w * gyr * szr
            H[6] += w * gzr * sxr; H[7] += w * gzr * syr; H[8] += w * gzr * szr
        }
        // SVD(H) = U S V^T. Use a small JAMA-style 3x3 SVD or pull a math lib (e.g. EJML).
        val (u, _, v) = svd3x3(H)
        // R = V * diag(1,1,det(V*U^T)) * U^T   (Kabsch, ensures right-handed)
        val r = kabschRotation(u, v)
        val tx = sx - (r[0]*gx + r[1]*gy + r[2]*gz)
        val ty = sy - (r[3]*gx + r[4]*gy + r[5]*gz)
        val tz = sz - (r[6]*gx + r[7]*gy + r[8]*gz)
        val q = matrixToQuaternion(r)
        return Pose(floatArrayOf(tx, ty, tz), q)
    }

    /** Drop correspondences whose post-fit residual exceeds `maxResidualMeters`. */
    fun rejectOutliers(correspondences: List<Correspondence>, fit: Pose, maxResidualMeters: Float = 1.0f): List<Correspondence> {
        return correspondences.filter { c ->
            val predicted = fit.compose(c.graphPose)
            val dx = predicted.tx() - c.sessionPose.tx()
            val dy = predicted.ty() - c.sessionPose.ty()
            val dz = predicted.tz() - c.sessionPose.tz()
            kotlin.math.sqrt(dx*dx + dy*dy + dz*dz) <= maxResidualMeters
        }
    }
}
```
- `svd3x3`, `kabschRotation`, `matrixToQuaternion`: implement inline (~80 LOC) or pull a tiny linear-algebra dep. EJML is the path of least resistance; for footprint reasons a hand-rolled 3×3 SVD via Jacobi iterations is fine.
- Iterate fit + outlier rejection up to 2 times for robustness.

Wire into `NavigationViewModel.publishNodeEstimates`:
```kotlin
private fun recomputeGraphToSession() {
    val graph = _uiState.value.graph ?: return
    val nodesById = graph.nodes.associateBy { it.id }
    val correspondences = resolvedAnchors.mapNotNull { (id, anchor) ->
        if (anchor.trackingState != TrackingState.TRACKING) return@mapNotNull null
        val node = nodesById[id] ?: return@mapNotNull null
        Correspondence(graphPose = node.graphPose(), sessionPose = anchor.pose, weight = 1f)
    }
    val initial = Relocalizer.fitGraphToSession(correspondences) ?: return
    val pruned = Relocalizer.rejectOutliers(correspondences, initial)
    graphToSessionPose = Relocalizer.fitGraphToSession(pruned) ?: initial
}
```
Call `recomputeGraphToSession()` at the start of `publishNodeEstimates` and once per frame from `onSessionUpdated` (cheap with ≤30 anchors).

### A2 — Confidence-weighted estimates
**Where:** `NavigationViewModel.kt:46-51` — `SessionNodePose` only carries `isResolved: Boolean`.
**Fix:** Add a confidence score and propagate it.
```kotlin
data class SessionNodePose(
    val nodeId: String,
    val label: String?,
    val pose: Pose,
    val isResolved: Boolean,
    val confidence: Float          // 0..1
)
```
Confidence formula (compose factors, clamp 0..1):
- `1.0` for resolved + tracking; else
- `exp(-distanceToNearestResolvedAnchorInGraphMeters / 8.0)` for estimates;
- multiplied by `exp(-secondsSinceLastResolve / 60.0)` (decay).

Use it: weight A1 fit by confidence; tint UI nodes with it ([NavigationScreen.kt:154](app/src/main/java/com/example/vecturai/ui/navigation/NavigationScreen.kt:154)) instead of binary resolved/estimated material.

### A3 — Re-poll resolved anchor poses each frame
**Where:** `NavigationViewModel.kt:331-342` reads `anchor.pose` only at publish time (every ~15 s).
**Problem:** ARCore continuously refines anchor poses as tracking improves, but the UI consumes a snapshot.
**Fix:** In `onSessionUpdated`, after `updateCurrentNode`, rebuild `nodePoses` (cheap loop) using fresh `anchor.pose` values:
```kotlin
private fun refreshNodePosesFromAnchors() {
    val graph = _uiState.value.graph ?: return
    val transform = graphToSessionPose ?: return
    val poses = graph.nodes.map { node ->
        val anchor = resolvedAnchors[node.id]
        val live = anchor?.takeIf { it.trackingState == TrackingState.TRACKING }?.pose
        SessionNodePose(
            nodeId = node.id,
            label = node.label,
            pose = live ?: estimateSessionPose(transform, node),
            isResolved = live != null,
            confidence = computeConfidence(node, live, transform)
        )
    }
    _uiState.update { it.copy(nodePoses = poses) }
}
```
Call from `onSessionUpdated` when `trackingState == TRACKING`.

### A4 — Drift-aware re-fit every frame
**Where:** Same as A1.
**Fix:** Once A1 + A3 are in, the Kabsch fit is cheap enough to run every frame. This eliminates the visible 15-s "teleport" cycle entirely (the relocalize loop becomes purely about *adding new anchors*, not refining the transform).

### A5 — Filter on `Anchor.trackingState`
**Where:** `NavigationViewModel.kt:331-342` and the new A1 code.
**Problem:** Cloud anchors can become `PAUSED` when the device is far from the original visibility cone. Using their stale poses pollutes the fit.
**Fix:** Only include anchors in the fit / `nodePoses` when `anchor.trackingState == TrackingState.TRACKING`. Already shown in A1/A3 snippets above.

---

## Part 2 — Relocalization Speed

### S1 — Spatial-prior resolve queue
**Where:** `NavigationViewModel.kt:287-293` — `graph.nodes.chunked(MAX_PARALLEL_RESOLVES)`.
**Problem:** All nodes resolved in arbitrary order; far-away anchors waste budget because they rarely succeed from the current view.
**Fix:** Once one anchor is resolved (and `graphToSessionPose` is known), sort the queue by graph-space distance to the *estimated* user position; resolve nearest-first.
```kotlin
private fun nextResolveOrder(graph: MapGraph): List<MapNode> {
    val transform = graphToSessionPose
    val cam = latestCameraPose ?: return graph.nodes
    if (transform == null) return graph.nodes
    val userInGraph = transform.inverse().compose(cam).translationVec()
    return graph.nodes.sortedBy {
        val n = Vec3(it.xMeters, it.yMeters, it.zMeters)
        (n - userInGraph).length()
    }
}
```
Use this in place of `graph.nodes` inside the resolve loop.

### S2 — Don't re-resolve already-resolved tracking anchors
**Where:** `NavigationViewModel.kt:287-293`.
**Problem:** Every 15 s the loop hammers every node, including ones in `resolvedAnchors`. Wastes API quota and crowds the camera-feature pipeline.
**Fix:**
```kotlin
val pending = nextResolveOrder(graph).filter { node ->
    val existing = resolvedAnchors[node.id]
    existing == null || existing.trackingState != TrackingState.TRACKING
}
for (batch in pending.chunked(MAX_PARALLEL_RESOLVES)) { /* resolve */ }
```
A separate slow watchdog (every ~60 s) re-resolves anchors that have been `PAUSED` for >10 s.

### S3 — Adaptive concurrency + per-node retry budget
**Where:** `NavigationViewModel.kt:439` — `MAX_PARALLEL_RESOLVES = 40`.
**Problem:** ARCore's resolve uses the camera image; >~10 concurrent resolves contend for the same frames and slow each other down. 40 is also above Google's stated practical concurrency.
**Fix:**
- Reduce to 8.
- Track a per-node failure counter:
```kotlin
private val nodeFailureCount = mutableMapOf<String, Int>()
private fun shouldAttempt(nodeId: String): Boolean {
    val failures = nodeFailureCount[nodeId] ?: 0
    return failures < 6   // give up after 6 failures, manual relocalize re-arms
}
private fun shouldDelay(nodeId: String): Long {
    val failures = nodeFailureCount[nodeId] ?: 0
    return (1L shl failures.coerceAtMost(4)) * 1000L  // 1,2,4,8,16s exponential
}
```
On failure: `nodeFailureCount.merge(node.id, 1, Int::plus)`. On success: `nodeFailureCount.remove(node.id)`. Skip nodes whose `shouldDelay` window hasn't elapsed since last attempt.
- `relocalizeNow()` should clear `nodeFailureCount`.

### S4 — Cancel ARCore futures on timeout
**Where:** `NavigationViewModel.kt:305-307`, `CloudAnchorHelper.kt:64-96`.
**Problem:** `withTimeoutOrNull` cancels the suspending coroutine; whether this triggers `invokeOnCancellation` in `suspendCancellableCoroutine` is version-dependent and the underlying `ResolveFuture` keeps running, holding camera resources.
**Fix:** Capture the future explicitly and cancel it on timeout:
```kotlin
suspend fun resolveAnchor(session: Session, cloudAnchorId: String, timeoutMs: Long): Result<Anchor> {
    var future: ResolveCloudAnchorFuture? = null
    return try {
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                future = session.resolveCloudAnchorAsync(cloudAnchorId) { anchor, state ->
                    if (!cont.isActive) return@resolveCloudAnchorAsync
                    if (state == Anchor.CloudAnchorState.SUCCESS) cont.resume(Result.success(anchor))
                    else { anchor?.detach(); cont.resume(Result.failure(IllegalStateException("Resolve failed: $state"))) }
                }
                cont.invokeOnCancellation { future?.cancel() }
            }
        }
    } catch (t: TimeoutCancellationException) {
        future?.cancel()
        Result.failure(IllegalStateException("Resolve timed out"))
    }
}
```
Move the timeout into the helper; remove `withTimeoutOrNull` wrapper from the ViewModel.

### S5 — First-fix pre-warm before fan-out
**Where:** `NavigationViewModel.kt:275-298`.
**Problem:** UI waits in `Localizing` until at least one anchor resolves, but the loop fans out 40 simultaneous resolves immediately, starving the camera pipeline; "first fix" can take 10–20 s instead of 2–4 s.
**Fix:** Sequential first-fix, parallel after:
```kotlin
resolveJob = viewModelScope.launch {
    // Phase 1: first-fix — try anchors one at a time, nearest first.
    while (isActive && resolvedAnchors.isEmpty()) {
        for (node in nextResolveOrder(graph).take(8)) {
            if (resolveNode(activeSession, node)) break
        }
        if (resolvedAnchors.isEmpty()) delay(2_000L)
    }
    // Phase 2: fill out the rest in small batches with adaptive concurrency.
    while (isActive) {
        val pending = pendingNodes(graph)
        for (batch in pending.chunked(MAX_PARALLEL_RESOLVES)) {
            coroutineScope { batch.map { async { resolveNode(activeSession, it) } }.awaitAll() }
        }
        delay(RELOCALIZE_INTERVAL_MS)
    }
}
```
`resolveNode` should return `Boolean` (success).

### S6 — Persist last-known transform across cold starts
**Where:** `GraphRepository.kt` (extend), `NavigationViewModel.selectBuilding`.
**Problem:** Every session re-runs full localization from scratch.
**Fix:** Save a per-building hint:
```json
{ "lastResolvedAnchorIds": ["..."], "lastFitTimestampMs": 1714512345678, "lastUserGraphPose": {...} }
```
On `selectBuilding`, prioritize `lastResolvedAnchorIds` in the resolve queue (S1's spatial prior fed by `lastUserGraphPose` as the seed when no live `latestCameraPose` is available yet). Save in `publishNodeEstimates` after a successful Kabsch fit.

### S7 — Stop reconfiguring the session mid-resolve
**Where:** `CloudAnchorHelper.kt:98-104` — `ensureCloudAnchorModeEnabled`.
**Problem:** `session.configure(...)` stalls a frame and can blip tracking. The session is already configured at startup (`ArSessionConfig.configureIndoorCloudSession` in `NavigationScreen.kt:147`).
**Fix:** Delete `ensureCloudAnchorModeEnabled` and the calls at `CloudAnchorHelper.kt:23, 69`. Trust startup config. Add a debug-only assert that `session.config.cloudAnchorMode == ENABLED` on first resolve.

### S8 — Geospatial / VPS as long-term path (informational)
On supported devices, `Config.GeospatialMode.ENABLED` + `Earth.getCameraGeospatialPose()` gives sub-meter localization without Cloud Anchor resolves (subject to street-level VPS coverage, which is poor indoors but improving). Not actionable now; revisit when ARCore Geospatial gets indoor localization data.

---

## Part 3 — Pathfinding Accuracy

### P1 — Re-route on user deviation
**Where:** `NavigationViewModel.kt:218` — `Pathfinder(graph).shortestPath(...)` runs only at `selectDestination`.
**Problem:** If the user wanders or drift shifts node poses, the original path becomes suboptimal but is never recomputed.
**Fix:** Add periodic re-routing in `updateNavigationProgress`:
```kotlin
private var lastRerouteAtMs = 0L
private fun maybeReroute(state: NavigationUiState, cameraPose: Pose) {
    val now = System.currentTimeMillis()
    if (now - lastRerouteAtMs < 3_000L) return            // throttle 3s
    val deviation = perpendicularDistanceToPath(state, cameraPose)
    if (deviation < 1.5f) return                          // still on path
    val graph = state.graph ?: return
    val destinationId = state.selectedDestinationId ?: return
    val startId = closestNodeIdToCamera(state, cameraPose) ?: return
    val newPath = Pathfinder(graph).shortestPath(startId, destinationId) ?: return
    lastRerouteAtMs = now
    _uiState.update { it.copy(path = newPath, currentWaypointIndex = 1.coerceAtMost(newPath.lastIndex)) }
}
```
`perpendicularDistanceToPath` reuses the projection from P2 below.

### P2 — Project user onto polyline; don't snap to nearest node
**Where:** `NavigationViewModel.kt:365-371` — `updateCurrentNode` picks the closest node by distance.
**Problem:** When the user passes a node off-route, "closest node" can flip backwards. With per-frame waypoint advance, the arrow jitters between waypoints.
**Fix:** Project the camera onto each path segment, find the segment with smallest perpendicular distance, advance waypoint based on segment progress:
```kotlin
private fun projectToPath(state: NavigationUiState, camHorizontal: Vec3): PathProjection? {
    val poses = state.path.mapNotNull { node ->
        state.nodePoses.firstOrNull { it.nodeId == node.id }?.pose?.translationVec()?.horizontal()
    }
    if (poses.size < 2) return null
    var bestIdx = 0; var bestT = 0f; var bestDist = Float.MAX_VALUE
    for (i in 0 until poses.lastIndex) {
        val a = poses[i]; val b = poses[i + 1]
        val ab = b - a; val len2 = ab.x*ab.x + ab.z*ab.z
        if (len2 < 1e-4f) continue
        val t = (((camHorizontal - a).x * ab.x) + ((camHorizontal - a).z * ab.z)) / len2
        val tc = t.coerceIn(0f, 1f)
        val proj = a + ab * tc
        val d = (camHorizontal - proj).length()
        if (d < bestDist) { bestDist = d; bestIdx = i; bestT = tc }
    }
    return PathProjection(segmentIndex = bestIdx, segmentT = bestT, perpDist = bestDist)
}
```
Advance: target waypoint is `path[segmentIndex + 1]` when `segmentT > 0.5`, else `path[segmentIndex + 1]` once perpendicular distance drops + horizontal distance to that waypoint < threshold. Replace the existing node-snapping waypoint advance entirely.

### P3 — Turn-cost edge weights + line-of-sight smoothing
**Where:** `Pathfinder.kt:7-15`, `MapGraph.kt:37-42`.
**Problem:** Dijkstra on raw distance produces zig-zag routes through dense node clusters.
**Fix (option A) — turn penalty:**
```kotlin
// In Pathfinder, augment state with previous edge direction.
private fun edgeCost(prev: Vec3?, here: Vec3, next: Vec3, baseDistance: Float): Float {
    if (prev == null) return baseDistance
    val a = (here - prev).horizontal().normalized()
    val b = (next - here).horizontal().normalized()
    val cosA = (a.x*b.x + a.z*b.z).coerceIn(-1f, 1f)
    val turnRad = kotlin.math.acos(cosA)
    return baseDistance + 0.6f * turnRad     // 0.6m per radian (~10° ≈ 0.1m)
}
```
Pathfinder state becomes `(nodeId, incomingFromId)` instead of `nodeId`; queue ordering by accumulated cost. This roughly doubles state space but is still <1 ms for typical graphs.

**Fix (option B) — string-pulling post-process:**
After computing the path, walk triples (A,B,C); if line-segment A→C stays within `maxDeviation` of the original A→B→C polyline, drop B. Iterate until stable. Keep B if it's on a different floor or labeled.

Apply both: A first, then B for further smoothing.

### P4 — Floor transitions
**Where:** `MapGraph.kt:25` (`floor` field exists, unused), `Pathfinder.kt`.
**Fix:**
- Add `MapEdge.kind: EdgeKind = CORRIDOR` (`enum class EdgeKind { CORRIDOR, STAIRS, ELEVATOR }`). Make it `@Serializable` with a default for backward-compat.
- In Pathfinder, multiply `STAIRS` edges by `1.5f`, `ELEVATOR` by `1.2f` (penalize friction).
- Surface edge kind to UI: when `currentWaypoint` is on a different floor than the camera-est current node, set `statusMessage = "Take the stairs to floor ${waypoint.floor}"`.
- Mapping flow needs UI to mark stair/elevator edges; out of scope for this doc but flag it.

### P5 — Recompute weights from current geometry
**Where:** `MapEdge.distanceMeters` ([MapGraph.kt:41](app/src/main/java/com/example/vecturai/graph/MapGraph.kt:41)) is captured at mapping time.
**Problem:** If you ever shift to session-space pathfinding (after Kabsch refit), stored weights are stale.
**Fix:** Add a helper:
```kotlin
fun MapGraph.withRefreshedWeights(): MapGraph {
    val byId = nodes.associateBy { it.id }
    val refreshed = edges.map { e ->
        val a = byId[e.fromNodeId] ?: return@map e
        val b = byId[e.toNodeId] ?: return@map e
        val d = kotlin.math.sqrt(
            (a.xMeters - b.xMeters).let { it*it } +
            (a.yMeters - b.yMeters).let { it*it } +
            (a.zMeters - b.zMeters).let { it*it }
        )
        e.copy(distanceMeters = d)
    }
    return copy(edges = refreshed)
}
```
Call once on graph load. Don't mutate persisted file.

### P6 — Deterministic tie-breaking
**Where:** `Pathfinder.kt:23`.
**Fix:** Secondary key by Euclidean heuristic to goal:
```kotlin
val queue = PriorityQueue(compareBy<Triple<String, Float, Float>>({ it.second + it.third }, { it.third }))
queue.add(Triple(startId, 0f, heuristic(startId, goalId)))
```
This naturally becomes A* (Q1 below).

---

## Part 4 — Pathfinding Speed

### Q1 — A\* (subsumes P6)
**Where:** `Pathfinder.kt:17-44`.
**Fix:** Add a Euclidean heuristic in graph space:
```kotlin
private fun euclidean(a: MapNode, b: MapNode): Float {
    val dx = a.xMeters - b.xMeters; val dy = a.yMeters - b.yMeters; val dz = a.zMeters - b.zMeters
    return kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)
}

fun shortestPath(startId: String, goalId: String): List<MapNode>? {
    val start = nodeById[startId] ?: return null
    val goal = nodeById[goalId] ?: return null
    if (startId == goalId) return listOf(start)

    val gScore = mutableMapOf(startId to 0f)
    val previous = mutableMapOf<String, String>()
    val open = PriorityQueue(compareBy<Pair<String, Float>> { it.second })
    open.add(startId to euclidean(start, goal))

    while (open.isNotEmpty()) {
        val (nodeId, _) = open.poll()
        if (nodeId == goalId) break
        val nodeG = gScore[nodeId] ?: continue
        for (edge in adjacentEdges[nodeId].orEmpty()) {
            val tentative = nodeG + edge.distanceMeters
            if (tentative < (gScore[edge.toNodeId] ?: Float.MAX_VALUE)) {
                gScore[edge.toNodeId] = tentative
                previous[edge.toNodeId] = nodeId
                val to = nodeById[edge.toNodeId] ?: continue
                open.add(edge.toNodeId to tentative + euclidean(to, goal))
            }
        }
    }
    if (goalId !in previous) return null
    return generateSequence(goalId) { previous[it] }.toList().reversed().mapNotNull { nodeById[it] }
}
```
Heuristic is admissible because graph edge weights are real-world distances ≥ Euclidean.

### Q2 — Cache adjacency on `MapGraph`, not in Pathfinder
**Where:** `Pathfinder.kt:6-15` — rebuilt every Pathfinder construction.
**Fix:** Move into `MapGraph` as lazy properties:
```kotlin
@Transient private var _adjacency: Map<String, List<MapEdge>>? = null
fun adjacency(): Map<String, List<MapEdge>> {
    _adjacency?.let { return it }
    val a = edges.flatMap { listOf(it, MapEdge(it.toNodeId, it.fromNodeId, it.distanceMeters, it.kind)) }
        .groupBy { it.fromNodeId }
    _adjacency = a; return a
}
@Transient private var _byId: Map<String, MapNode>? = null
fun nodeById(): Map<String, MapNode> {
    _byId?.let { return it }
    val m = nodes.associateBy { it.id }; _byId = m; return m
}
```
`Pathfinder` just reads these. Allocations drop to one map per graph load.

### Q3 — Native bidirectional edges
**Where:** `Pathfinder.kt:9-15` doubles edges at runtime.
**Fix:** Add `MapEdge.bidirectional: Boolean = true`. Adjacency builder respects it. Persisting one direction in JSON stays compatible. Saves O(E) allocations on graph load.

### Q4 — Incremental re-pathing
**Where:** Re-route flow from P1.
**Problem:** Recomputing the entire path every 3 s on a 500-node graph is fine (sub-millisecond with A*) but wasteful.
**Fix:** Cheap heuristic: if perpendicular deviation < 3 m and the head of the existing path remains optimal for the user's current segment, **don't** re-run A\*. Only re-run when:
- deviation > 3 m, OR
- user is past the path's end (rare), OR
- a new node became resolved within the path corridor (might shorten the route).
For >1k-node graphs, swap to D\*-Lite (overkill at current scale).

### Q5 — Landmark-distance heuristic (ALT)
For very large graphs, precompute on save: pick K landmarks (random or farthest-point-sampled), run Dijkstra from each, store `dist[landmark][node]` in graph metadata. Heuristic becomes `max_k |dist[k][n] - dist[k][goal]|` — a tighter lower bound than Euclidean. Not worth implementing until the largest graph exceeds ~1k nodes.

---

## Suggested order of execution

1. **A1 + A4 + A3** — Kabsch fit + per-frame refresh. Eliminates 15-s teleport. Pair with `fix.md` C5/C6.
2. **S2 + S5 + S3 + S7** — first-fix pre-warm, skip resolved, lower concurrency, drop session reconfigure. Localization time drops from minutes to seconds.
3. **S1 + S4** — spatial-prior queue + reliable cancel.
4. **Q1** — A\* heuristic; one-function change.
5. **Q2** — cache adjacency on `MapGraph`.
6. **P2** — project user onto path polyline (kills "arrow snaps backward" jitter).
7. **P1** — re-route on deviation.
8. **A2** — confidence scoring, plumbed to UI.
9. **P3** — turn-cost / string-pulling smoothing.
10. **P4** — floor-transition edges and UX strings.
11. **S6** — persist last-known transform.
12. **P5, Q3, Q4, Q5** — cleanup and scale-out.

---

## Verification checklist

Relocalization:
- Cold start with 1 mapped anchor visible: first fix in ≤4 s on a typical Pixel-class device. (S5)
- Walking around a 30-anchor building: `graphToSessionPose` does **not** jump every 15 s. (A1, A4)
- `resolveAttemptCount` does not climb on already-resolved anchors. (S2)
- Killing the app mid-localization and re-opening: UI suggests the last-known anchor first. (S6)
- Resolving an anchor that becomes `PAUSED` (cover the camera): node falls back to estimate seamlessly. (A5)

Pathfinding:
- Walk past a waypoint by 1 m off-route: arrow continues to target the *next* waypoint, not the one you just passed. (P2)
- Walk perpendicular to the path 3 m: path recomputes and arrow rotates accordingly within ~3 s. (P1)
- A\* on a 100-node graph: completes in < 1 ms (log via `Trace.beginSection`). (Q1)
- Stair-only path: status text mentions floor change. (P4)
- Two routes of equal length: same one chosen across runs. (P6/Q1)

Smoke test for combined pipeline (after `fix.md` + this doc):
- Open app, select a building, walk a 30 m route through 6 waypoints with one stair transition. Arrow stays glued ahead of camera, no swing on relocalize, advances cleanly through each waypoint, says "Arrived" within 1.2 m of destination.
