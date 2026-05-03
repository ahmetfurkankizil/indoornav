# File Dossier: ADR-030-admin-draft-ingestion-pipeline.md

## Path
`docs\adr\ADR-030-admin-draft-ingestion-pipeline.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-030: Dev-Only Admin Draft-Ingestion Pipeline

**Status:** Accepted
**Date:** 2026-03-29
**Deciders:** Vectura AI team
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

- Accepts multipart `.glb` uploads via `POST /admin/draft-jo
```

## Status
Mapped (Pass 3 Normalization)
