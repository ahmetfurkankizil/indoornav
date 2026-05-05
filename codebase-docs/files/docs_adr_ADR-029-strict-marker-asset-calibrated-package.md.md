# File Dossier: ADR-029-strict-marker-asset-calibrated-package.md

## Path
`docs\adr\ADR-029-strict-marker-asset-calibrated-package.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-029: Demo Device Path Requires Strict Entrance Marker Asset Matching and Calibrated Reviewed Package Coordinates

**Status:** Accepted
**Date:** 2026-03-23
**Deciders:** VecturAI iOS team
**Relates to:** ADR-026, ADR-028

## Context

Phase 4 established entrance-marker-based AR alignment, but the device path still had a lenient fallback: `ARMarkerDetector` would accept *any* detected image anchor and treat it as the entrance marker. This was acceptable during development but creates two problems for a real demo:

1. **False positive alignment**: A random poster, logo, or texture could trigger alignment, placing arrows in a completely wrong position.
2. **No asset validation**: If the AR reference image asset was missing from the Xcode asset catalog, the session would start silently without detection capability — the user would wait 30 seconds and hit the timeout every time.

Additionally, the reviewed package node coordinates were placeholder values from the manual editing phase.
```

## Status
Mapped (Pass 3 Normalization)
