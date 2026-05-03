# Vectura AI — AGENTS.md

## Project Overview

Vectura AI is an AR indoor navigation app built with KMP (Kotlin Multiplatform) for shared logic and native Swift/ARKit for the iOS AR experience. The project uses a preprocessor pipeline to convert 3D scans into navigation graphs.

## Phase 11 — Client-Facing Polish (2026-04-13)

### What Changed

Phase 11 polishes the visitor-facing screens for client demos. No mechanics, data models, or AR rendering are changed.

1. **AR overlay redesign**: Active navigation now shows a "next action" guidance card ("Turn left ahead", "Continue straight", "You're almost there") derived from the existing `ArrowPlacementData` turn markers. Progress strip shows remaining distance and walking ETA. Tracking confidence badge replaces raw ARKit quality strings with user-friendly labels ("Tracking", "Hold steady", "Re-centering...").
2. **Haptic feedback**: `HapticManager` singleton fires four haptic events: route start (medium impact), imminent turn (warning notification within 2m), re-centering (light impact on tracking degradation), arrival (success notification). Gated by `isEnabled` flag.
3. **Home screen polish**: Stronger visual hierarchy with gradient CTA button, value subtitle ("Find your way indoors"), admin tools moved to top-right gear icon to separate it from the visitor flow.
4. **Destination selection**: Rooms grouped by category with section headers. Richer room cards showing descriptions and category badge pills. Flat list when searching.
5. **Route preview**: Walking time estimate (distance / 1.2 m/s), "Walking" route type badge, polished summary card, gradient CTA.
6. **Microcopy pass**: All user-facing strings updated from technical wording to calmer product language. Examples: "Detecting entrance poster..." → "Scanning...", "Follow the arrows" → "Follow the path", "Navigation Data Unavailable" → "Unable to load navigation data".
7. **VecturTheme**: Lightweight style namespace with shared gradient, card background, and `vecturPrimaryButton()` modifier for consistent button styling.
8. **ADR-033**: Documents the polish decisions and constraints.

### New Files

- `apps/iosApp/iosApp/VecturTheme.swift` — centralized style constants
- `apps/iosApp/iosApp/HapticManager.swift` — haptic feedback singleton
- `docs/adr/ADR-033-client-facing-polish.md`

### Modified Files

- `apps/iosApp/iosApp/ar/ARNavigationView.swift` — overlay redesign, next-action logic, haptic calls, microcopy
- `apps/iosApp/iosApp/ContentView.swift` — home screen layout, microcopy
- `apps/iosApp/iosApp/DestinationSelectView.swift` — category grouping, richer cards, microcopy
- `apps/iosApp/iosApp/RoutePreviewView.swift` — walking time, compact card, microcopy

### Verification

Manual checklist:
1. Home screen: gradient CTA, gear icon for admin, value subtitle
2. Destination list: category sections, descriptions, badges
3. Route preview: walking time, route type, polished card
4. AR overlay: next-action card shows "Continue straight" / "Turn left ahead"
5. AR overlay: tracking badge shows "Tracking" / "Hold steady" / "Re-centering..."
6. AR overlay: progress strip with remaining distance and ETA
7. Arrival: "You've reached {dest}" with spring animation
8. Haptics: fire on alignment lock, near turn, re-centering, arrival
9. Visitor flow still works end-to-end
10. Admin tools still accessible via gear icon

### Known Limitations After Phase 11

- Microcopy is English-only; no localization framework
- No voice guidance / TTS
- No sound effects
- No custom 3D arrow models
- Haptics are iOS-only (no Android)

### Intentionally Deferred (Post Phase 11)

- Localization / i18n framework
- Full design system with dark mode
- Voice guidance / TTS
- Sound effects system
- Custom AR arrow models
- Android parity

## Phase 10 — Async Upload + Timeout Resilience (2026-04-13)

### What Changed

Phase 10 makes the admin draft upload resilient against GLB files that take a long time to process. The fix decouples the HTTP upload response from the preprocessing pipeline so the iOS client never experiences a timeout waiting for a slow pipeline to finish.

1. **Upload is already asynchronous**: `POST /admin/draft-jobs` creates the job directory, writes `input.glb`, and returns `{ status: "queued" }` within milliseconds. Draft preprocessing runs in an in-process background coroutine (`CoroutineScope(Dispatchers.IO).launch`) and is never part of the HTTP request lifecycle.

2. **Job lifecycle** (`queued → processing → succeeded/failed`): Status transitions are persisted to `job.json` as they occur. If the pipeline errors or throws, status becomes `failed` with a readable `errorMessage`; the job is never left stuck in `queued` or `processing`.

3. **iOS client polls, never blocks**: After upload the app receives `queued` and navigates to the job detail view, which polls `GET /admin/draft-jobs/{id}` every 2 seconds until the job reaches a terminal state. The jobs list view (`AdminDraftJobsView`) also auto-polls every 3 seconds while any job is active, so status updates are visible without manual refresh.

4. **Backend tests** (`AsyncJobLifecycleTest`): 5 new tests verify:
   - `POST /admin/draft-jobs` returns `queued` within 5 s (never `succeeded`/`failed`)
   - `createJob` persists `queued` before any pipeline work
   - `runDraftGeneration` transitions to `failed` with a persisted error message for an invalid GLB
   - `runDraftGeneration` throws (rather than silently succeeding) for a nonexistent job

5. **Verification script updated**: `scripts/verify-admin-draft-api.sh` now uses `curl --max-time 10` for the upload step and reports a failure if the server does not respond within 10 s, proving the pipeline does not block the response.

### Async Upload Contract

```
iOS Client                    Admin API (Ktor)               Background (Dispatchers.IO)
    │                               │                                  │
    │  POST /admin/draft-jobs       │                                  │
    │  (multipart .glb)             │                                  │
    │ ───────────────────────────►  │                                  │
    │                               │  createJob() → job.json (queued) │
    │  ◄─── 201 { status: queued }  │                                  │
    │                               │  launch { runDraftGeneration() } ──►
    │  (polls GET every 2–3 s)      │                                  │
    │                               │                      queued → processing
    │                               │                      pipeline.execute()
    │                               │                      processing → succeeded/failed
    │  GET /admin/draft-jobs/{id}   │                                  │
    │ ───────────────────────────►  │  read job.json                   │
    │  ◄─── { status: succeeded }   │                                  │
```

### Known Limitations After Phase 10

- Same as Phase 9 — no database, no queue, no auth, single in-process worker

### Intentionally Deferred (Post Phase 10)

- Concurrent pipeline execution (currently one-at-a-time by JVM thread pool)
- Package activation / runtime switching
- Android admin UI
- Authentication
- Cloud deployment

## Phase 9 — Admin Room Editing + Reviewed Package Export (2026-03-30)

### What Changed

Phase 9 adds per-room metadata editing and reviewed package export to the admin draft pipeline. Admins can now rename rooms, set categories and descriptions, and export a 5-file reviewed package without touching the generated draft artifacts.

1. **Room override persistence** (`RoomOverrideService`): Patches are stored in `<jobDir>/room_overrides.json` — a separate file that is never written by the pipeline. The generated `authoring_config.generated.json` is always read-only.
2. **PATCH room endpoint**: `PATCH /admin/draft-jobs/{jobId}/rooms/{roomId}` — accepts `{ displayName?, category?, description? }`, validates the room ID against the draft config, rejects blank display names, and writes partial updates (existing fields preserved).
3. **Summary merges overrides**: `GET /admin/draft-jobs/{jobId}/summary` now merges `room_overrides.json` into the room list before returning, so the iOS review screen always shows the current edited state.
4. **Reviewed package exporter** (`ReviewedPackageExporter`): Produces all 5 reviewed-package files in `<jobDir>/reviewed-package/`: `manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, `route_rendering.json`. Nodes/edges/markers are copied verbatim from the draft. Room metadata is merged from overrides.
5. **Export endpoint**: `POST /admin/draft-jobs/{jobId}/export-reviewed-package` — returns `{ jobId, status, files[], exportPath, warnings[] }`.
6. **Package listing + content endpoints**: `GET /admin/draft-jobs/{jobId}/reviewed-package` lists exported files; `GET /admin/draft-jobs/{jobId}/reviewed-package/{filename}/content` serves the JSON. Path traversal is blocked.
7. **iOS room edit sheet** (`RoomEditSheet`): Tapping a room row in `AdminDraftReviewView` opens a form sheet with editable display name, category picker, and description field. Saves via `PATCH` and reloads the summary on success.
8. **iOS export action**: "Export Reviewed Package" button in the review screen calls `POST export-reviewed-package`, shows progress, and lists the exported files on success.
9. **iOS client methods**: `patchRoom()`, `exportReviewedPackage()`, `getExportedPackageFiles()`, `reviewedPackageFileContentURL()` added to `AdminAPIClient`. `RoomOverrideResponse` and `ExportResultResponse` models added.
10. **Backend tests**: `RoomOverrideServiceTest` (8 tests), `ReviewedPackageExporterTest` (8 tests), `RoomEditAndExportRoutesTest` (11 integration tests).
11. **Verification script**: `scripts/verify-admin-room-edit-and-export.sh` — uploads GLB, patches room, verifies summary reflects override, exports package, verifies all 5 files and edited values in rooms.json.
12. **ADR-032**: Documents the override-not-mutation pattern and why package activation is deferred.

### Extended API (Phase 9 additions)

```
PATCH /admin/draft-jobs/{jobId}/rooms/{roomId}
    Body: { displayName?, category?, description? }
    → RoomOverride { displayName, category, description, updatedAt }
    → 404 if job not found, 400 if roomId unknown or displayName blank

POST /admin/draft-jobs/{jobId}/export-reviewed-package
    → ExportResult { jobId, status, files[], exportPath, warnings[] }
    → 200 on success, 422 on failure, 404 if job not found

GET /admin/draft-jobs/{jobId}/reviewed-package
    → [filename, …]
    → 404 if not yet exported

GET /admin/draft-jobs/{jobId}/reviewed-package/{filename}/content
    → raw JSON file content
    → 404/400 for unknown filename or path traversal
```

### Running Room Edit + Export

```bash
# Start admin API (if not already running)
./gradlew :tools:admin-api:run

# Verify Phase 3 end-to-end
./scripts/verify-admin-room-edit-and-export.sh

# Or auto-start server
./scripts/verify-admin-room-edit-and-export.sh --start-server

# Run all backend tests (Phase 1 + Phase 2 + Phase 3)
./gradlew :tools:admin-api:test
```

### iOS ATS / HTTP Dev Networking

The admin API uses plain HTTP. iOS ATS blocks HTTP by default. `Info.plist` carries two targeted exceptions (no `NSAllowsArbitraryLoads`):
- `NSAllowsLocalNetworking: true` — covers simulator (`localhost` / `127.0.0.1`)
- `NSExceptionDomains` entry for the Mac's current LAN IP — covers real-device testing

When your Mac's IP changes, update **both** the `NSExceptionDomains` key in `Info.plist` **and** `AdminAPIClient.baseURL`.

### Known Limitations After Phase 9

- Exported reviewed package is not automatically activated — it must be manually copied to the iOS bundle
- No package activation / runtime switching in this phase
- No Android admin UI
- No multi-floor or multi-building support
- Authentication and cloud deployment deferred

### Intentionally Deferred (Post Phase 9)

- Package activation / runtime switching from admin UI
- Android admin UI
- Authentication
- Cloud deployment

## Phase 8 — Admin Draft Review — Read-Only (2026-03-30)

### What Changed

Phase 8 adds read-only review of succeeded draft jobs from within the iOS admin UI. Admins can now inspect the generated draft before any editing or export step. This phase is strictly inspection only — no editing, no package activation.

1. **Summary endpoint**: `GET /admin/draft-jobs/{jobId}/summary` — parses `authoring_config.generated.json`, `generation_metadata.json`, and `geometry_stats.json` from the job's output directory and returns a structured JSON summary. Fails gracefully if artifacts are missing.
2. **Artifact content endpoint**: `GET /admin/draft-jobs/{jobId}/artifacts/{artifactName}/content` — serves the raw SVG or JSON artifact file with correct content type. Path traversal is blocked by validating against the job's known artifact list.
3. **Summary data model** (`DraftSummary`): includes `buildingId`, `buildingName`, `floorId`, `artifactAvailability`, `counts` (nodes/edges/rooms/markers), `rooms[]`, `generationMetadata`, `geometryStats`, and `warnings[]`.
4. **iOS review screen** (`AdminDraftReviewView`): accessible via "Review Draft" on the job detail screen (succeeded jobs only). Shows building metadata, counts, generation stats, a segmented Plan/Graph SVG preview via `WKWebView`, and the room candidate list.
5. **Room candidate rows** (`DraftRoomRow`): read-only rows with display name, category badge, destination node ID, and description. Structured for future editing evolution.
6. **iOS client methods**: `getDraftSummary(jobId:)` and `getArtifactContent(jobId:artifactName:)` added to `AdminAPIClient`. Response models (`DraftSummaryResponse`, `DraftRoomResponse`, etc.) added to the same file.
7. **Backend tests**: `DraftSummaryExtractorTest` (8 tests covering parsing, missing files, warnings, artifact availability) and `SummaryAndArtifactRoutesTest` (12 tests covering HTTP responses, path traversal, missing jobs).
8. **Verification script**: `scripts/verify-admin-draft-review.sh` — uploads sample GLB, waits for success, verifies summary endpoint, checks SVG content, tests path traversal rejection.
9. **ADR-031**: Documents why Phase 2 is read-only and why draft review is separated from runtime package activation.

### Extended API (Phase 8 additions)

```
GET /admin/draft-jobs/{jobId}/summary
    → DraftSummary { jobId, buildingId, buildingName, floorId,
                     artifactAvailability, counts, rooms[],
                     generationMetadata, geometryStats, warnings[] }

GET /admin/draft-jobs/{jobId}/artifacts/{artifactName}/content
    → SVG / JSON raw file content
    → 404 if artifact not in job's known list (path traversal protection)
```

### Running the Admin Draft Review

```bash
# Start admin API (if not already running)
./gradlew :tools:admin-api:run

# Verify Phase 2 end-to-end
./scripts/verify-admin-draft-review.sh

# Or auto-start server
./scripts/verify-admin-draft-review.sh --start-server

# Run all backend tests (Phase 1 + Phase 2)
./gradlew :tools:admin-api:test
```

### Known Limitations After Phase 8

- Draft review is read-only — no editing, renaming, or category changes (added in Phase 9)
- SVG previews require the admin server to be running; they load from `localhost:8080` by default
- No Android admin UI
- No multi-floor or multi-building support

### Intentionally Deferred (Post Phase 8)

- Room name / category editing → Done in Phase 9
- Reviewed package export from admin UI → Done in Phase 9
- Package activation / runtime switching
- Android admin UI
- Authentication
- Cloud deployment

## Phase 7 — Admin Draft-Ingestion Pipeline (2026-03-29)

### What Changed

Phase 7 adds a dev-only admin pipeline for uploading Polycam GLB scans and generating draft navigation packages, with visibility from the iOS app. This is the first step toward assisted authoring from within the app.

1. **Admin API backend**: `tools/admin-api/` — Ktor/JVM HTTP server that accepts `.glb` uploads, runs the existing `DraftPipeline` from `tools/nav-preprocessor`, and exposes job status and artifacts via REST endpoints. Filesystem-backed, no database.
2. **Draft job lifecycle**: Jobs transition through `queued → processing → succeeded/failed`. Each job directory (`build/admin-draft-jobs/<jobId>/`) contains `job.json`, `input.glb`, and `output/` with generated artifacts.
3. **API endpoints**: `POST /admin/draft-jobs` (upload), `GET /admin/draft-jobs` (list), `GET /admin/draft-jobs/{id}` (detail), `GET /admin/draft-jobs/{id}/artifacts` (artifact list).
4. **iOS admin UI**: "Admin Tools" button on home screen opens a sheet with GLB file picker, upload action, job list with status indicators, and per-job detail view showing artifacts and errors. Completely isolated from the visitor navigation flow.
5. **iOS admin API client**: `AdminAPIClient` with configurable `baseURL` (defaults to `localhost:8080` for simulator, overridable for LAN device testing). Isolated from `BuildingPackageLoader`.
6. **Verification script**: `scripts/verify-admin-draft-api.sh` — uploads sample GLB, polls status, verifies artifacts, tests error rejection.
7. **Backend tests**: Unit tests for job service (persistence, status transitions) and route tests (upload validation, error responses).
8. **ADR-030**: Documents the decision for dev-only, filesystem-backed, minimal pipeline.

### Admin API Architecture

```
iOS App (Admin Tools)          Admin API (Ktor)           nav-preprocessor
       │                            │                           │
       │  POST /admin/draft-jobs    │                           │
       │  (multipart .glb upload)   │                           │
       │ ────────────────────────►  │                           │
       │                            │  DraftPipeline.execute()  │
       │                            │ ────────────────────────► │
       │                            │                           │
       │                            │  ◄──── exit code + files  │
       │                            │                           │
       │  GET /admin/draft-jobs/{id}│                           │
       │ ────────────────────────►  │                           │
       │  ◄──── job status + artifacts                          │
```

### Running the Admin Pipeline

```bash
# Start admin API
./gradlew :tools:admin-api:run

# Verify end-to-end
./scripts/verify-admin-draft-api.sh

# Or auto-start server
./scripts/verify-admin-draft-api.sh --start-server
```

### Known Limitations After Phase 7

- Admin pipeline is dev-only (no auth, no cloud deployment)
- Filesystem persistence does not scale
- Synchronous processing blocks during draft generation
- No room editing, map preview, or package export from admin UI
- Admin UI does not modify the bundled reviewed package
- No Android admin UI

### Intentionally Deferred (Post Phase 7)

- 2D map preview rendering in admin UI
- Room name/category editing
- Reviewed package export/activation from admin
- Runtime package switching
- Android admin UI
- Authentication
- Cloud deployment

## Phase 6 — Unified Entrance Poster & AR Deadlock Fix (2026-03-23)

### What Changed

Phase 6 fixes the real-device AR alignment deadlock caused by a QR/marker contract mismatch. The root cause: the system required two separate physical artifacts (a QR code poster AND a matching AR reference image poster) but the docs/UX implied one. Users printed only the QR code; the bundled `entrance_marker.png` was a placeholder that never matched anything physical, so ARKit never detected it → infinite "Waiting for Marker" loop.

1. **Unified entrance poster contract**: One physical artifact serves as both the QR code (scanned by AVCaptureSession) and the AR reference image (detected by ARKit). No hidden two-marker requirement.
2. **Poster generation script**: `scripts/generate-entrance-poster.sh` — generates the combined entrance poster with embedded QR code and copies it into the Xcode asset catalog, ensuring the printed poster and bundled reference image are identical.
3. **Diagnostic logging**: `ARSessionManager`, `ARMarkerDetector`, and `AnchorDetectionDelegate` now log: whether the AR session starts, how many reference images are loaded, their names and physical sizes, how many image anchor candidates ARKit delivers, which are accepted/rejected and why.
4. **Detection failure categorization**: `ARMarkerDetector` tracks candidates seen vs rejected and exposes `DetectionFailureReason` with three categories: `noCandidatesSeen` (poster not in view or doesn't match), `candidatesRejected` (images seen but name mismatch), `assetMissing` (reference image not in bundle).
5. **Accurate UI copy**: Pre-alignment overlay now says "Point the camera at the entrance poster" and "Hold steady at the same poster you scanned" instead of misleading "entrance QR".
6. **Categorized timeout messages**: When alignment times out, the UI shows the specific reason (no poster detected / poster mismatch / asset missing) with actionable hints instead of generic "Marker not detected".
7. **Tightened asset validation**: Pre-start validation checks the full chain (reviewed package `referenceImageName` → asset catalog) before starting the AR session. Surfaces specific errors before the user waits.
8. **Poster source consistency check**: `check-demo-readiness.sh` now verifies that `sample/entrance-poster/entrance_poster.png` matches the asset catalog image, and warns about placeholder-sized images.
9. **Updated docs**: Demo operator guide, QR payload contract, and this file all describe the single-poster contract. No more ambiguous "QR code and AR marker" phrasing.
10. **Fixed decompression bomb regression**: The `qrencode` CLI fallback in `generate-entrance-poster.sh` was using `--size=1050` which sets **module size** (not image size), producing a 64,050×64,050px image (~16 GB uncompressed). This caused Xcode and SwiftUI Preview to consume tens of GB of RAM. Fixed the script and added a safety cap (max 4000px) that auto-resizes oversized images. Added pixel dimension checks to `check-demo-readiness.sh`.

### Demo Architecture (Phase 6)

```
                    ┌─────────────────────────┐
                    │   Entrance Poster        │
                    │   (single physical item) │
                    │   ┌─────────────────┐   │
                    │   │  QR code with    │   │
                    │   │  JSON payload    │   │
                    │   └─────────────────┘   │
                    │   + Vectura AI branding    │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                                      ▼
   AVCaptureSession scanner              ARKit reference image detection
   reads QR → validates payload          matches full poster image
              │                                      │
              ▼                                      ▼
   Entrance confirmed                    Alignment locked
              │                                      │
              └──────────────┬───────────────────────┘
                             ▼
                Rolling lookahead navigation
                (arrows, fade, arrival)
```

### Entrance Poster Workflow

1. Run `./scripts/generate-entrance-poster.sh` — generates poster + copies to asset catalog
2. Print `sample/entrance-poster/entrance_poster.png` at exactly 21×21cm on matte paper
3. Place at eye level (~1.2m) at building entrance
4. Rebuild the iOS app in Xcode
5. Run `./scripts/check-demo-readiness.sh` — all checks must pass

### Demo Operator Flow

1. Generate + print entrance poster: `./scripts/generate-entrance-poster.sh`
2. Place at entrance, eye level, good lighting
3. Run `./scripts/check-demo-readiness.sh` — all checks must pass
4. Build and install on device
5. Demo: Home → Scan poster QR → Confirm Entrance → Select Destination → Route Preview → AR Navigation (point at same poster) → Arrival
6. Recommended order: Mutfak (shortest) → Salon (medium) → Yatak Odası (longest)

### Known Limitations After Phase 6

- Single entrance only (Entrance A)
- Single floor only
- Node positions require manual calibration per physical space
- No checkpoint marker tracking (single entrance marker only)
- No runtime package download — reviewed package is bundled
- No Android support
- No multi-building support

### Build Safety Notes

**Image asset size limit**: AR reference images in the asset catalog must be ≤ 4000×4000px. Larger images cause Xcode and SwiftUI Preview to allocate tens of GB of RAM (the image is decompressed to width × height × 4 bytes). The `generate-entrance-poster.sh` script has a safety cap, and `check-demo-readiness.sh` checks pixel dimensions.

**qrencode `--size` flag**: The `--size` / `-s` flag in `qrencode` sets **module pixel size**, NOT total image size. Never pass the target image width as `--size`. A QR code with module size 1050 produces a ~60,000px image.

**SwiftUI Preview safety**: Do NOT add `#Preview` macros to views that use ARKit (`ARNavigationView`) or AVCaptureSession (`QRScanView`). These require hardware not available in the Preview canvas and can cause resource exhaustion. The only preview is on `ContentView`, which shows the lightweight Home screen.

### Intentionally Postponed (Post-MVP)

- Checkpoint marker tracking along the route
- Multi-entrance / multi-floor support
- AR rendering redesign (custom 3D arrow models)
- Android parity
- Full authoring UI/editor
- Runtime package download/update
- Designed/branded entrance poster

## Phase 5 — Demo-Ready Single-House iOS MVP (2026-03-23)

### What Changed

Phase 5 makes the app demo-ready on a real iOS device with strict marker validation, AR asset integration, and comprehensive tooling.

1. **Real AR reference image asset**: Added `entrance_marker_main.arreferenceimage` to the Xcode asset catalog (`Assets.xcassets/AR Resources.arresourcegroup/`) with a 21×21cm reference image and `Contents.json` specifying `physicalWidth: 0.21`.
2. **Strict marker detection**: Removed the "any image anchor" fallback from `ARMarkerDetector`. Only images matching a registered `knownMarkers` entry are accepted. Unknown images are logged and rejected.
3. **Asset validation at session start**: `ARSessionManager.startSession()` verifies the expected reference image exists in the AR Resources group before configuring the session. Missing asset → `onMarkerAssetMissing` callback → UI shows configuration error. Simulator skips this check.
4. **Configuration error overlay**: `ARNavigationView` shows a dedicated error overlay if the marker asset is missing, with a "Return Home" action. No silent failure.
5. **Demo readiness validator**: `scripts/check-demo-readiness.sh` — 7-section comprehensive check (reviewed package, bundle consistency, AR assets, naming, QR contract, route connectivity, Xcode project).
6. **Regression checks**: `scripts/regression-checks.sh` — 4-section fast pre-commit checks (QR parsing, package loading, route existence, marker consistency).
7. **Calibration guide**: `docs/calibration-guide.md` — documents the building-local coordinate system and step-by-step calibration procedure for matching nav_graph coordinates to a physical space.
8. **Demo operator guide**: `docs/demo-operator-guide.md` — complete demo-day procedure: entrance poster printing, placement, demo flow, troubleshooting, quick reference.

### Calibration Workflow

1. Run `./scripts/generate-entrance-poster.sh` to generate entrance poster
2. Print at 21×21cm on matte paper
3. Place at eye level (~1.2m) at building entrance
4. Measure corridor junctions and door positions from marker origin
5. Update `nav_graph.json` with measured coordinates (building-local: +X right, +Y up, -Z forward)
6. Update edge costs with walking distances
7. Run `./scripts/validate-reviewed-package.sh sample/reviewed-house-package/`
8. Copy to bundle: `cp sample/reviewed-house-package/*.json apps/iosApp/iosApp/reviewed-package/`
9. Run `./scripts/check-demo-readiness.sh`
10. Build, deploy, test all three routes

### Known Limitations After MVP

- Single entrance only (Entrance A)
- Single floor only
- Node positions require manual calibration per physical space
- No checkpoint marker tracking (single entrance marker only)
- No runtime package download — reviewed package is bundled
- No Android support
- No multi-building support

### Intentionally Postponed (Post-MVP)

- Checkpoint marker tracking along the route
- Multi-entrance / multi-floor support
- AR rendering redesign (custom 3D arrow models)
- Android parity
- Full authoring UI/editor
- Runtime package download/update

## Phase 4 — Real QR Scanning and Entrance-Marker AR Alignment (2026-03-23)

### What Changed

Phase 4 replaces the simulated QR scan with a real iOS camera-backed scanner and grounds the initial AR alignment in the entrance marker metadata from the reviewed package.

1. **Real QR scanner**: `QRScanView` now uses `AVCaptureSession` with `AVCaptureMetadataOutput` on device. Simulator keeps a "Simulate Entrance Scan" fallback button.
2. **QR payload contract**: Explicit JSON format: `{"type":"Vectura AI-entrance","buildingId":"house-demo-01","entranceId":"marker-entrance-a","v":1}`. Decoded by `QRPayload.swift`.
3. **Payload validation**: QR `buildingId` is checked against `manifest.json`; `entranceId` is checked against `entrance_markers.json`. Invalid/mismatched QR shows a clear error with retry.
4. **Validated entrance marker through the flow**: `NavigationFlowModel.validatedEntranceMarker` carries the reviewed-package marker from QR confirmation through to AR startup. No hardcoded marker metadata.
5. **Alignment-gated AR rendering**: Navigation arrows are not placed until the entrance marker is detected. Pre-alignment overlay: "Point the camera at the entrance QR."
6. **Alignment timeout and retry**: If the marker is not detected within 30 seconds, the overlay shows a retry/cancel pair. No silent fallback to invented alignment.
7. **Alignment from marker metadata**: `ARNavigationViewModel.configure(with:entranceMarker:)` reads `physicalWidthMeters`, `referenceImageName`, `position`, and `rotationYDegrees` directly from the reviewed package marker.
8. **Post-alignment behavior unchanged**: Phase 3 rolling lookahead, fade-behind, and distance-based arrival all continue to work after alignment is locked.
9. **Validator extended**: `validate-reviewed-package.sh` now checks entrance marker metadata completeness (physical dimensions, position coords, referenceImageName, forwardBasis) and building id presence for QR contract consistency.

### Real QR Scan Flow

1. User taps "Scan QR Code" on home screen.
2. `QRScanView` opens live camera via `AVCaptureSession`.
3. First QR code detected → `QRPayload.parse()` decodes the JSON.
4. `NavigationFlowModel.validateQRPayload()` checks building + entrance match.
5. On success → `confirmEntrance(fromPayload:)` looks up the entrance marker and stores it in `validatedEntranceMarker`.
6. Flow proceeds: entrance confirmed sheet → destination select → route preview → AR.

### Entrance-Marker-Based AR Alignment

1. `ARNavigationView.onAppear` calls `configure(with:entranceMarker:)` — reads all marker metadata.
2. `startSession()` passes `referenceImageName` and `physicalWidthMeters` to `ARSessionManager`.
3. ARKit detects the physical marker → `ARMarkerDetector.processAnchor()` fires.
4. `handleMarkerDetected()` computes the building-local → AR-world transform from the marker pose.
5. Arrows are placed and Phase 3 rendering begins.

### Alignment Failure / Retry

- 30-second timeout → overlay shows "Marker not detected" with Retry and Cancel.
- Retry resets `ARMarkerDetector` and restarts the AR session.
- Cancel returns to the previous flow screen.
- No navigation arrows are ever rendered in an unaligned state.

### Intentionally Postponed to Phase 5

- Checkpoint marker tracking
- Multi-entrance support
- AR rendering redesign (custom 3D arrow models)
- Android parity
- Full authoring UI/editor
- Runtime package download/update

## Phase 3 — Rolling Lookahead AR Guidance (2026-03-22)

### What Changed

Phase 3 makes AR guidance behave like real step-by-step navigation instead of a static carpet of arrows.

1. **Rolling lookahead**: Only arrows within a forward window (8m by default from `route_rendering.json`) are visible. As the user walks, the visible window slides forward.
2. **Fade-behind**: Passed arrows fade out (opacity 1.0→0.2, slight scale shrink) before disappearing, so the user sees a smooth transition.
3. **Distance-based arrival**: Arrival triggers when the user is within 1.5m of the destination door node (from `destinationThresholdMeters` in `route_rendering.json`), not based on progress percentage.
4. **Arrival message**: Shows "You've arrived at [Room Name]" (e.g., "You've arrived at Mutfak").
5. **Cumulative distance tracking**: Each arrow stores its cumulative distance along the route polyline. User progress is estimated by projecting camera position onto the route.
6. **Runtime config**: `lookaheadDistanceMeters`, `destinationThresholdMeters`, and `arrowHeightOffsetMeters` are all read from `route_rendering.json` at runtime.

### How Rolling Lookahead Works

- All arrows are placed into the scene at startup but hidden (scale = 0).
- Each frame, `updateVisibility(userCumulativeDistance:)` classifies each arrow:
  - **Active** (within lookahead window) → full size and opacity
  - **Fading** (behind user, within fade distance) → shrinking and fading
  - **Hidden** (all others) → scale zero
- User progress is estimated by finding the closest route segment to the camera and projecting onto the route polyline.

### How Arrival Works

- Each frame, 2D distance (x, z) from camera to destination door node is computed.
- When distance ≤ `destinationThresholdMeters` (1.5m), arrival triggers once.
- All arrows hide, overlay shows "You've arrived at [Room Name]".

### Intentionally Postponed to Phase 4+

- Real QR scanner (AVCaptureSession)
- Checkpoint marker tracking
- AR rendering redesign (custom 3D arrow models)
- Android parity
- Full authoring UI/editor

## Phase 2 — Reviewed Package as Runtime Truth (2026-03-22)

### What Changed

Phase 2 replaced the draft-generated building data with a reviewed, human-corrected house navigation package.

1. **Reviewed package format**: 5-file structure (`manifest.json`, `rooms.json`, `nav_graph.json`, `entrance_markers.json`, `route_rendering.json`) replaces the monolithic `authoring_config.generated.json`.
2. **Real room names**: Destinations are now Mutfak, Salon, Yatak Odasi — not Zone A/B/C.
3. **Minimal realistic graph**: 7 nodes, 6 edges. Corridor-based layout with no wall-cutting shortcuts.
4. **BuildingPackageLoader rewritten**: `loadReviewedPackage()` returns `Result<ReviewedConfig, PackageError>`. No silent fallback.
5. **Error handling**: If the reviewed package is missing/corrupt, the app shows a `PackageErrorView` with retry — never falls back to draft data.
6. **Validation tooling**: `scripts/validate-reviewed-package.sh` checks graph connectivity, node references, duplicate ids, and data integrity.
7. **Manual correction workflow**: Documented in `docs/manual-package-review-workflow.md`.

### Runtime Truth

- **Reviewed package** (`apps/iosApp/iosApp/reviewed-package/`) is the ONLY runtime data source.
- **Draft config** (`sample/demo-building/draft/`) is an offline authoring artifact only.
- See ADR-026 for the full decision rationale.

### Manual Correction Workflow

1. Generate draft from GLB (preprocessor)
2. Inspect debug outputs
3. Create/edit reviewed package files (human-readable JSON)
4. Run validator: `./scripts/validate-reviewed-package.sh`
5. Copy to iOS bundle: `cp sample/reviewed-house-package/*.json apps/iosApp/iosApp/reviewed-package/`
6. Build and test

### Intentionally Postponed (after Phase 2)

- Real QR scanner (AVCaptureSession)
- ~~Rolling arrow lookahead/fade~~ → Done in Phase 3
- Checkpoint marker tracking
- AR rendering redesign
- Android parity
- Full authoring UI/editor

### Known Limitations After Phase 2

- QR scanning is still simulated (demo mode).
- Single entrance only (Entrance A).
- Node positions are approximate — need real-world calibration.
- No offline/network package loading — reviewed package is bundled.

## Phase 1 — iOS Truth-Path Navigation Flow (2026-03-22)

### What Changed

Established the correct iOS user flow with explicit destination gating before AR:
- State machine (`NavigationFlowModel.swift`): `home → qrScan → entranceConfirmed → destinationSelect → routePreview → arNavigation`
- New screens: `QRScanView`, `DestinationSelectView`, `RoutePreviewView`
- AR requires explicit destination — no fallbacks
- See ADR-025

## Build

```bash
# Generate entrance poster and update asset catalog (required before first build)
./scripts/generate-entrance-poster.sh

# iOS: open in Xcode
open apps/iosApp/iosApp.xcodeproj
# Build target: iosApp, device: any iOS 17+ device/simulator with ARKit

# Validate reviewed package
./scripts/validate-reviewed-package.sh sample/reviewed-house-package/

# Demo readiness (comprehensive pre-demo check)
./scripts/check-demo-readiness.sh

# Regression checks (fast pre-commit)
./scripts/regression-checks.sh

# Admin draft API (dev-only)
./gradlew :tools:admin-api:run

# Admin draft API verification
./scripts/verify-admin-draft-api.sh
```

## Key Architecture Decisions

See `docs/adr/` for the full ADR log. Key ones:
- ADR-001: KMP Shared UI + Native AR
- ADR-008: Shared Route / Native AR Boundary
- ADR-025: iOS truth-path navigation flow with explicit destination gating
- ADR-026: Reviewed package is runtime truth; generated draft is authoring input only
- ADR-027: AR guidance renders a rolling forward route slice driven by route progress
- ADR-028: Entrance QR establishes initial iOS alignment and facing before AR guidance begins
- ADR-029: Demo device path requires strict entrance marker asset matching and calibrated reviewed package coordinates
- ADR-030: Dev-only admin draft-ingestion pipeline with filesystem persistence
