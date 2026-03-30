# ADR-030: Dev-Only Admin Draft-Ingestion Pipeline

**Status:** Accepted
**Date:** 2026-03-29
**Deciders:** VecturAI team
**Relates to:** ADR-006 (assisted authoring), ADR-026 (reviewed package runtime truth)

## Context

The current workflow requires running the nav-preprocessor CLI manually from the terminal to generate draft authoring configs from Polycam GLB scans. This works for developers but creates friction:

1. The operator must have a working Gradle/JVM toolchain.
2. There is no visibility into job status or artifacts from the iOS app.
3. There is no path toward a future in-app authoring experience.

We need a minimal pipeline to prove: GLB upload -> preprocessing job -> draft artifacts generated -> status visible in app. This is the first step toward assisted authoring from within the app.

## Decision

### Dev-only Ktor backend for draft jobs

Add a `tools/admin-api` module containing a Ktor/JVM HTTP server that:

- Accepts multipart `.glb` uploads via `POST /admin/draft-jobs`
- Creates filesystem-backed jobs under `build/admin-draft-jobs/<jobId>/`
- Runs the existing `DraftPipeline` from `tools/nav-preprocessor` directly (library dependency, not subprocess)
- Persists job state as a simple JSON file (`job.json`) in the job directory
- Exposes job listing, detail, and artifact endpoints

### Filesystem persistence, no database

Jobs are stored as directories on the local filesystem. Each job directory contains:
- `job.json` — status, timestamps, error message, artifact list
- `input.glb` — the uploaded GLB file
- `output/` — draft generation artifacts

This is intentionally minimal. No database, no auth, no cloud deployment. The backend is designed for local/LAN development only.

### Isolated iOS admin surface

The iOS app gets a separate "Admin Tools" entry on the home screen that leads to:
- A file picker for `.glb` uploads
- A job list with status indicators
- A job detail view showing artifacts and errors

This surface is completely isolated from the visitor navigation flow. It does not modify the bundled reviewed package or affect runtime navigation.

### Reuse existing preprocessor

The admin API calls `DraftPipeline().execute()` directly as a library dependency. No new preprocessing logic is introduced. The same artifacts are produced (`authoring_config.generated.json`, debug SVGs, metadata JSONs).

## Consequences

### Positive
- Proves the upload-to-draft pipeline end-to-end
- Reuses existing preprocessor without duplication
- Zero risk to visitor navigation flow (additive, isolated)
- Simple to verify locally with a shell script

### Negative
- Filesystem persistence does not scale (acceptable for dev-only)
- No auth means anyone on the LAN can upload (acceptable for dev-only)
- Synchronous processing blocks the request thread during draft generation (acceptable for small GLBs)

### Intentionally deferred
- Room editing UI
- 2D map preview rendering
- Reviewed package export/activation
- Runtime package switching
- Android admin UI
- Authentication
- Cloud deployment
