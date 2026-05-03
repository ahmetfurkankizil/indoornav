# Vectura AI Building Package — Contract Reference

## Coordinate Conventions

All spatial data in Vectura AI uses a single, consistent coordinate system:

| Property | Convention |
|----------|-----------|
| **Unit** | Meters (all coordinates, distances, dimensions) |
| **Handedness** | Right-handed |
| **Up axis** | Y-up (`+Y` points toward ceiling) |
| **Forward** | `-Z` (conventional for Y-up right-handed systems) |
| **Origin** | Building-local origin, chosen during authoring |
| **Floor plane** | `Y = 0` for single-floor MVP |
| **Rotation unit** | Degrees (clockwise when viewed from above) |

### Why Y-up?

Y-up is the default coordinate system for:
- glTF / .glb files (Polycam exports in Y-up)
- ARKit (uses Y-up by default)
- RealityKit (Y-up)
- Most 3D tooling (Blender defaults to Z-up but exports glTF as Y-up)

ARCore uses Y-up as well. Using Y-up avoids coordinate transforms between the .glb asset, the navigation graph, and both AR runtimes.

### Origin Placement

The building-local origin should be placed at a memorable, accessible location — typically:
- The entrance where the primary marker is placed
- A corner of the building footprint
- The center of the main corridor

The origin must be consistent across all data files in the package: the same origin used for graph nodes must be used for entrance marker positions and room centers.

## Package Structure

A processed building package is a directory containing:

```
package/
├── manifest.json           # Package metadata, version, file list
├── nav_graph.json          # Navigation nodes and edges
├── rooms.json              # Searchable room/POI list
├── entrance_markers.json   # AR alignment markers
├── route_rendering.json    # Arrow/path rendering config
├── preview.glb             # Copy of the Polycam scan for 3D preview
├── graph_debug.json        # Debug: full graph with computed metadata
└── plan_view_debug.svg     # Debug: visual plan-view of the graph
```

## Authoring Workflow (V1)

1. **Scan** the building with Polycam → export `.glb`
2. **Author** `authoring_config.json` with:
   - Building metadata
   - Graph nodes (measured positions in building-local coordinates)
   - Graph edges (connections with distances)
   - Room definitions (name, entry node)
   - Entrance marker(s) (position, orientation, QR payload)
   - Route rendering preferences
3. **Run** the preprocessor:
   ```bash
   ./gradlew :tools:nav-preprocessor:run \
     --args="--input scan.glb --config authoring_config.json --output ./package/"
   ```
4. **Inspect** `plan_view_debug.svg` in a browser to verify graph layout
5. **Iterate** until the graph matches the real building
6. **Deploy** the `package/` directory to the backend server

## Contract Files

| File | Schema | Description |
|------|--------|-------------|
| `manifest.json` | [manifest.schema.json](manifest.schema.json) | Package metadata |
| `nav_graph.json` | [nav_graph.schema.json](nav_graph.schema.json) | Nodes and edges |
| `rooms.json` | [rooms.schema.json](rooms.schema.json) | Searchable rooms |
| `entrance_markers.json` | [entrance_markers.schema.json](entrance_markers.schema.json) | AR markers |
| `route_rendering.json` | [route_rendering.schema.json](route_rendering.schema.json) | Rendering config |
| `authoring_config.json` | [authoring_config.schema.json](authoring_config.schema.json) | **Input** config |

## Schema Versioning

All package files include a `schemaVersion` field (integer). The app should reject packages with unsupported schema versions. Current version: **1**.
