# ADR-002: Graph-Based Routing Instead of Raw Point Cloud

**Status:** Accepted  
**Date:** 2026-03-08

## Context

The Polycam scan produces a dense 3D mesh (potentially millions of triangles). We need to compute walkable routes through the building for the navigation feature.

Two approaches were considered:
1. **Route on raw point cloud / mesh** — treat the mesh as navigable space directly
2. **Route on a preprocessed navigation graph** — simplify the mesh into nodes/edges offline

## Decision

Use a **preprocessed navigation graph** generated offline from the .glb scan. The graph consists of `NavNode` waypoints connected by `NavEdge`s with distance weights.

- Graph is generated once by the `nav-preprocessor` CLI tool
- Graph is shipped as `nav_graph.json` in the building package
- Routing at runtime uses Dijkstra's algorithm on the graph (≪1ms for typical buildings)

## Consequences

### Positive
- Routing is O(V log V + E) instead of expensive real-time mesh analysis
- Graph is tiny (~KB) vs raw mesh (~MB), fast to download and cache
- Deterministic routes — same input always produces same path
- Easy to extend: add accessibility weights, temporary closures, preferred paths
- Works offline with zero latency

### Negative
- Requires offline preprocessing step (acceptable — scans are also done offline)
- Graph may not capture every possible path (acceptable for MVP — main corridors suffice)
- Manual tuning may be needed for complex layouts

### Alternatives Rejected
- **NavMesh generation at runtime**: Too slow on mobile, complex to implement per-platform
- **Voxel-based pathfinding**: Memory-intensive, unnecessary for single-floor indoor
