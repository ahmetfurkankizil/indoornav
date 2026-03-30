# VecturAI — Indoor AR Navigation

> Kotlin Multiplatform indoor AR navigation for Android & iOS.

**v1.7.0-rc1** | [First-Day Setup](docs/setup/first-day-setup.md) | [Architecture](docs/handoff/architecture-summary.md)

---

## Quick Start

```bash
git clone <repo-url> && cd vecturai
make help                  # show all targets
make test-preprocessor     # run ~170 tests
make verify-all            # full verification (tests + build + package)
make android-debug         # build Android APK
make ios-open              # open Xcode
```

New? Start with the [First-Day Setup Guide](docs/setup/first-day-setup.md).

## Architecture

```
Authoring Config → Preprocessor → Building Package
                                        ↓
Native AR (ARKit/ARCore)          PackageLoader → RouteEngine → ArrowMapper
  │ CameraPose @ 2Hz                                                ↓
  ↓                                                          ProgressEstimator
AlignmentTransform ← MarkerDetector                                ↓
  ↓                                                          ArrivalDetector
CorrectionCoordinator ← Checkpoint Observations                    ↓
  ↓                                                          SessionCoordinator
OffRouteDetector → Recovery Recommendations                        ↓
                                                              HistoryRepository
```

Full architecture with Mermaid diagram: [Architecture Summary](docs/handoff/architecture-summary.md)

## What Works

| Feature | Demo | Live |
|---------|------|------|
| Search + preview | ✓ | ✓ |
| AR route arrows | ✓ | ✓ |
| Progress tracking | Buttons | Camera |
| Remaining distance | ✓ | ✓ |
| Arrival detection | ≥95% | Position + progress |
| Rescan/recenter | — | ✓ |
| Checkpoint correction | — | ✓ (optional) |
| Confidence/off-route | — | ✓ |
| History persistence | ✓ | ✓ |

## Config Profiles

| Profile | Debug | Simulate | Preload | 
|---------|-------|----------|---------| 
| Dev | ✓ | ✓ | ✓ |
| Demo | ✗ | ✓ | ✓ |
| Release | ✗ | ✗ | ✗ |

## Verification

```bash
make verify-all          # everything: tests + Android build + package
make verify-package      # demo package integrity only
make verify-ios          # local iOS build
make test-preprocessor   # unit tests only (~170)
```

CI runs automatically on push/PR to `main` via GitHub Actions.

## Documentation

### Getting Started
- [First-Day Setup](docs/setup/first-day-setup.md) — clone → build → run in 15 min

### Demo & Presentation
- [Presenter Guide](docs/demo/presenter-guide.md) — what to say / show
- [Operator Guide](docs/demo/operator-guide.md) — setup / troubleshoot
- [Presentation Cheatsheet](docs/demo/presentation-cheatsheet.md) — 30s / 2m / 5m scripts
- [Demo Risks & Fallbacks](docs/demo/demo-risks-and-fallbacks.md) — what can go wrong

### QA & Testing
- [Demo Smoke Checklist](docs/qa/demo-smoke-checklist.md) — scripted path
- [Live AR Checklist](docs/qa/live-ar-smoke-checklist.md) — real device
- [Checkpoint Checklist](docs/qa/checkpoint-marker-smoke-checklist.md) — checkpoint flow
- [Regression Matrix](docs/qa/regression-matrix.md) — 10 demo-critical features
- [RC Checklist](docs/release/rc-checklist.md) — release candidate gates

### Release
- [Release Process](docs/release/release-process.md) — versioning + RC flow
- [Release Notes Template](docs/release/release-notes-template.md)

### Architecture
- [Architecture Summary](docs/handoff/architecture-summary.md) — diagram + components
- [Marker Placement](docs/demo/checkpoint-marker-placement.md) — physical setup

### ADRs (1–31)

| 1–9 | KMP, routing, preprocessing, marker alignment, single-floor, assisted authoring |
|-----|---|
| 10–12 | Arrow-progress arrival, local history, demo-first UX |
| 13–15 | Build strategy, visual polish, QA testing |
| 16–18 | Controlled-route hardening, route-relative progress, recenter/rescan |
| 19–21 | Checkpoint-marker correction, confidence-scored progress, recovery/off-route |
| 22–24 | RC stabilization, CI/regression policy, presentation/operator handoff |
| 25–26 | iOS truth-path navigation flow, reviewed package runtime truth |
| 27–29 | Rolling lookahead AR guidance, entrance QR alignment, strict marker calibration |
| 30–32 | Admin draft-ingestion pipeline (dev-only), admin draft review read-only, room editing + reviewed package export |

## Admin Draft Pipeline (dev-only)

A minimal backend + iOS admin surface for uploading Polycam GLB scans and generating draft navigation packages, then reviewing the generated output. This is dev-only infrastructure for the assisted-authoring workflow.

### Run the admin API

```bash
# Start the admin API server (default port 8080)
./gradlew :tools:admin-api:run

# Run all admin API tests
./gradlew :tools:admin-api:test
```

### Verify Phase 1 — Upload & Job Status

```bash
# With server already running:
./scripts/verify-admin-draft-api.sh

# Auto-start server, run tests, stop server:
./scripts/verify-admin-draft-api.sh --start-server
```

### Verify Phase 2 — Draft Review

```bash
# With server already running:
./scripts/verify-admin-draft-review.sh

# Auto-start server, run tests, stop server:
./scripts/verify-admin-draft-review.sh --start-server
```

### Verify Phase 3 — Room Edit + Export

```bash
# With server already running:
./scripts/verify-admin-room-edit-and-export.sh

# Auto-start server, run tests, stop server:
./scripts/verify-admin-room-edit-and-export.sh --start-server
```

### iOS admin UI

From the app home screen, tap "Admin Tools" to access the GLB upload and job status screens. On a succeeded job, tap "Review Draft" to open the review screen with SVG previews and room candidates. Tap any room row to edit its display name, category, and description. Use the "Export Reviewed Package" button to produce the 5-file reviewed package. The admin API must be running on localhost (simulator) or your LAN IP (real device). Set `AdminAPIClient.baseURL` for real device testing.

### iOS networking — ATS / HTTP

The admin API runs over plain HTTP (no TLS). iOS App Transport Security (ATS) blocks HTTP by default. The Info.plist includes targeted exceptions:

| Scenario | How it works |
|----------|-------------|
| **Simulator** | `NSAllowsLocalNetworking = true` permits HTTP to `localhost` and `127.0.0.1`. No IP change needed. |
| **Real device** | `localhost` on a physical device points to the phone itself, not your Mac. Set `AdminAPIClient.baseURL` to your Mac's LAN IP (e.g. `http://192.168.x.x:8080`). The LAN IP must also be listed in the `NSExceptionDomains` entry in Info.plist. Update the domain key to match your IP before building. |

To update the LAN IP for real device testing:
1. Find your Mac's IP: `ipconfig getifaddr en0`
2. Edit `apps/iosApp/iosApp/Info.plist` — change the `NSExceptionDomains` key to match your IP
3. Edit `apps/iosApp/iosApp/admin/AdminAPIClient.swift` line 8 — set `baseURL` to the same IP
4. Rebuild and run on device

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/admin/draft-jobs` | Upload `.glb` (multipart, field: `file`) |
| GET | `/admin/draft-jobs` | List all jobs |
| GET | `/admin/draft-jobs/{id}` | Get job detail |
| GET | `/admin/draft-jobs/{id}/artifacts` | List generated artifact filenames |
| GET | `/admin/draft-jobs/{id}/summary` | Parsed draft summary (rooms, counts, stats) — merges room overrides |
| GET | `/admin/draft-jobs/{id}/artifacts/{name}/content` | Raw artifact content (SVG/JSON) |
| PATCH | `/admin/draft-jobs/{id}/rooms/{roomId}` | Update room display name, category, description |
| POST | `/admin/draft-jobs/{id}/export-reviewed-package` | Export 5-file reviewed package |
| GET | `/admin/draft-jobs/{id}/reviewed-package` | List exported package files |
| GET | `/admin/draft-jobs/{id}/reviewed-package/{file}/content` | Serve exported file content |

## Known Limitations (v1.7)

- VIO drift: ~1–2% on routes < 50m (checkpoint markers reduce visible impact)
- Checkpoint correction bounded to max 2m / 15° per observation
- Single floor only
- Progress is route-relative projection, not absolute indoor localization
- 3D arrows: placeholder geometry
- History: local JSON file, no cloud sync
- Controlled-route assumption: authored nav-graph, no dynamic obstacles
