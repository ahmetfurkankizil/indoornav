# Vectura AI Navigation Preprocessor Pipeline

The nav-preprocessor is a Kotlin CLI tool that converts a 3D building scan (`.glb`) into a navigation package for the AR app.

## CLI Commands

```bash
# Inspect GLB file — print geometry stats and floor estimate
./gradlew :tools:nav-preprocessor:run --args="inspect --input scan.glb"

# Generate draft config — semi-automatic pipeline from GLB geometry
./gradlew :tools:nav-preprocessor:run --args="generate-draft --input scan.glb --output draft/"

# Export package — validate config + GLB and produce final package
./gradlew :tools:nav-preprocessor:run --args="export-package --input scan.glb --config authoring_config.json --output package/ --overwrite"
```

Or via `make`:
```bash
make inspect          # Inspect sample GLB
make generate-draft   # Generate draft from sample GLB
make preprocess       # Export package from sample building
```

## Workflow

### Option A: Manual (original)
1. Scan building with Polycam → `scan.glb`
2. **Manually** write `authoring_config.json` (nodes, edges, rooms, markers)
3. Run `export-package` → validates and produces final package

### Option B: Semi-Automatic (new)
1. Scan building with Polycam → `scan.glb`
2. Run `generate-draft` → auto-generates `authoring_config.generated.json`
3. **Review and edit** the generated config (rename zones, adjust nodes, add real markers)
4. Rename to `authoring_config.json` (or copy with edits)
5. Run `export-package` → validates and produces final package

## Draft Pipeline Steps

| Step | What it does |
|------|-------------|
| 1. Parse GLB | Reads binary glTF 2.0 chunks (JSON + BIN) |
| 2. Extract geometry | Walks meshes → accessors → bufferViews → float32 vertex positions |
| 3. Estimate floor | Y-histogram peak detection → floor Y with confidence score |
| 4. Occupancy grid | Projects floor vertices onto 2D XZ grid, fills 1-cell gaps |
| 5. Discover zones | Connected-component labeling (BFS flood-fill), noise filtering |
| 6. Draft nav graph | Zone centroid nodes, corridor waypoints, inter-zone edges |
| 7. Generate config | Assembles `authoring_config.generated.json` with neutral labels |
| 8. Debug export | SVGs (occupancy, nav graph) + `geometry_stats.json` |

## Automation Levels

| Capability | Level | Details |
|-----------|-------|---------|
| GLB parsing & vertex extraction | ✅ Automatic | Pure binary parsing, no manual work |
| Floor plane estimation | ✅ Automatic | Y-histogram, with confidence score |
| Walkable area mapping | ✅ Automatic | 2D occupancy grid from floor vertices |
| Zone/region discovery | ✅ Automatic | Connected-component labeling |
| Navigation graph topology | ⚡ Semi-automatic | Auto-generated, but needs review |
| Zone/room naming | ⚡ Semi-automatic | Neutral labels ("Zone A"), rename manually |
| Room categories & keywords | ❌ Manual | No semantic understanding of room types |
| Entrance marker placement | ❌ Manual | Physical marker position must be configured |
| Checkpoint markers | ❌ Manual | Optional, for mid-route correction |
| Route rendering config | ✅ Automatic | Sensible defaults, editable |

## Debug Outputs

| File | Description |
|------|-------------|
| `occupancy_debug.svg` | 2D grid colored by zone, dark theme |
| `draft_graph_debug.svg` | Navigation nodes + edges overlaid on plan view |
| `geometry_stats.json` | Vertex count, bounds, floor Y, confidence, zone stats |
| `generation_metadata.json` | Pipeline metadata, timestamps, confidence level |
| `graph_debug.json` | (export-package only) Full graph with computed metadata |
| `plan_view_debug.svg` | (export-package only) Node/edge/room plan view |

## Generated Config Format

The `authoring_config.generated.json` follows the exact same schema as the manual `authoring_config.json`. Key differences:

- Tags include `"draft"` and `"auto-generated"`
- Zone labels are neutral: "Zone A", "Zone B", etc.
- Room categories are `"unknown"` — must be renamed
- Entrance marker is a **placeholder** — must be positioned
- Graph metadata notes say "Auto-generated draft"
- A separate `generation_metadata.json` records pipeline confidence

> **Important**: The generated config is meant to be a starting point. Always review and edit before using with `export-package`.
