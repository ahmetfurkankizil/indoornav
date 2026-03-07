# ADR-003: Offline Preprocessing from Polycam .glb

**Status:** Accepted  
**Date:** 2026-03-08

## Context

Building scan data comes as a `.glb` file from Polycam. We need to convert this into navigation-ready data structures. This conversion involves geometry analysis (floor detection, obstacle identification, graph construction).

## Decision

All scan processing is done **offline** via a JVM-based CLI tool (`tools/nav-preprocessor`). The tool:

1. Loads the `.glb` asset
2. Validates geometry
3. Extracts the floor plane
4. Builds a navigation graph
5. Exports standardized JSON contracts (`nav_graph.json`, `rooms.json`, etc.)

The output contracts are uploaded to the backend server and downloaded by the mobile app.

## Consequences

### Positive
- No heavy computation on mobile devices
- Processing can use powerful desktop hardware for complex geometry analysis
- Generated contracts are small, cacheable JSON files
- Pipeline is extensible — add steps without changing the mobile app
- Reproducible: same input .glb always produces same output

### Negative
- Building updates require re-running the preprocessor (acceptable — buildings change rarely)
- Room labeling currently requires manual configuration alongside the generated data
- Not suitable for dynamic building layouts (out of MVP scope)

### Future Extensions
- GUI wrapper around the CLI for non-technical staff
- Automatic room detection using ML/computer vision on the mesh
- Integration with building BIM (Building Information Modeling) data
