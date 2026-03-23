# Manual Package Review Workflow

How to create or correct a reviewed navigation package from a draft-generated config.

## Overview

The reviewed package is the **only runtime truth** for the iOS app. The draft `authoring_config.generated.json` is an offline authoring input — it is never loaded at runtime.

## Package Structure

A reviewed package is a directory containing 5 JSON files:

```
reviewed-house-package/
  manifest.json            # Package metadata and file references
  rooms.json               # Destination rooms with display names
  nav_graph.json           # Navigation nodes and edges
  entrance_markers.json    # Entrance marker positions
  route_rendering.json     # Arrow rendering configuration
```

## Workflow Steps

### 1. Generate Draft from GLB

Run the preprocessor to generate a draft config from a 3D scan:

```bash
# (preprocessor command — see tools/ for details)
# Output goes to sample/demo-building/draft/authoring_config.generated.json
```

### 2. Inspect Draft Output

Review the draft to understand the auto-generated graph:
- Check `nodes` — are positions reasonable?
- Check `edges` — are there bogus shortcut edges through walls?
- Check `rooms` — are labels generic ("Zone A") or meaningful?
- Look at debug SVG outputs in `sample/demo-building/draft/` if available.

### 3. Create / Edit Reviewed Package

Create a new reviewed package directory (or edit an existing one).

#### rooms.json

Replace generic zone names with real room names:
```json
{
    "rooms": [
        {
            "id": "mutfak",
            "displayName": "Mutfak",
            "destinationNodeId": "mutfak_door",
            "category": "kitchen",
            "description": "Kitchen area"
        }
    ]
}
```

**Room categories** (used for icons): `kitchen`, `living_room`, `bedroom`, `bathroom`, `office`, `unknown`

#### nav_graph.json

Build a minimal, realistic graph:
- Use only the nodes needed for the demo (6-10 total).
- Use human-readable node ids: `entrance_a`, `corridor_1`, `mutfak_door`, etc.
- Remove edges that cut through walls or non-walkable space.
- Set `cost` to approximate walking distance in meters.
- All edges should be `bidirectional: true` unless one-way paths exist.

**Node types:** `entrance`, `junction`, `room_entry`

```json
{
    "nodes": [
        {"id": "entrance_a", "x": 0.0, "y": 0.0, "z": 0.0, "type": "entrance", "label": "Entrance A"},
        {"id": "corridor_1", "x": 2.5, "y": 0.0, "z": 0.0, "type": "junction", "label": "Hallway 1"},
        {"id": "mutfak_door", "x": 2.5, "y": 0.0, "z": -3.0, "type": "room_entry", "label": "Mutfak Door"}
    ],
    "edges": [
        {"id": "e01", "from": "entrance_a", "to": "corridor_1", "cost": 2.5, "bidirectional": true},
        {"id": "e02", "from": "corridor_1", "to": "mutfak_door", "cost": 3.0, "bidirectional": true}
    ]
}
```

#### entrance_markers.json

Set the entrance marker position (where the physical QR marker is placed):
- `position` is in building-local coordinates, typically at eye height (~1.2m).
- `startNodeId` must reference a node in the nav graph.
- `referenceImageName` must match the ARKit reference image asset name.

#### manifest.json

Update metadata:
- Set `reviewStatus` to `"reviewed"`.
- Set `reviewedBy` and `reviewedDate`.

### 4. Run Validator

```bash
./scripts/validate-reviewed-package.sh sample/reviewed-house-package/
```

The validator checks:
- All required files exist
- Manifest fields are present
- Room destination nodes exist in the graph
- Entrance marker nodes exist in the graph
- All edge endpoints are valid
- No duplicate ids
- Edge costs are positive
- Graph is connected from entrance to each destination

**All checks must pass before bundling.**

### 5. Bundle into iOS App

Copy the reviewed package files into the iOS app:

```bash
cp sample/reviewed-house-package/*.json apps/iosApp/iosApp/reviewed-package/
```

The files are already referenced in the Xcode project (`project.pbxproj`) as bundle resources.

### 6. Build and Test

```bash
cd apps/iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS' build -allowProvisioningUpdates
```

Verify in the app:
- Home screen shows "Scan QR Code"
- After scan, destination list shows correct room names (not "Zone A" etc.)
- Route preview shows reasonable distances
- AR navigation follows the graph edges

## Tips

- **Keep the graph minimal.** 6-10 nodes is ideal for a house demo. Dense mesh-derived graphs create unrealistic shortcuts.
- **Use stable ids.** `mutfak_door` is better than `n02` — it's self-documenting and survives graph edits.
- **Test each destination.** The validator checks connectivity but not whether distances are physically reasonable.
- **Version the package.** Bump `packageVersion` in manifest when making changes.
