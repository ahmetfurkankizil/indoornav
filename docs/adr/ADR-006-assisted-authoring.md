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
2. A human author creates an **`authoring_config.json`** file defining:
   - Graph nodes (waypoints) with 3D coordinates in building-local space
   - Graph edges (connections) with traversal costs
   - Room definitions with destination node IDs
   - Entrance marker metadata (pose, QR payload, physical dimensions)
   - Route rendering settings
3. The **nav-preprocessor CLI** validates the authored config against strict rules and exports a production building package
4. The pipeline architecture is **modular** — future automatic extraction plugs in by generating the same `authoring_config.json` format

### Authoring Workflow

```
Author measures building → authors config JSON → runs CLI → inspects debug SVG → iterates
```

The debug SVG (plan view of nodes/edges/rooms/markers) provides immediate visual feedback for the author.

## Consequences

### Positive
- **Reliable, deterministic output** — human-authored graphs are correct by construction
- **Fast iteration** — authoring a 10-node graph takes minutes, not days of CV debugging
- **Clean separation** — same pipeline works whether input is human-authored or auto-generated
- **Debug SVG** enables fast visual validation by non-technical team members
- **No dependency** on fragile mesh-processing libraries

### Negative
- Manual effort per building (acceptable for MVP with one building)
- Human measurement inaccuracies (mitigated by debug SVG review)
- Does not scale to hundreds of buildings without tooling improvements

### Migration Path
- Future: GUI authoring tool overlaid on .glb preview
- Future: ML-based automatic node/edge suggestion from scans
- Future: BIM import for buildings with CAD data
- All future approaches emit the same `authoring_config.json` → same pipeline
