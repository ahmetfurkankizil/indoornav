# Indoor AR Navigation — Android Demo App

**Comprehensive Implementation Plan**

Target: A two-mode Android application that (1) maps an indoor building by walking through it with the camera open, then (2) localizes the user anywhere in that building and guides them to a chosen room via on-screen AR arrows.

Scope: Single building, demo quality, single device. No multi-user sync, no production deployment, no CI/CD, no analytics.

---

## Table of Contents

1. [Core Concept and Why It Works](#1-core-concept-and-why-it-works)
2. [Technology Stack](#2-technology-stack)
3. [System Architecture](#3-system-architecture)
4. [Data Model](#4-data-model)
5. [Project Setup](#5-project-setup)
6. [Implementation Phase 1 — Project Skeleton and AR Session](#6-implementation-phase-1--project-skeleton-and-ar-session)
7. [Implementation Phase 2 — Cloud Anchor Host/Resolve Round-Trip](#7-implementation-phase-2--cloud-anchor-hostresolve-round-trip)
8. [Implementation Phase 3 — Mapping Mode](#8-implementation-phase-3--mapping-mode)
9. [Implementation Phase 4 — Persistence Layer](#9-implementation-phase-4--persistence-layer)
10. [Implementation Phase 5 — Navigation Mode](#10-implementation-phase-5--navigation-mode)
11. [Implementation Phase 6 — Pathfinding](#11-implementation-phase-6--pathfinding)
12. [Implementation Phase 7 — AR Arrow Rendering](#12-implementation-phase-7--ar-arrow-rendering)
13. [Implementation Phase 8 — Drift Correction and Waypoint Advancement](#13-implementation-phase-8--drift-correction-and-waypoint-advancement)
14. [Testing and Tuning](#14-testing-and-tuning)
15. [Known Gotchas and Mitigations](#15-known-gotchas-and-mitigations)
16. [Build Schedule](#16-build-schedule)
17. [Stretch Goals](#17-stretch-goals-optional)

---

## 1. Core Concept and Why It Works

The hard problem in indoor AR navigation is *persistent localization*: knowing where the phone is, in a building-fixed coordinate system, every time the app launches. GPS is too noisy and often unavailable indoors. Building a custom visual SLAM system from scratch is months of work.

**The shortcut:** Google ARCore's **Cloud Anchors** API solves persistent localization for us. A Cloud Anchor is a 3D position in space, hosted on Google's servers, that any device with ARCore can later *resolve* by pointing its camera at the same physical location. Once resolved, you know the transform between the device's local AR coordinate frame and the building's saved coordinate frame.

**The architecture this enables:**

- **Mapping** = walking through the building dropping Cloud Anchors as breadcrumbs, building a graph where nodes are anchors and edges are walked segments between them.
- **Navigation** = on app start, resolve as many of those anchors as possible to localize, then run shortest-path search over the graph and render arrows to the destination.

ARCore's built-in **Visual-Inertial Odometry (VIO)** fuses camera frames with IMU data continuously, so once you're localized, the device knows where it is moment-to-moment without needing to re-resolve every frame. Cloud Anchor resolutions are used periodically to *correct drift*.

**No GPS is needed indoors.** The IMU is implicit inside ARCore — you don't access it directly.

---

## 2. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| IDE | Android Studio (current stable) | Standard. |
| Min SDK | API 24 (Android 7.0) | ARCore minimum. |
| Target SDK | API 34 | Current Play target requirement. |
| Language | Kotlin | First-class Android, concise, coroutines for async. |
| UI | Jetpack Compose | Faster to iterate than XML; integrates fine with AR view. |
| AR Engine | ARCore SDK | Cloud Anchors, VIO, plane detection. |
| AR Rendering | SceneView for Android | Maintained successor to deprecated Sceneform. Wraps ARCore + Filament. Provides `ARSceneView` Compose composable, `ModelNode`, `AnchorNode`, GLB loading. |
| Async | Kotlin Coroutines + Flow | For non-blocking anchor resolution and UI state. |
| Persistence | JSON file in app internal storage via `kotlinx.serialization` | Tiny dataset, no need for Room/SQLite. |
| 3D Models | A single arrow `.glb` from Poly Haven, Sketchfab CC0, or modeled in Blender | One asset is enough. |
| Permissions | `CAMERA` only | Internet is implicit; no location permission needed. |

**Things explicitly excluded:**
- No Niantic Lightship, no Immersal, no Mapbox — overkill for a demo.
- No Firebase — single device means no sync needed.
- No Sceneform — deprecated since 2021. Use SceneView instead.
- No GPS / `ACCESS_FINE_LOCATION` — useless indoors.

---

## 3. System Architecture

### High-level flow

```
APP LAUNCH
   |
   v
[Mode Picker Screen]  ----> [Mapping Mode]  ----> save graph.json
                       \
                        ----> [Navigation Mode] --> load graph.json
                                                 --> resolve anchors
                                                 --> pick destination
                                                 --> render arrows
```

### Module layout

```
app/
  src/main/java/com/example/indoornav/
    MainActivity.kt                  -- single activity, hosts Compose nav
    ui/
      ModePickerScreen.kt            -- "Map a building" / "Navigate"
      mapping/
        MappingScreen.kt             -- Compose UI on top of ARSceneView
        MappingViewModel.kt          -- mapping state machine
      navigation/
        NavigationScreen.kt          -- Compose UI on top of ARSceneView
        NavigationViewModel.kt       -- navigation state machine
        DestinationPicker.kt         -- list of room labels
    ar/
      ArSessionConfig.kt             -- ARCore Session configuration helper
      CloudAnchorHelper.kt           -- host/resolve wrappers, coroutines
      PoseUtils.kt                   -- pose math, distance, transforms
    graph/
      MapGraph.kt                    -- nodes, edges, serialization
      Pathfinder.kt                  -- A* / Dijkstra
    persistence/
      GraphRepository.kt             -- read/write graph.json
  src/main/assets/
    arrow.glb                        -- the AR arrow model
  src/main/AndroidManifest.xml       -- CAMERA + ARCore meta-data
  build.gradle.kts (app)             -- dependencies
build.gradle.kts (project)
```

### State machines

**Mapping mode states:**

`Idle` → `Hosting` (after user taps Drop Pin) → `Idle` (anchor confirmed) → ... → `Tagging` (user names the spot) → `Idle` → `Saving` → done.

Auto-drop logic runs continuously in `Idle`: every frame, compare current camera pose to the last anchor's pose; if distance > threshold and tracking is healthy, transition to `Hosting`.

**Navigation mode states:**

`Loading` → `Localizing` (resolving anchors in parallel) → `PickingDestination` → `Navigating` (rendering arrows, advancing waypoints) → `Arrived`.

`Localizing` can re-trigger anytime confidence drops or the user requests it.

---

## 4. Data Model

The persistent map is a small graph. Use Kotlin data classes with `kotlinx.serialization`:

```kotlin
@Serializable
data class MapGraph(
    val buildingName: String,
    val createdAtEpochMs: Long,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>
)

@Serializable
data class MapNode(
    val id: String,                 // local UUID, used as graph key
    val cloudAnchorId: String,      // returned by ARCore on host success
    val label: String? = null,      // null = waypoint, non-null = named room
    // Pose of this anchor in the *first anchor's* coordinate frame.
    // Captured during mapping by composing relative poses.
    val xMeters: Float,
    val yMeters: Float,
    val zMeters: Float,
    val floor: Int = 0
)

@Serializable
data class MapEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float
)
```

**Coordinate frame note:** ARCore's session-local frame is reset each session. We anchor the *graph* to the first hosted anchor — every other node's recorded position is computed relative to that first anchor at mapping time. At navigation time, when we resolve any anchor, we get its pose in the *current* session frame and can therefore reconstruct everyone else's pose by composing the saved relative offsets. (In practice, since each resolved anchor gives us its own session-frame pose directly, we mostly use the saved offsets only for rendering arrows along edges between resolved nodes.)

**On-disk file:** `<internal storage>/graphs/<buildingName>.json`.

---

## 5. Project Setup

### 5.1 Create the project

In Android Studio: New Project → Empty Activity (Compose) → Kotlin → Min SDK 24.

### 5.2 `AndroidManifest.xml`

Add inside `<manifest>`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />

<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
```

Add inside `<application>`:

```xml
<meta-data android:name="com.google.ar.core" android:value="required" />
<meta-data android:name="com.google.ar.core.min_apk_version" android:value="240916000" />
```

`required` means Play Store will only show the app to ARCore-supported devices. For an installed-from-APK demo this is fine; you can also use `optional` to soften it.

### 5.3 `app/build.gradle.kts` dependencies

```kotlin
dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // ARCore + SceneView
    implementation("com.google.ar:core:1.45.0")          // check current
    implementation("io.github.sceneview:arsceneview:2.2.1") // check current

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}
```

Add the Kotlin serialization plugin to the `plugins { }` block:

```kotlin
plugins {
    kotlin("plugin.serialization") version "2.0.20"
}
```

Always check the SceneView GitHub releases for the latest version compatible with your ARCore version — they bump together.

### 5.4 Add the arrow model

Drop `arrow.glb` into `app/src/main/assets/`. Make it ~20 cm long, pointing along the +Z axis with origin at its tail, so positioning code is intuitive.

---

## 6. Implementation Phase 1 — Project Skeleton and AR Session

**Goal:** Open the app, see a live camera feed with ARCore tracking, render a static cube in front of the camera. Confirms ARCore + SceneView are wired correctly.

### 6.1 Camera permission

Use `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` in a Compose effect. Block the AR screen until granted.

### 6.2 ARSceneView in Compose

```kotlin
@Composable
fun ArScreen() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val nodes = remember { mutableStateListOf<Node>() }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        childNodes = nodes,
        sessionConfiguration = { session, config ->
            config.depthMode = Config.DepthMode.AUTOMATIC
            config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }
    )
}
```

`Config.CloudAnchorMode.ENABLED` is required to host or resolve. Forgetting this is the #1 first-day bug.

### 6.3 Smoke test

Add a debug button that drops a small `ModelNode` (cube or arrow) one meter in front of the camera. If it stays planted in space as you walk around, ARCore tracking is working.

**Exit criteria:** Camera view renders, debug cube anchors in space, app doesn't crash on rotate or background/foreground.

---

## 7. Implementation Phase 2 — Cloud Anchor Host/Resolve Round-Trip

**Goal:** On one button tap, host a Cloud Anchor at the current camera pose. On a second button tap (later, possibly after restart), resolve that same anchor by ID and render a marker at it.

This is the **single highest-risk piece**. Validate it in your actual building before building anything else, because if Cloud Anchors don't resolve well in your environment, the whole architecture is wrong and you'll want to switch to Immersal or another VPS.

### 7.1 Hosting

Use the modern async API (the older `hostCloudAnchor` is deprecated):

```kotlin
suspend fun hostAnchor(
    session: Session,
    pose: Pose,
    ttlDays: Int = 365
): Result<String> = suspendCancellableCoroutine { cont ->
    val anchor = session.createAnchor(pose)
    session.hostCloudAnchorAsync(anchor, ttlDays) { cloudId, state ->
        when (state) {
            CloudAnchorState.SUCCESS ->
                cont.resume(Result.success(cloudId))
            else ->
                cont.resume(Result.failure(IllegalStateException("Host failed: $state")))
        }
    }
}
```

365 days is the maximum free TTL — plenty for a demo.

### 7.2 Resolving

```kotlin
suspend fun resolveAnchor(
    session: Session,
    cloudAnchorId: String
): Result<Anchor> = suspendCancellableCoroutine { cont ->
    session.resolveCloudAnchorAsync(cloudAnchorId) { anchor, state ->
        when (state) {
            CloudAnchorState.SUCCESS ->
                cont.resume(Result.success(anchor))
            else ->
                cont.resume(Result.failure(IllegalStateException("Resolve failed: $state")))
        }
    }
}
```

Resolve takes 2–10 seconds typically. Run all resolves in parallel via `coroutineScope { anchorIds.map { async { resolveAnchor(...) } }.awaitAll() }`. ARCore allows up to 40 simultaneous resolves.

### 7.3 Required user-facing copy

ARCore Cloud Anchors send camera imagery to Google. Show a one-time disclosure: *"This app uses ARCore Cloud Anchors. Visual data from your camera is processed by Google to enable AR features."* Link to Google's privacy policy. This is required by Google's developer policy.

**Exit criteria:** Host an anchor in your building. Force-quit the app. Reopen, paste the saved anchor ID, walk to the same area, watch it resolve and a marker appear at the right physical spot. Repeat in 3 different rooms. If success rate is below ~70%, your environment is too feature-poor (blank walls, glass, mirrors) — either add visual landmarks (posters, tape markers) or switch VPS provider.

---

## 8. Implementation Phase 3 — Mapping Mode

**Goal:** A `MappingScreen` where the user walks through the building and the app builds the graph.

### 8.1 UI elements

- Live AR view (full screen).
- Top bar: building name input, current node count, current tracking quality indicator (`ARCore TrackingState.TRACKING` vs not).
- Bottom bar:
  - "Drop Pin" button (manual anchor)
  - "Tag Room" button (opens dialog → text input → label attached to most recent successful anchor)
  - "Save & Exit" button
- Visual: render a small green sphere `ModelNode` at every successfully hosted anchor so the user can see their breadcrumbs.

### 8.2 Auto-drop logic

In a coroutine that ticks each ARCore frame (subscribe to `onSessionUpdated` callback exposed by SceneView):

```kotlin
val cameraPose = frame.camera.pose
val lastAnchorPose = mostRecentAnchor?.pose
val distance = lastAnchorPose?.let { distance(cameraPose, it) } ?: Float.MAX_VALUE

if (distance > AUTO_DROP_DISTANCE_M
    && frame.camera.trackingState == TrackingState.TRACKING
    && !isHosting) {
    triggerHost(cameraPose)
}
```

Where `AUTO_DROP_DISTANCE_M = 4f` (4 meters). Tune empirically — denser graphs resolve more reliably but cost more time and storage.

`distance(a, b)` is simple Euclidean over `pose.tx/ty/tz`.

### 8.3 Edge creation

Every time a new anchor `N` is successfully hosted while a previous anchor `P` exists, automatically add an edge `P → N` with `distanceMeters = distance(P.pose, N.pose)`. The user is *physically walking* between them, so it's a valid traversable edge by definition.

You may also want a "Connect" mode where the user can manually link two non-adjacent anchors (e.g., two doors of the same hallway encountered in different walks). For demo, skip unless needed.

### 8.4 Tagging

"Tag Room" opens an `AlertDialog` with a `TextField`. On submit, set `mostRecentAnchor.label = entered text`. Re-render that anchor's marker as red so it's distinct from waypoints.

### 8.5 Mapping best practices to enforce in UI

Show on-screen hints during mapping:
- "Walk slowly"
- "Keep camera pointed at textured surfaces (posters, furniture)"
- "Avoid blank walls, glass, mirrors"
- "Loop back through key intersections"

These hints aren't gimmicks — they directly determine resolve success rate.

### 8.6 Save

On "Save & Exit," serialize `MapGraph` to `graphs/<buildingName>.json` via the persistence layer (Phase 4). Navigate back to mode picker.

**Exit criteria:** Walk through ~5 rooms, drop ~15 anchors, tag 5 rooms. JSON file is written to internal storage with valid content.

---

## 9. Implementation Phase 4 — Persistence Layer

Trivial but worth isolating.

```kotlin
class GraphRepository(private val context: Context) {
    private val dir get() = File(context.filesDir, "graphs").apply { mkdirs() }
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun save(graph: MapGraph) {
        File(dir, "${graph.buildingName}.json")
            .writeText(json.encodeToString(graph))
    }

    fun load(buildingName: String): MapGraph? {
        val f = File(dir, "$buildingName.json")
        return if (f.exists()) json.decodeFromString(f.readText()) else null
    }

    fun listBuildings(): List<String> =
        dir.listFiles { f -> f.extension == "json" }
            ?.map { it.nameWithoutExtension }
            .orEmpty()
}
```

For the demo, hardcode `buildingName = "demo"` if you don't want a picker.

---

## 10. Implementation Phase 5 — Navigation Mode

**Goal:** User opens the app anywhere in the mapped building. App resolves anchors, localizes the user, asks for a destination, navigates them there.

### 10.1 Screen flow

`NavigationScreen` is composed of overlapping states:

1. **Loading**: read `graph.json`, start ARCore session.
2. **Localizing**: kick off parallel resolve of *all* anchor IDs. Show "Look around slowly" overlay with a spinner. Stay in this state until at least one anchor resolves with `CloudAnchorState.SUCCESS`.
3. **PickingDestination**: bottom sheet with a list of all rooms (nodes where `label != null`). User taps one.
4. **Navigating**: arrows are rendered (Phase 7). Continuous frame loop advances waypoints (Phase 8).
5. **Arrived**: show "You have arrived" message, button to pick a new destination (back to step 3).

### 10.2 Localization details

Once the first anchor resolves, you have a known node `N_first` with both:
- A pose in the **session frame** (from the resolved `Anchor` object)
- A pose in the **graph frame** (saved during mapping)

These give you a transform `T_session_to_graph`. Apply this to all other saved node positions to know where they are in the current session frame, even before they themselves resolve.

In practice, just use each anchor's own resolved pose when available, and fall back to the transformed saved pose when not.

### 10.3 Continuous resolution

Don't stop after the first resolve. Keep resolving anchors as the user moves and new ones come into view. Each new resolution adds a fresh "ground truth" pose, helping correct accumulated VIO drift (Phase 8).

### 10.4 The user's current node

At any moment, the user's "current node" for pathfinding purposes is the *graph node closest to the camera in the session frame*. Recompute this every few seconds.

**Exit criteria:** Walk into building, app localizes within 15 seconds, user picks a room, arrows guide them there.

---

## 11. Implementation Phase 6 — Pathfinding

The graph is small (tens to maybe a hundred nodes). Dijkstra is fine. A* with Euclidean heuristic is barely more code and can't hurt.

```kotlin
class Pathfinder(private val graph: MapGraph) {
    private val adj: Map<String, List<MapEdge>> =
        graph.edges.flatMap { listOf(it, MapEdge(it.toNodeId, it.fromNodeId, it.distanceMeters)) }
            .groupBy { it.fromNodeId }

    private val nodeById = graph.nodes.associateBy { it.id }

    fun shortestPath(startId: String, goalId: String): List<MapNode>? {
        val dist = mutableMapOf(startId to 0f)
        val prev = mutableMapOf<String, String>()
        val pq = java.util.PriorityQueue<Pair<String, Float>>(compareBy { it.second })
        pq.add(startId to 0f)

        while (pq.isNotEmpty()) {
            val (u, d) = pq.poll()
            if (u == goalId) break
            if (d > (dist[u] ?: Float.MAX_VALUE)) continue
            for (e in adj[u].orEmpty()) {
                val nd = d + e.distanceMeters
                if (nd < (dist[e.toNodeId] ?: Float.MAX_VALUE)) {
                    dist[e.toNodeId] = nd
                    prev[e.toNodeId] = u
                    pq.add(e.toNodeId to nd)
                }
            }
        }

        if (goalId !in prev && startId != goalId) return null
        val path = generateSequence(goalId) { prev[it] }.toList().reversed()
        return path.mapNotNull { nodeById[it] }
    }
}
```

Note edges are made bidirectional (you can walk both ways down a hallway).

**Output:** an ordered list of `MapNode`s from current location to destination. These become the waypoint sequence.

---

## 12. Implementation Phase 7 — AR Arrow Rendering

Two reasonable approaches. Pick one. I recommend **Approach B** for clarity and stability.

### Approach A — Path of arrows

Place a `ModelNode` with `arrow.glb` at every waypoint along the path, ~0.5 m above the floor, oriented toward the next waypoint. The user sees a sequence of fixed arrows in space pointing along the route.

Pros: looks great in screenshots, very obvious.
Cons: requires accurate floor height, all arrows depend on accurate session-frame poses for every waypoint, drift becomes very visible.

### Approach B — Single floating arrow ("nav arrow")

Render one `ModelNode` 1.5 m in front of the camera, locked at a fixed offset relative to the camera each frame. Rotate it so it points toward the *next waypoint*. As the user walks past each waypoint, it auto-advances to point at the one after.

Pros: drift is invisible because the arrow is always camera-relative; only the *direction* needs to be correct, which depends on knowing the next waypoint's session-frame position — which is what localization gives us.
Cons: less spatially impressive, but objectively more reliable.

#### Implementation of Approach B

Each frame:

1. Get camera pose `Pcam`.
2. Compute arrow position: 1.5 m forward of camera, at camera height.
3. Compute target direction: `dir = normalize(nextWaypoint.position - Pcam.translation)`.
4. Project `dir` onto horizontal plane (zero out Y) so the arrow doesn't tilt up/down — keeps it readable.
5. Compute the rotation that maps `+Z` (the arrow's forward axis in its model coordinates) to `dir`.
6. Set `arrowNode.worldPosition` and `arrowNode.worldRotation`.

```kotlin
fun updateArrow(
    camera: Camera,
    nextWaypointPos: Float3,
    arrow: ModelNode
) {
    val camPos = camera.pose.translation.toFloat3()
    val camForward = camera.pose.zAxis.toFloat3() * -1f  // ARCore convention
    val arrowPos = camPos + camForward * 1.5f

    val toTarget = (nextWaypointPos - camPos).copy(y = 0f).normalized()
    val rotation = quaternionLookRotation(toTarget, Float3(0f, 1f, 0f))

    arrow.worldPosition = arrowPos
    arrow.worldRotation = rotation.toEulerAngles()  // SceneView API specifics
}
```

Exact math depends on SceneView's current pose/quaternion utilities — check their docs for `Quaternion.lookRotation` or equivalent. The principle is the same regardless.

### Distance label

Render a small Compose overlay at the bottom showing distance to next waypoint and to final destination. Compute as straight-line distance for the demo.

---

## 13. Implementation Phase 8 — Drift Correction and Waypoint Advancement

### 13.1 Waypoint advancement

Every frame, compute `distance(camera.pose.translation, currentWaypoint.position)`. If under threshold (e.g., 1.2 m), advance:

```kotlin
if (currentWaypointIndex < path.lastIndex) {
    currentWaypointIndex++
} else {
    state = NavState.Arrived
}
```

### 13.2 Drift correction

ARCore's VIO drifts ~1–2% over distance. After 50 m of walking that's a meter of error — enough to point arrows wrong.

Mitigation: keep resolving anchors continuously. Each fresh resolution gives a known-correct pose. Two simple strategies:

- **Naive:** every time an anchor resolves, recompute `T_session_to_graph` from that anchor and overwrite. Simple but jumpy.
- **Smoothed:** maintain a small recent-history of resolved anchors, compute the transform from the closest one, lerp toward it over a few frames.

For the demo, naive is fine. Show a tiny "Re-localized" toast when it happens so you can debug.

### 13.3 Tracking loss handling

If `frame.camera.trackingState != TRACKING`, freeze the arrow and overlay a "Move slowly to recover tracking" message. ARCore will recover automatically once it sees enough features again.

---

## 14. Testing and Tuning

### 14.1 Test path

1. Map building.
2. Force-quit app.
3. Walk to far corner.
4. Open app in nav mode.
5. Confirm localization within 15 s.
6. Pick a destination 3 rooms away.
7. Follow arrows. Confirm arrival.
8. From arrival point, pick another destination.
9. Repeat from steps 2 and 4 across multiple sessions, lighting conditions, times of day.

### 14.2 Tuning knobs

| Parameter | Start | If too low | If too high |
|---|---|---|---|
| `AUTO_DROP_DISTANCE_M` | 4.0 | localization fails in middle of hallways | huge graph, slow saves |
| Resolve timeout | 15 s | gives up before resolving | user waits forever |
| Waypoint advance distance | 1.2 m | arrow flickers between waypoints | overshoots destinations |
| Arrow forward offset | 1.5 m | arrow clips into walls | arrow feels detached |
| Drift correction lerp | naive | jittery | slow correction |

### 14.3 Diagnostics overlay (debug build only)

Show in a corner: tracking state, FPS, current frame's feature point count, last resolve latency, current node ID, distance to next waypoint, transform residual after relocalization. Invaluable when something breaks in the field.

---

## 15. Known Gotchas and Mitigations

**Cold-start localization can take 5–15 s.** Give the user a clear "Look around slowly" prompt. Don't gate on ARCore's `TRACKING` state alone — wait for the first cloud anchor resolution.

**Multi-floor buildings.** Cloud Anchors don't reliably distinguish altitude across floors that look similar. For demo: treat each floor as a separate graph file and let the user pick. For more polish: read `SensorManager.TYPE_PRESSURE` and detect floor changes by pressure delta.

**Lighting changes between mapping and navigation.** Resolves degrade significantly. Map under realistic lighting. If the building has both daytime and evening conditions, map twice (two graph files) or map under whichever you'll demo in.

**Feature-poor environments.** Long blank corridors, glass walls, mirrors — these break ARCore. Mitigations: stick high-contrast posters or printed AprilTags every 5–10 m before mapping. Yes, it feels like cheating; yes, every commercial indoor AR product does this.

**Cloud Anchor TTL.** 365 days max on the free tier. If the demo will be used long-term, schedule a remap.

**ARCore Geospatial API confusion.** ARCore now also offers a "Geospatial" API that uses Google's Visual Positioning Service (Street View imagery). This works *outdoors* and in some indoor spaces with VPS coverage. It's *not* the same as Cloud Anchors. For an arbitrary indoor building with no VPS coverage, stick to Cloud Anchors.

**Battery and thermals.** Continuous AR sessions heat the phone fast and drain battery. Demo should aim for under 10-minute continuous sessions; stop the AR session when navigating to a non-AR screen.

**SceneView API churn.** SceneView is actively developed and APIs change between versions. Pin a specific version in `build.gradle.kts` and don't auto-bump. Their GitHub README has working sample apps for the version you pick — read those, not stale tutorials.

**The free Cloud Anchor service has request quotas.** For one developer one demo this is fine, but if you set up a CI loop that hosts/resolves repeatedly you'll hit limits.

**Privacy disclosure is required.** Cloud Anchors send camera frames to Google. You must inform users. Google may reject the app at review time without this — not relevant for a demo APK but worth knowing.

---

## 16. Build Schedule

A realistic two-week schedule for one developer working full-time:

| Day | Phase | Deliverable |
|---|---|---|
| 1 | 1 | Project created, dependencies pulled, app builds and launches. Camera permission flow works. |
| 2 | 1 | ARSceneView renders camera feed. Debug cube anchors in space. |
| 3 | 2 | Cloud Anchor host succeeds, ID logged. |
| 4 | 2 | Resolve in same session works. Resolve across app restart works. **Validate in target building** — this is the go/no-go moment. |
| 5 | 3 | Mapping screen UI. Manual Drop Pin button works. Markers render on hosted anchors. |
| 6 | 3 | Auto-drop based on distance. Tag Room dialog. Edge auto-creation between sequential anchors. |
| 7 | 4 | Save/load JSON. Survives app restart. |
| 8 | 5 | Navigation screen skeleton. Parallel resolve. Localization-ready state. |
| 9 | 6 | Pathfinder implementation and unit-tested with synthetic graphs. |
| 10 | 7 | Single floating arrow renders and points at hardcoded target. |
| 11 | 7+8 | Wire arrow to current waypoint. Waypoint advancement. End-to-end nav works on a 3-anchor test path. |
| 12 | 8 | Drift correction. Tracking loss handling. Diagnostics overlay. |
| 13 | 14 | Walk-test. Tune parameters. Fix top 3 bugs found. |
| 14 | 14 | Second walk-test under different conditions. Demo-ready. |

**Critical milestone:** end of Day 4. If Cloud Anchors don't resolve reliably in your specific building by then, you have a forking decision: add visual landmarks, switch to Immersal/another VPS, or accept lower reliability. Don't push past Day 4 without resolving this.

---

## 17. Stretch Goals (Optional)

Only after the core flow works:

- **Multi-floor support** with barometer-based floor detection.
- **Voice prompts** ("In 5 meters, turn left") via Android `TextToSpeech`.
- **Mini-map overlay** showing the graph and current position from above.
- **Re-mapping mode** that adds nodes to an existing graph instead of starting fresh.
- **Multi-user sync** by uploading the JSON graph to Firebase. Cloud Anchor IDs are already shareable.
- **Accessibility:** larger arrow, haptic pulse on waypoint reached, audio beacon at destination.

These are all nice but none are required to satisfy the original spec. Keep scope tight.

---

## Appendix A — One-screen flow recap

```
MAPPING                          NAVIGATION
-------                          ----------
open app                         open app
pick "Map a building"            pick "Navigate"
type building name               (graph auto-loaded)
walk into entrance               walk into building (anywhere)
ARCore tracks                    ARCore tracks
auto-drops anchor every 4 m      resolves all anchors in parallel
  -> hosts cloud anchor          first anchor resolves -> localized
  -> adds node + edge            user picks destination from list
user taps "Tag Room"             A* finds path
  -> labels current node         floating arrow points at next waypoint
repeat until building covered    user walks; arrow updates each frame
tap "Save & Exit"                waypoint advances when reached
graph.json written               continuous re-resolution corrects drift
                                 arrival -> "You arrived" -> pick another
```

---

## Appendix B — File checklist

Before declaring the demo done, confirm these exist and work:

- [ ] `MainActivity.kt` with Compose nav between mode picker, mapping, navigation
- [ ] `ArSessionConfig.kt` enabling `CloudAnchorMode.ENABLED`
- [ ] `CloudAnchorHelper.kt` with suspend `hostAnchor` and `resolveAnchor`
- [ ] `MappingScreen.kt` + `MappingViewModel.kt` with auto-drop, manual drop, tag, save
- [ ] `NavigationScreen.kt` + `NavigationViewModel.kt` with parallel resolve, picker, navigation loop
- [ ] `MapGraph.kt` with `@Serializable` classes
- [ ] `GraphRepository.kt` with save/load
- [ ] `Pathfinder.kt` with Dijkstra
- [ ] `assets/arrow.glb` present
- [ ] `AndroidManifest.xml` with CAMERA permission and ARCore meta-data
- [ ] One Cloud Anchor disclosure dialog shown on first run
- [ ] Diagnostics overlay (debug build)
