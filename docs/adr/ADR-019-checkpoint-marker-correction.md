# ADR-019: Optional Checkpoint-Marker Correction for Controlled-Route Robustness

**Status:** Accepted  
**Date:** 2026-03-10

## Context

VIO (Visual-Inertial Odometry) drift on routes > 20 m can cause visible misalignment between AR arrows and the physical environment. The current system uses a single entrance marker for alignment with no mid-route correction.

## Decision

Support **optional checkpoint markers** placed along the route. When the user's device observes a checkpoint marker during navigation, the shared `CorrectionCoordinator` computes a **bounded alignment correction** (max 2 m translation, 15° rotation) and updates the active `AlignmentTransform`.

Checkpoint markers are:
- **Optional** — packages without them work identically to v1.5
- **Additive** — they do not replace entrance markers
- **Conservative** — corrections are bounded to avoid jarring jumps
- **Role-tagged** — `entrance` vs `checkpoint` roles are explicit in the schema

## Consequences

- Drift compensation improves on longer controlled routes
- No full relocalization or SLAM required
- Package authors choose where to place checkpoint markers
- Native AR layers must load and track multiple reference images
- Existing single-marker packages remain unchanged
