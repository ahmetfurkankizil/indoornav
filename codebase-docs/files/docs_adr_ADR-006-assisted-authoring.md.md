# File Dossier: ADR-006-assisted-authoring.md

## Path
`docs\adr\ADR-006-assisted-authoring.md`

## Type
Authored Source

## Role
Authored Source for the docs component.

## Logic Overview
(Inferred from first 50 lines)
```kotlin
# ADR-006: Assisted Graph Authoring for V1

**Status:** Accepted  
**Date:** 2026-03-08

## Context

The Polycam .glb scan is a dense 3D mesh. Automatically extracting a walkable navigation graph from raw mesh geometry requires:
- Floor plane detection via normal clustering
- Wall/obstacle identification via mesh segmentation
- Room boundary detection (unsolved in general for unstructured scans)
- Doorway inference
- Walkable-area discretization into a clean graph

These are hard computer-vision problems. The raw phone scans from Polycam are noisy, have holes, and lack semantic labels. Building a robust automatic extraction pipeline would take months and produce fragile results — unacceptable for an MVP that prioritizes smooth investor demos.

## Decision

**V1 uses a semi-automatic assisted authoring workflow:**

1. The .glb scan serves as the **geometric reference and AR preview asset** (not processed for navigation)
2. A human author creates an **`authoring_config.json`** file defin
```

## Status
Mapped (Pass 3 Normalization)
